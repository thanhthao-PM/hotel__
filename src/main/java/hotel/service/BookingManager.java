package hotel.service;

import hotel.model.Booking;
import hotel.model.BookingDetail;
import hotel.model.BookingList;
import hotel.model.BookingRequest;
import hotel.model.BookingRequestList;
import hotel.model.Deposit;
import hotel.model.DepositList;
import hotel.model.IndividualBooking;
import hotel.model.GroupBooking;
import hotel.model.bookingDetailList;
import hotel.model.enums.DepositStatus;
import hotel.model.enums.BookingStatus;
import hotel.model.enums.PaymentMethod;
import hotel.model.enums.RequestStatus;
import hotel.storage.IFileHandler;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lớp Quản lý các đơn Đặt phòng, Yêu cầu đặt phòng và Tiền cọc (Module P3).
 * Cập nhật theo luồng nghiệp vụ mới nhất: BookingRequest -> Deposit -> Booking.
 */
public class BookingManager implements IFileHandler {
    public static final String DATA_DIR = "src/main/java/hotel/file";
    private static final String BOOKINGS_FILE = "src/main/java/hotel/file/bookings.txt";
    private static final String REQUESTS_FILE = "src/main/java/hotel/file/booking_requests.txt";
    private static final String DETAILS_FILE = "src/main/java/hotel/file/booking_details.txt";
    private static final String DEPOSITS_FILE = "src/main/java/hotel/file/deposits.txt";
    private static final DateTimeFormatter FILE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final BookingList bookingList;
    private final BookingRequestList requestList;
    private final bookingDetailList bookingDetails = new bookingDetailList();
    private final DepositList deposits = new DepositList();
    private final String dataDir;

    // Bộ đếm sinh mã tự động (Static counter)
    private static int nextBookingId = 1;
    private static int nextRequestId = 1;
    private static int nextDetailId = 1;
    private static int nextDepositId = 1;

    public BookingManager() {
        this(100, DATA_DIR);
    }

    public BookingManager(int initialCapacity) {
        this(initialCapacity, DATA_DIR);
    }

    public BookingManager(String dataDir) {
        this(100, dataDir);
    }

    public BookingManager(int initialCapacity, String dataDir) {
        this.bookingList = new BookingList(initialCapacity);
        this.requestList = new BookingRequestList();
        CheckingExpression.checkNotNullOrEmpty(dataDir, "Thư mục dữ liệu đặt phòng không được để trống");
        this.dataDir = dataDir;
    }

    public static String resolveDataDirectory() {
        Path directory = Paths.get(DATA_DIR);
        if (Files.isDirectory(directory)) return directory.toString();
        Path sourceTreeDirectory = Paths.get("hotel", "file");
        if (Files.isDirectory(sourceTreeDirectory)) return sourceTreeDirectory.toString();
        return DATA_DIR;
    }

    @Override
    public void readFile() {
        bookingList.clear();
        requestList.clear();
        bookingDetails.clear();
        deposits.clear();
        nextBookingId = nextRequestId = nextDetailId = nextDepositId = 1;

        try {
            readRequests();
            readDetails();
            readBookings();
            validateBookingReferences();
            readDeposits();
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được dữ liệu đặt phòng trong " + dataDir, e);
        }
    }

    private void readRequests() throws IOException {
        int lineNumber = 0;
        for (String line : readDataLines(REQUESTS_FILE)) {
            lineNumber++;
            String[] fields = fields(line, 12, REQUESTS_FILE, lineNumber);
            try {
                String groupName = emptyToNull(fields[5]);
                BookingRequest request = new BookingRequest(fields[0], fields[1], fields[4],
                        groupName, LocalDate.parse(fields[6]), LocalDate.parse(fields[7]),
                        fields[2], 100);
                request.setBookingId(emptyToNull(fields[3]));
                request.setRequestStatus(RequestStatus.valueOf(fields[8]));
                request.setDepositDueAt(LocalDateTime.parse(fields[9]));
                request.setCancelReason(emptyToNull(fields[10]));
                request.setCreatedAt(LocalDateTime.parse(fields[11]));
                request.setCancellationPending(request.getCancelReason() != null
                        && request.isActive());
                if (findRequestById(request.getRequestId()) != null) {
                    throw new IllegalArgumentException("Mã yêu cầu bị trùng");
                }
                requestList.add(request);
                syncCounter(request.getRequestId(), "RQ");
            } catch (RuntimeException e) {
                throw invalidRecord(REQUESTS_FILE, lineNumber, e);
            }
        }
    }

