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
import hotel.model.enums.BookingStatus;
// import hotel.model.enums.DepositStatus;
import hotel.model.enums.PaymentMethod;
import hotel.model.enums.RequestStatus;
import hotel.util.CheckingExpression;

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
public class BookingManager {
    private final BookingList bookingList;
    private final BookingRequestList requestList;
    private final bookingDetailList bookingDetails = new bookingDetailList();
    private final DepositList deposits = new DepositList();

    // Bộ đếm sinh mã tự động (Static counter)
    private static int nextBookingId = 1;
    private static int nextRequestId = 1;
    private static int nextDetailId = 1;
    private static int nextDepositId = 1;

    public BookingManager() {
        this.bookingList = new BookingList();
        this.requestList = new BookingRequestList();
    }

    public BookingManager(int initialCapacity) {
        this.bookingList = new BookingList(initialCapacity);
        this.requestList = new BookingRequestList();
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
                    || !request.getExpectedCheckInDate().equals(detail.getCheckInDate())
                    || !request.getExpectedCheckOutDate().equals(detail.getCheckOutDate())
                    || !roomIds.add(detail.getOriginalRoomId().toUpperCase(Locale.ROOT))
                    || !isRoomAvailable(detail.getOriginalRoomId(),
                            detail.getCheckInDate(), detail.getCheckOutDate())) return false;
        }
        try {
            requestList.add(request);
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

    public List<BookingRequest> getAllRequests() {
        return requestList.getAll();
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
                bookingDetails.add(detail);
            }
            Deposit deposit = findDepositByRequestId(requestId);
            if (deposit != null) deposit.markApplied(bookingId);
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
        return String.format("REQ%03d", nextRequestId++);
    }

    public String generateDetailId() {
        return String.format("DT%03d", nextDetailId++);
    }

    public String generateDepositId() {
        return String.format("DEP%03d", nextDepositId++);
    }

    public int getBookingCount() {
        return bookingList.size();
    }

    public int getRequestCount() {
        return requestList.size();
    }
}