    private void readDetails() throws IOException {
        int lineNumber = 0;
        for (String line : readDataLines(DETAILS_FILE)) {
            lineNumber++;
            String[] fields = fields(line, 11, DETAILS_FILE, lineNumber);
            try {
                String requestId = emptyToNull(fields[1]);
                String bookingId = emptyToNull(fields[2]);
                if (requestId == null && bookingId == null) {
                    throw new IllegalArgumentException("Chi tiết không gắn với yêu cầu hoặc booking");
                }
                BookingDetail detail = new BookingDetail(fields[0], requestId, bookingId,
                        fields[3], emptyToNull(fields[4]), Integer.parseInt(fields[5]),
                        Double.parseDouble(fields[6]), Double.parseDouble(fields[7]),
                        LocalDate.parse(fields[8]), LocalDate.parse(fields[9]),
                        emptyToNull(fields[10]));
                if (bookingDetails.findById(detail.getBookingDetailId()) != null) {
                    throw new IllegalArgumentException("Mã chi tiết bị trùng");
                }
                if (requestId != null) {
                    BookingRequest request = findRequestById(requestId);
                    if (request == null) {
                        throw new IllegalArgumentException("Không tìm thấy yêu cầu " + requestId);
                    }
                    if (request.getDetailCount() >= 100) {
                        throw new IllegalArgumentException("Yêu cầu vượt quá 100 dòng chi tiết");
                    }
                    request.addDetail(detail);
                }
                bookingDetails.add(detail);
                syncCounter(detail.getBookingDetailId(), "BD");
            } catch (RuntimeException e) {
                throw invalidRecord(DETAILS_FILE, lineNumber, e);
            }
        }
    }

    private void readBookings() throws IOException {
        int lineNumber = 0;
        for (String line : readDataLines(BOOKINGS_FILE)) {
            lineNumber++;
            String[] fields = line.split("\\|", -1);
            try {
                if (fields.length < 10) {
                    throw new IllegalArgumentException("Thiếu trường dữ liệu");
                }
                BookingStatus status = BookingStatus.valueOf(fields[5]);
                LocalDate checkIn = LocalDate.parse(fields[3]);
                LocalDate checkOut = LocalDate.parse(fields[4]);
                LocalDateTime createdAt = LocalDateTime.parse(fields[6]);
                String requestId = findRequestIdByBookingId(fields[1]);
                List<BookingDetail> details = findDetailsByBookingId(fields[1]);
                if (details.isEmpty()) {
                    throw new IllegalArgumentException("Không có chi tiết cho booking " + fields[1]);
                }

                Booking booking;
                if ("IND".equals(fields[0]) && fields.length == 10) {
                    booking = new IndividualBooking(fields[1], requestId, fields[2],
                            checkIn, checkOut, fields[7], emptyToNull(fields[8]),
                            details.get(0).getRoomId(), Integer.parseInt(fields[9]));
                } else if ("GRP".equals(fields[0]) && fields.length == 11) {
                    String[] roomIds = details.stream()
                            .map(BookingDetail::getRoomId).toArray(String[]::new);
                    GroupBooking group = new GroupBooking(fields[1], requestId, fields[2],
                            checkIn, checkOut, fields[7], emptyToNull(fields[8]), fields[9],
                            roomIds, 100);
                    group.setMemberIds(fields[10].isBlank()
                            ? new String[0] : fields[10].split(";"));
                    booking = group;
                } else {
                    throw new IllegalArgumentException("Loại booking không hợp lệ");
                }
                booking.setStatus(status);
                booking.setCreatedAt(createdAt);
                if (bookingList.findById(booking.getBookingId()) != null) {
                    throw new IllegalArgumentException("Mã booking bị trùng");
                }
                bookingList.add(booking);
                syncCounter(booking.getBookingId(), "BK");
            } catch (RuntimeException e) {
                throw invalidRecord(BOOKINGS_FILE, lineNumber, e);
            }
        }
    }

    private void readDeposits() throws IOException {
        int lineNumber = 0;
        for (String line : readDataLines(DEPOSITS_FILE)) {
            lineNumber++;
            String[] fields = fields(line, 9, DEPOSITS_FILE, lineNumber);
            try {
                String requestId = emptyToNull(fields[1]);
                String bookingId = emptyToNull(fields[2]);
                if (requestId != null && findRequestById(requestId) == null) {
                    throw new IllegalArgumentException("Không tìm thấy yêu cầu " + requestId);
                }
                if (bookingId != null && bookingList.findById(bookingId) == null) {
                    throw new IllegalArgumentException("Không tìm thấy booking " + bookingId);
                }
                if (requestId == null && bookingId == null) {
                    throw new IllegalArgumentException("Tiền cọc không gắn với yêu cầu hoặc booking");
                }
                Deposit deposit = new Deposit(fields[0], requestId, bookingId,
                        Double.parseDouble(fields[3]), PaymentMethod.valueOf(fields[4]),
                        LocalDateTime.parse(fields[5]), fields[6], emptyToNull(fields[7]),
                        DepositStatus.valueOf(fields[8]));
                if (deposits.findById(deposit.getDepositId()) != null) {
                    throw new IllegalArgumentException("Mã tiền cọc bị trùng");
                }
                deposits.add(deposit);
                syncCounter(deposit.getDepositId(), "DP");
            } catch (RuntimeException e) {
                throw invalidRecord(DEPOSITS_FILE, lineNumber, e);
            }
        }
    }

    @Override
    public void writeFile() {
        ArrayList<String> bookingLines = new ArrayList<>();
        for (Booking booking : bookingList.getAll()) {
            if (booking instanceof GroupBooking group) {
                bookingLines.add(FileUtil.join("GRP", booking.getBookingId(), booking.getCustomerId(),
                        booking.getCheckInDate(), booking.getExpectedCheckOutDate(), booking.getStatus(),
                        formatDateTime(booking.getCreatedAt()), booking.getCreatedBy(),
                        booking.getNote(), group.getGroupName(), String.join(";", group.getMemberIds())));
            } else if (booking instanceof IndividualBooking individual) {
                bookingLines.add(FileUtil.join("IND", booking.getBookingId(), booking.getCustomerId(),
                        booking.getCheckInDate(), booking.getExpectedCheckOutDate(), booking.getStatus(),
                        formatDateTime(booking.getCreatedAt()), booking.getCreatedBy(),
                        booking.getNote(), individual.getNumberOfGuests()));
            } else {
                throw new IllegalStateException("Loại booking không được hỗ trợ: "
                        + booking.getClass().getName());
            }
        }

        ArrayList<String> requestLines = new ArrayList<>();
        for (BookingRequest request : requestList.getAll()) {
            requestLines.add(FileUtil.join(request.getRequestId(), request.getCustomerId(),
                    request.getCreatedBy(), request.getBookingId(), request.getBookingType(),
                    request.getGroupName(), request.getExpectedCheckInDate(),
                    request.getExpectedCheckOutDate(), request.getRequestStatus(),
                    formatDateTime(request.getDepositDueAt()), request.getCancelReason(),
                    formatDateTime(request.getCreatedAt())));
        }

        ArrayList<String> detailLines = new ArrayList<>();
        for (BookingDetail detail : bookingDetails.getAll()) {
            detailLines.add(FileUtil.join(detail.getBookingDetailId(), detail.getRequestId(),
                    detail.getBookingId(), detail.getRoomId(), detail.getPreviousRoomId(),
                    detail.getGuestCount(), detail.getBookingUnitPrice(), detail.getDepositShare(),
                    detail.getCheckInDate(), detail.getCheckOutDate(), detail.getNotes()));
        }

        ArrayList<String> depositLines = new ArrayList<>();
        for (Deposit deposit : deposits.getAll()) {
            depositLines.add(FileUtil.join(deposit.getDepositId(), deposit.getRequestId(),
                    deposit.getBookingId(), deposit.getAmount(), deposit.getMethod(),
                    formatDateTime(deposit.getPaidAt()), deposit.getReceivedBy(),
                    deposit.getReferenceNo(), deposit.getStatus()));
        }

        writeDataLines(BOOKINGS_FILE, bookingLines);
        writeDataLines(REQUESTS_FILE, requestLines);
        writeDataLines(DETAILS_FILE, detailLines);
        writeDataLines(DEPOSITS_FILE, depositLines);
    }

    private ArrayList<String> readDataLines(String fileName) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        for (String line : FileUtil.readLines(dataPath(fileName))) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) lines.add(trimmed);
        }
        return lines;
    }

    private static String[] fields(String line, int count, String fileName, int lineNumber) {
        String[] result = line.split("\\|", -1);
        if (result.length != count) {
            throw new IllegalArgumentException(fileName + " dòng " + lineNumber
                    + ": cần " + count + " trường, nhận " + result.length);
        }
        return result;
    }

    private static IllegalStateException invalidRecord(String fileName, int lineNumber,
                                                       RuntimeException cause) {
        return new IllegalStateException(fileName + " dòng " + lineNumber
                + " không hợp lệ: " + cause.getMessage(), cause);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String findRequestIdByBookingId(String bookingId) {
        for (BookingRequest request : requestList.getAll()) {
            if (bookingId.equals(request.getBookingId())) return request.getRequestId();
        }
        return null;
    }

    private void validateBookingReferences() {
        for (BookingDetail detail : bookingDetails.getAll()) {
            if (detail.getBookingId() != null && bookingList.findById(detail.getBookingId()) == null) {
                throw new IllegalStateException("Chi tiết " + detail.getBookingDetailId()
                        + " tham chiếu booking không tồn tại: " + detail.getBookingId());
            }
        }
        for (BookingRequest request : requestList.getAll()) {
            if (request.getBookingId() != null
                    && bookingList.findById(request.getBookingId()) == null) {
                throw new IllegalStateException("Yêu cầu " + request.getRequestId()
                        + " tham chiếu booking không tồn tại: " + request.getBookingId());
            }
        }
    }

    private List<BookingDetail> findDetailsByBookingId(String bookingId) {
        ArrayList<BookingDetail> found = new ArrayList<>();
        for (BookingDetail detail : bookingDetails.getAll()) {
            if (bookingId.equals(detail.getBookingId())) found.add(detail);
        }
        return found;
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(FILE_DATE_TIME);
    }

    private void writeDataLines(String fileName, ArrayList<String> lines) {
        String path = dataPath(fileName);
        try {
            ArrayList<String> content = new ArrayList<>();
            for (String line : FileUtil.readLines(path)) {
                if (line.trim().startsWith("#")) content.add(line);
            }
            content.addAll(lines);
            FileUtil.writeLines(path, content);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc/ghi được " + path, e);
        }
    }

    private String dataPath(String fileName) {
        if (DATA_DIR.equals(dataDir)) {
            return Paths.get(resolveDataDirectory()).resolve(fileName).toString();
        }
        return Paths.get(dataDir).resolve(fileName).toString();
    }

    private static void syncCounter(String id, String prefix) {
        String digits = id.replaceFirst("^.*?(\\d+)$", "$1");
        int next = Integer.parseInt(digits) + 1;
        if ("BK".equals(prefix)) nextBookingId = Math.max(nextBookingId, next);
        else if ("RQ".equals(prefix)) nextRequestId = Math.max(nextRequestId, next);
        else if ("BD".equals(prefix)) nextDetailId = Math.max(nextDetailId, next);
        else if ("DP".equals(prefix)) nextDepositId = Math.max(nextDepositId, next);
    }

    // =========================================================================
    // 1. THUẬT TOÁN CHỐNG ĐẶT TRÙNG PHÒNG (TÍNH CẢ BOOKING VÀ REQUEST GIỮ CHỖ)
    // =========================================================================

    /**
     * Kiểm tra phòng trống trong khoảng [checkIn, checkOut].
     * Phải kiểm tra trùng lịch trên cả BookingRequest đang giữ chỗ và Booking chính thức.
     */
    public boolean isRoomAvailable(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return isRoomAvailable(roomId, checkIn, checkOut, null);
    }

    public boolean isRoomAvailable(String roomId, LocalDate checkIn, LocalDate checkOut,
                                   String excludedRequestId) {
        if (roomId == null || checkIn == null || checkOut == null) return false;
        if (!checkOut.isAfter(checkIn)) return false;

        // 1. Kiểm tra trên danh sách BookingRequest đang giữ chỗ (PENDING_DEPOSIT hoặc CONFIRMED)
        List<BookingRequest> allRequests = requestList.getAll();
        for (BookingRequest req : allRequests) {
            if (excludedRequestId != null
                    && excludedRequestId.equalsIgnoreCase(req.getRequestId())) continue;
            if (req.isActive()) {
                BookingDetail[] details = req.getDetails();
                for (int i = 0; i < req.getDetailCount(); i++) {
                    BookingDetail d = details[i];
                    if (d != null && roomId.equalsIgnoreCase(d.getOriginalRoomId())) {
                        if (checkIn.isBefore(d.getCheckOutDate()) && checkOut.isAfter(d.getCheckInDate())) {
                            return false; // Đã bị giữ chỗ bởi một Request!
                        }
                    }
                }
            }
        }

        // 2. Kiểm tra trên danh sách Booking chính thức
        Booking[] allBookings = bookingList.getAll();
        for (Booking b : allBookings) {
            if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.COMPLETED) {
                continue;
            }

            boolean containsRoom = false;
            if (b instanceof IndividualBooking) {
                containsRoom = roomId.equalsIgnoreCase(((IndividualBooking) b).getRoomId());
            } else if (b instanceof GroupBooking) {
                String[] roomIds = ((GroupBooking) b).getRoomIds();
                if (roomIds != null) {
                    for (String rId : roomIds) {
                        if (roomId.equalsIgnoreCase(rId)) {
                            containsRoom = true;
                            break;
                        }
                    }
                }
            }

            if (containsRoom) {
                if (checkIn.isBefore(b.getExpectedCheckOutDate()) && checkOut.isAfter(b.getCheckInDate())) {
                    return false; // Đã bị đụng độ lịch ở một Booking chính thức!
                }
            }
        }

        return true;
    }

    // =========================================================================
    // 2. QUẢN LÝ PHIẾU YÊU CẦU ĐẶT PHÒNG (BOOKING REQUEST & DEPOSIT)
    // =========================================================================

    /**
     * Tiếp nhận yêu cầu qua điện thoại/trực tuyến và lưu BookingRequest
     */
    public boolean createBookingRequest(BookingRequest request) {
        if (request == null || findRequestById(request.getRequestId()) != null
                || request.getDetailCount() == 0) return false;
        Set<String> roomIds = new HashSet<>();
        for (int i = 0; i < request.getDetailCount(); i++) {
            BookingDetail detail = request.getDetails()[i];
            if (detail == null || !request.getRequestId().equalsIgnoreCase(detail.getRequestId())
                    || bookingDetails.findById(detail.getBookingDetailId()) != null
                    || !request.getExpectedCheckInDate().equals(detail.getCheckInDate())
                    || !request.getExpectedCheckOutDate().equals(detail.getCheckOutDate())
                    || !roomIds.add(detail.getOriginalRoomId().toUpperCase(Locale.ROOT))
                    || !isRoomAvailable(detail.getOriginalRoomId(),
                            detail.getCheckInDate(), detail.getCheckOutDate())) return false;
        }
        try {
            requestList.add(request);
            for (int i = 0; i < request.getDetailCount(); i++) {
                bookingDetails.add(request.getDetails()[i]);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Xác nhận đóng tiền cọc cho Yêu cầu đặt phòng
     */
    public Deposit processDeposit(String requestId, double amount, PaymentMethod method, 
                                  String receivedBy, String referenceNo) {
        BookingRequest req = findRequestById(requestId);
        if (req == null || req.getRequestStatus() != RequestStatus.PENDING_DEPOSIT
                || req.isCancellationPending()) {
            return null;
        }
        CheckingExpression.checkPositiveDouble(amount, "Số tiền cọc phải > 0");
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("Số tiền cọc phải là số hữu hạn");
        }
        if (req.getDetailCount() == 0) {
            throw new IllegalArgumentException("Phiếu yêu cầu chưa có phòng nào để chia tiền cọc");
        }
        double requiredAmount = req.calculateRequiredDeposit();
        if (amount < requiredAmount) {
            throw new IllegalArgumentException(String.format(
                    "Tiền cọc chưa đủ. Cần ít nhất %,.0f", requiredAmount));
        }

        String depositId = generateDepositId();
        Deposit deposit = new Deposit(depositId, requestId, amount, method, receivedBy, referenceNo);

        double totalRoomAmount = 0;
        BookingDetail[] details = req.getDetails();
        for (int i = 0; i < req.getDetailCount(); i++) {
            totalRoomAmount += details[i].calculateRoomAmount();
        }
        for (int i = 0; i < req.getDetailCount(); i++) {
            BookingDetail detail = details[i];
            detail.setDepositShare(amount * detail.calculateRoomAmount() / totalRoomAmount);
        }
        req.confirmDeposit();
        deposits.add(deposit);
        return deposit;
    }

    /**
     * Tìm phiếu yêu cầu theo mã requestId
     */
    public BookingRequest findRequestById(String requestId) {
        if (requestId == null) return null;
        List<BookingRequest> allRequests = requestList.getAll();
        for (BookingRequest req : allRequests) {
            if (requestId.equalsIgnoreCase(req.getRequestId())) {
                return req;
            }
        }
        return null;
    }

    /**
     * Lấy toàn bộ phiếu yêu cầu (bản sao, không sửa được danh sách gốc). P4 dùng để tra BookingDetail.
     */

    public List<BookingRequest> getAllRequests() {
        return requestList.getAll();
    }

    /**
     * Hủy Yêu cầu đặt phòng (Duyệt bởi Quản lý)
     */
    public boolean cancelBookingRequest(String requestId, String reason, boolean refundDeposit, Deposit deposit) {
        BookingRequest req = findRequestById(requestId);
        if (req == null || !req.isActive() || !req.isCancellationPending()) return false;
        Deposit trackedDeposit = findDepositByRequestId(requestId);
        if (deposit != trackedDeposit) return false;

        req.cancel(reason);
        if (trackedDeposit != null) {
            if (refundDeposit) {
                trackedDeposit.refund();
            } else {
                trackedDeposit.forfeit(); // Tịch thu cọc (mất cọc)
            }
        }
        return true;
    }

    public boolean requestCancellation(String requestId, String reason) {
        BookingRequest request = findRequestById(requestId);
        if (request == null || !request.isActive() || request.isCancellationPending()) return false;
        request.requestCancellation(reason);
        return true;
    }

    public boolean rejectCancellation(String requestId) {
        BookingRequest request = findRequestById(requestId);
        if (request == null || !request.isCancellationPending()) return false;
        request.rejectCancellation();
        return true;
    }

    public Deposit findDepositByRequestId(String requestId) {
        if (requestId == null) return null;
        for (Deposit deposit : deposits.getAll()) {
            if (requestId.equalsIgnoreCase(deposit.getRequestId())) return deposit;
        }
        return null;
    }

    public Deposit[] getAllDeposits() {
        return deposits.getAll();
    }

    public List<BookingRequest> getCancellationRequests() {
        List<BookingRequest> pending = new ArrayList<>();
        for (BookingRequest request : requestList.getAll()) {
            if (request.isCancellationPending()) pending.add(request);
        }
        return Collections.unmodifiableList(pending);
    }

    public List<BookingDetail> getBookingDetails(String bookingId) {
        List<BookingDetail> result = new ArrayList<>();
        for (BookingDetail detail : bookingDetails.getAll()) {
            if (bookingId != null && bookingId.equalsIgnoreCase(detail.getBookingId())) {
                result.add(detail);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Booking createDirectBooking(String customerId, String bookingType, String groupName,
                                       LocalDate checkIn, LocalDate checkOut, String employeeId,
                                       String note, BookingDetail[] details) {
        if (details == null || details.length == 0) {
            throw new IllegalArgumentException("Đặt phòng phải có ít nhất một phòng");
        }
        CheckingExpression.checkValidDateRange(checkIn, checkOut);
        Set<String> selectedRoomIds = new HashSet<>();
        for (BookingDetail detail : details) {
            if (detail == null || detail.getRequestId() != null || detail.getBookingId() != null
                    || !checkIn.equals(detail.getCheckInDate())
                    || !checkOut.equals(detail.getCheckOutDate())
                    || !selectedRoomIds.add(detail.getOriginalRoomId().toUpperCase(Locale.ROOT))
                    || !isRoomAvailable(detail.getOriginalRoomId(), checkIn, checkOut)) {
                throw new IllegalArgumentException("Chi tiết phòng không hợp lệ hoặc phòng đã được đặt");
            }
        }
        String bookingId = generateBookingId();
        Booking booking;
        if ("GROUP".equalsIgnoreCase(bookingType)) {
            String[] roomIds = new String[details.length];
            for (int i = 0; i < details.length; i++) {
                if (details[i] == null) throw new IllegalArgumentException("Chi tiết phòng không hợp lệ");
                roomIds[i] = details[i].getOriginalRoomId();
            }
            booking = new GroupBooking(bookingId, null, customerId, checkIn, checkOut,
                    employeeId, note, groupName, roomIds, 20);
        } else if ("INDIVIDUAL".equalsIgnoreCase(bookingType) && details.length == 1) {
            BookingDetail detail = details[0];
            booking = new IndividualBooking(bookingId, null, customerId, checkIn, checkOut,
                    employeeId, note, detail.getOriginalRoomId(), detail.getGuestCount());
        } else {
            throw new IllegalArgumentException("Loại đặt phòng hoặc số phòng không hợp lệ");
        }
        if (!addBooking(booking)) return null;
        for (BookingDetail detail : details) {
            detail.setBookingId(bookingId);
            bookingDetails.add(detail);
        }
        return booking;
    }

    public boolean updatePendingRequest(String requestId, BookingDetail[] updatedDetails,
                                        LocalDate checkIn, LocalDate checkOut) {
        BookingRequest request = findRequestById(requestId);
        if (request == null || request.getRequestStatus() != RequestStatus.PENDING_DEPOSIT
                || request.isCancellationPending() || updatedDetails == null
                || updatedDetails.length != request.getDetailCount()) return false;
        CheckingExpression.checkValidDateRange(checkIn, checkOut);
        Set<String> roomIds = new HashSet<>();
        for (BookingDetail detail : updatedDetails) {
            if (detail == null || !requestId.equalsIgnoreCase(detail.getRequestId())
                    || !checkIn.equals(detail.getCheckInDate())
                    || !checkOut.equals(detail.getCheckOutDate())
                    || !roomIds.add(detail.getOriginalRoomId().toUpperCase(Locale.ROOT))
                    || !isRoomAvailable(detail.getOriginalRoomId(), checkIn, checkOut, requestId)) {
                return false;
            }
        }
        for (int i = 0; i < updatedDetails.length; i++) {
            BookingDetail updated = updatedDetails[i];
            request.getDetails()[i].updateReservation(updated.getOriginalRoomId(),
                    updated.getGuestCount(), updated.getBookingUnitPrice(), checkIn, checkOut);
        }
        request.setExpectedCheckInDate(checkIn);
        request.setExpectedCheckOutDate(checkOut);
        return true;
    }

    // =========================================================================
    // 3. QUẢN LÝ PHIẾU ĐẶT PHÒNG CHÍNH THỨC (BOOKING)
    // =========================================================================

    /**
     * Chuyển đổi từ BookingRequest đã cọc (CONFIRMED) sang Booking chính thức khi khách đến nhận phòng
     */
    public Booking convertRequestToBooking(String requestId, String employeeId, String note) {
        BookingRequest req = findRequestById(requestId);
        if (req == null || req.getRequestStatus() != RequestStatus.CONFIRMED
                || req.isCancellationPending()) {
            return null;
        }

        String bookingId = generateBookingId();
        Booking booking;

        if ("GROUP".equalsIgnoreCase(req.getBookingType())) {
            // đặt phòng đoàn phải có ít nhất một dòng phòng
            CheckingExpression.checkPositiveInt(req.getDetailCount(), "Phiếu đặt phòng đoàn chưa có phòng nào");
            String[] roomIds = new String[req.getDetailCount()];
            BookingDetail[] details = req.getDetails();
            for (int i = 0; i < req.getDetailCount(); i++) {
                roomIds[i] = details[i].getOriginalRoomId();
            }
            // Truyền đúng 10 tham số chuẩn của GroupBooking
            booking = new GroupBooking(bookingId, req.getRequestId(), req.getCustomerId(), 
                    req.getExpectedCheckInDate(), req.getExpectedCheckOutDate(), 
                    employeeId, note, req.getGroupName(), roomIds, 20);
        } else {
            // chặn truy cập details[0] khi phiếu chưa có dòng nào (tránh NullPointerException)
            CheckingExpression.checkValidArrayIndex(0, req.getDetailCount());
            BookingDetail detail = req.getDetails()[0];
            // Truyền đúng 9 tham số chuẩn của IndividualBooking
            booking = new IndividualBooking(bookingId, req.getRequestId(), req.getCustomerId(), 
                    req.getExpectedCheckInDate(), req.getExpectedCheckOutDate(), 
                    employeeId, note, detail.getOriginalRoomId(), detail.getGuestCount());
        }

        if (addBooking(booking)) {
            for (int i = 0; i < req.getDetailCount(); i++) {
                BookingDetail detail = req.getDetails()[i];
                detail.setBookingId(bookingId);
            }
            Deposit deposit = findDepositByRequestId(requestId);
            if (deposit != null) deposit.markApplied(bookingId);
            req.setBookingId(bookingId);
            req.convert();
            return booking;
        }
        return null;
    }

    /**
     * Thêm đơn đặt phòng trực tiếp vào BookingList
     */
    public boolean addBooking(Booking booking) {
        if (booking == null) return false;
        try {
            bookingList.add(booking);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }

    public Booking findBookingById(String bookingId) {
        return bookingList.findById(bookingId);
    }

    public boolean removeBookingById(String bookingId) {
        return bookingList.removeById(bookingId);
    }

    public Booking[] getAllBookings() {
        return bookingList.getAll();
    }

    public boolean cancelBookingById(String bookingId) {
        Booking booking = findBookingById(bookingId);
        if (booking == null || booking.getStatus() != BookingStatus.CONFIRMED) return false;
        booking.cancel();
        return true;
    }

    /**
     * Lấy danh sách Booking đã xác nhận để P4 phục vụ check-in
     */
    public Booking[] getConfirmedBookings() {
        Booking[] allBookings = bookingList.getAll();
        int count = 0;
        for (Booking b : allBookings) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                count++;
            }
        }

        Booking[] result = new Booking[count];
        int idx = 0;
        for (Booking b : allBookings) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                result[idx++] = b;
            }
        }
        return result;
    }

    // =========================================================================
    // 4. BỘ SINH MÃ TỰ ĐỘNG (AUTO-GENERATOR)
    // =========================================================================

    public String generateBookingId() {
        return String.format("BK%03d", nextBookingId++);
    }

    public String generateRequestId() {
        return String.format("RQ%03d", nextRequestId++);
    }

    public String generateDetailId() {
        return String.format("BD%03d", nextDetailId++);
    }

    public String generateDepositId() {
        return String.format("DP%03d", nextDepositId++);
    }

    public int getBookingCount() {
        return bookingList.size();
    }

    public int getRequestCount() {
        return requestList.size();
    }
}