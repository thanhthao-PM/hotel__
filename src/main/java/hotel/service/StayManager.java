package hotel.service;

import hotel.model.*;
import hotel.model.enums.Role;
import hotel.model.enums.StayStatus;
import hotel.storage.IFileHandler;
import hotel.util.FileUtil;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import hotel.util.CheckingExpression;

/**
 * Quản lý lượt ở (StayRecord), phiếu nhận, phiếu trả, khách lưu trú và dịch vụ sử dụng (P4).
 * Giữ mảng đối tượng thuần (không dùng ArrayList), tự mở rộng khi đầy.
 */
public class StayManager implements IManager<StayRecord>, IFileHandler {
    // ---------- static ----------
    public static final String STAY_FILE = "stays.txt";
    public static final String GUEST_FILE = "stay_guests.txt";
    public static final String USAGE_FILE = "service_usages.txt";
    public static final String CHECKIN_FILE = "checkin_receipts.txt";
    public static final String CHECKOUT_FILE = "checkout_receipts.txt";
    /** Quá giờ này của ngày nhận mà khách chưa đến thì được coi là no-show (nhóm chốt lại con số). */
    public static final LocalTime NO_SHOW_CUTOFF_TIME = LocalTime.of(18, 0);
    private static final int INITIAL_CAPACITY = 16;

    /** Kết quả trả phòng. */
    public static class CheckOutResult {
        public final CheckOutReceipt receipt;
        public final String invoiceId;       // null nếu P5 chưa lập được
        public final boolean bookingCompleted;
        public final String warning;         // null nếu mọi thứ ổn

        CheckOutResult(CheckOutReceipt receipt, String invoiceId, boolean bookingCompleted, String warning) {
            this.receipt = receipt;
            this.invoiceId = invoiceId;
            this.bookingCompleted = bookingCompleted;
            this.warning = warning;
        }
    }

    // ---------- dữ liệu ----------
    private StayRecord[] stays = new StayRecord[INITIAL_CAPACITY];
    private int count;
    private CheckInReceipt[] checkIns = new CheckInReceipt[INITIAL_CAPACITY];
    private int checkInCount;
    private CheckOutReceipt[] checkOuts = new CheckOutReceipt[INITIAL_CAPACITY];
    private int checkOutCount;
    private String[] loadErrors = new String[0];

    private final StayLinks.RoomGateway rooms;
    private final StayLinks.ServiceGateway services;
    private final StayLinks.PersonGateway persons;
    private final StayLinks.BookingGateway bookings;
    private final StayLinks.InvoiceGateway invoices;
    private final String dataDir;
    private final Clock clock;

    public StayManager(StayLinks.RoomGateway rooms, StayLinks.ServiceGateway services,
                       StayLinks.PersonGateway persons, StayLinks.BookingGateway bookings,
                       StayLinks.InvoiceGateway invoices, String dataDir, Clock clock) {
        this.rooms = Objects.requireNonNull(rooms);
        this.services = Objects.requireNonNull(services);
        this.persons = Objects.requireNonNull(persons);
        this.bookings = Objects.requireNonNull(bookings);
        this.invoices = Objects.requireNonNull(invoices);
        // thư mục dữ liệu không được null hoặc rỗng
        CheckingExpression.checkNotNullOrEmpty(dataDir, "Thư mục dữ liệu không được để trống");
        this.dataDir = dataDir;
        this.clock = Objects.requireNonNull(clock);
    }

    public StayManager(StayLinks.RoomGateway rooms, StayLinks.ServiceGateway services,
                       StayLinks.PersonGateway persons, StayLinks.BookingGateway bookings,
                       StayLinks.InvoiceGateway invoices, String dataDir) {
        this(rooms, services, persons, bookings, invoices, dataDir, Clock.systemDefaultZone());
    }

    /** Dùng cho test: đưa mọi bộ đếm mã về 1. */
    public static void resetAllCounters() {
        StayRecord.resetCounter();
        StayGuest.resetCounter();
        ServiceUsage.resetCounter();
        CheckInReceipt.resetCounter();
        CheckOutReceipt.resetCounter();
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }

    // =====================================================================
    //  NGHIỆP VỤ CHÍNH
    // =====================================================================

    /**
     * Nhận phòng cho MỘT dòng BookingDetail (đoàn đến lẻ tẻ thì gọi nhiều lần, mỗi dòng một lần).
     * code là mã BookingRequest (đã CONFIRMED) hoặc mã Booking đặt trực tiếp.
     * Mọi điều kiện của P4 được kiểm tra TRƯỚC khi nhờ P3 lập Booking, để không bỏ dở giữa chừng.
     */
    public StayRecord checkIn(String code, String bookingDetailId, String[] guestIds,
                              String employeeId, String note) {
        requireText(employeeId, "Thiếu mã nhân viên thực hiện");
        requireText(code, "Thiếu mã phiếu yêu cầu hoặc mã đặt phòng");
        // mã dòng đặt phòng không được rỗng
        requireText(bookingDetailId, "Thiếu mã dòng đặt phòng");
        if (guestIds == null || guestIds.length == 0) throw new IllegalArgumentException("Phải có ít nhất 1 khách");

        StayLinks.BookingLine line = findLine(code, bookingDetailId);
        for (int i = 0; i < count; i++) {
            if (stays[i].getBookingDetailId().equals(bookingDetailId)) {
                if (stays[i].getStatus() == StayStatus.ACTIVE)
                    throw new IllegalStateException("Dòng " + bookingDetailId + " đã nhận phòng (lượt ở " + stays[i].getStayId() + ")");
                if (stays[i].getStatus() == StayStatus.CHECKED_OUT)
                    throw new IllegalStateException("Dòng " + bookingDetailId + " đã trả phòng, không nhận lại được");
            }
        }

        LocalDateTime now = now();
        LocalDate today = now.toLocalDate();
        if (today.isBefore(line.checkInDate))
            throw new IllegalStateException("Chưa đến ngày nhận phòng (" + line.checkInDate + ")");
        if (!today.isBefore(line.checkOutDate))
            throw new IllegalStateException("Đã quá ngày trả phòng (" + line.checkOutDate + "), hãy xử lý no-show");

        String[] ids = validateGuests(guestIds, line.guestCount);

        if (!rooms.isReadyForCheckIn(line.roomId))
            throw new IllegalStateException("Phòng " + line.roomId + " chưa sẵn sàng (đang có khách, dọn dẹp hoặc bảo trì)");
        if (findActiveByRoom(line.roomId) != null)
            throw new IllegalStateException("Phòng " + line.roomId + " đang có lượt ở ACTIVE");

        // Từ đây mới thay đổi dữ liệu.
        String bookingId = bookings.resolveBookingForCheckIn(code, employeeId);
        requireText(bookingId, "P3 không trả về mã Booking");
        if (!rooms.markOccupied(line.roomId))
            throw new IllegalStateException("Không chuyển được phòng " + line.roomId + " sang OCCUPIED");

        StayRecord stay = new StayRecord(bookingId, bookingDetailId, line.roomId);
        for (String id : ids) stay.addGuest(new StayGuest(stay.getStayId(), id, persons.isCustomer(id)));
        stay.checkIn(now);
        appendStay(stay);

        int earlyHours = 0;
        if (today.equals(line.checkInDate) && now.toLocalTime().isBefore(StayRecord.STANDARD_CHECK_IN_TIME)) {
            earlyHours = StayRecord.ceilHours(now, today.atTime(StayRecord.STANDARD_CHECK_IN_TIME));
        }
        appendCheckIn(new CheckInReceipt(stay.getStayId(), now, ids.length, earlyHours, employeeId, note));
        bookings.markCheckedIn(bookingId);
        return stay;
    }

    /** Trả phòng của một lượt ở: lập phiếu trả, chuyển phòng sang CLEANING, nhờ P5 lập Invoice. */
    public CheckOutResult checkOut(String stayId, String employeeId, String roomCondition, String note) {
        requireText(employeeId, "Thiếu mã nhân viên thực hiện");
        StayRecord stay = requireActive(stayId);
        StayLinks.BookingLine line = bookings.getLine(stay.getBookingDetailId());
        LocalDateTime due = line == null ? null : line.checkOutDate.atTime(StayRecord.STANDARD_CHECK_OUT_TIME);
        LocalDateTime now = now();

        CheckOutReceipt receipt = new CheckOutReceipt(stayId, now, roomCondition, due, employeeId, note);
        stay.checkOut(now);
        appendCheckOut(receipt);

        StringBuilder warning = new StringBuilder();
        if (!rooms.markCleaning(stay.getRoomId()))
            warning.append("Không chuyển được phòng ").append(stay.getRoomId()).append(" sang CLEANING. ");

        String invoiceId = null;
        try {
            invoiceId = invoices.createInvoiceForStay(stay, receipt, employeeId);
        } catch (RuntimeException e) {
            warning.append("P5 chưa lập được hóa đơn: ").append(e.getMessage()).append(". ");
        }

        boolean completed = isBookingFullyCheckedOut(stay.getBookingId());
        if (completed) bookings.markCompleted(stay.getBookingId());
        return new CheckOutResult(receipt, invoiceId, completed, warning.length() == 0 ? null : warning.toString().trim());
    }

    /**
     * Đổi phòng giữa kỳ: đóng lượt ở cũ (ROOM_CHANGED), mở lượt ở mới cho phòng mới, giữ nguyên khách.
     * Dịch vụ đã ghi vẫn nằm ở lượt ở cũ để đúng giá từng phòng.
     */
    public StayRecord changeRoom(String stayId, String newRoomId, String employeeId, String reason) {
        requireText(employeeId, "Thiếu mã nhân viên thực hiện");
        requireText(newRoomId, "Thiếu mã phòng mới");
        StayRecord old = requireActive(stayId);
        if (old.getRoomId().equals(newRoomId)) throw new IllegalArgumentException("Phòng mới trùng phòng hiện tại");
        if (!rooms.isReadyForCheckIn(newRoomId)) throw new IllegalStateException("Phòng " + newRoomId + " chưa sẵn sàng");
        if (findActiveByRoom(newRoomId) != null) throw new IllegalStateException("Phòng " + newRoomId + " đang có lượt ở ACTIVE");
        if (!rooms.markOccupied(newRoomId)) throw new IllegalStateException("Không chuyển được phòng " + newRoomId + " sang OCCUPIED");

        LocalDateTime now = now();
        old.closeForRoomChange(now);
        rooms.markCleaning(old.getRoomId());
        appendCheckOut(new CheckOutReceipt(CheckOutReceipt.generateId(), old.getStayId(), now, "Đổi phòng sang " + newRoomId,
                null, 0, employeeId, reason));

        StayRecord fresh = new StayRecord(old.getBookingId(), old.getBookingDetailId(), newRoomId);
        for (StayGuest g : old.getGuests()) fresh.addGuest(new StayGuest(g, fresh.getStayId()));
        fresh.checkIn(now);
        appendStay(fresh);
        appendCheckIn(new CheckInReceipt(fresh.getStayId(), now, fresh.getGuestCount(), 0, employeeId,
                "Đổi từ phòng " + old.getRoomId() + (reason == null || reason.trim().isEmpty() ? "" : ": " + reason)));
        return fresh;
    }

    /** Gia hạn: P3 kiểm tra phòng không bị đặt kế tiếp. false nếu bị từ chối. */
    public boolean extendStay(String stayId, LocalDate newCheckOutDate, String employeeId) {
        requireText(employeeId, "Thiếu mã nhân viên thực hiện");
        StayRecord stay = requireActive(stayId);
        StayLinks.BookingLine line = bookings.getLine(stay.getBookingDetailId());
        if (line == null) throw new IllegalStateException("Không tìm thấy BookingDetail " + stay.getBookingDetailId());
        // chặn null và bắt ngày trả mới phải sau ngày trả hiện tại
        CheckingExpression.checkValidDateRange(line.checkOutDate, newCheckOutDate);
        if (!newCheckOutDate.isAfter(line.checkOutDate)) throw new IllegalArgumentException("Ngày trả mới phải sau ngày trả hiện tại (" + line.checkOutDate + ")");
        return bookings.extendLine(stay.getBookingDetailId(), newCheckOutDate);
    }

    /** Khách không đến: quá giờ quy định thì chuyển phiếu NO_SHOW (P3 xử lý Deposit) và trả phòng về trống. */
    public void processNoShow(String code, String employeeId) {
        requireText(employeeId, "Thiếu mã nhân viên thực hiện");
        // mã phiếu không được rỗng
        requireText(code, "Thiếu mã phiếu yêu cầu hoặc mã đặt phòng");
        StayLinks.BookingLine[] lines = bookings.getLines(code);
        if (lines == null || lines.length == 0) throw new IllegalArgumentException("Không tìm thấy phiếu " + code);
        LocalDate earliest = lines[0].checkInDate;
        for (StayLinks.BookingLine l : lines) {
            if (l.checkInDate.isBefore(earliest)) earliest = l.checkInDate;
            for (int i = 0; i < count; i++) {
                StayRecord s = stays[i];
                if (s.getBookingDetailId().equals(l.bookingDetailId)
                        && (s.getStatus() == StayStatus.ACTIVE || s.getStatus() == StayStatus.CHECKED_OUT)) {
                    throw new IllegalStateException("Khách đã nhận phòng " + l.roomId + ", không thể đánh dấu no-show");
                }
            }
        }
        if (now().isBefore(earliest.atTime(NO_SHOW_CUTOFF_TIME)))
            throw new IllegalStateException("Chưa quá giờ quy định (" + earliest + " " + NO_SHOW_CUTOFF_TIME + ")");
        bookings.markNoShow(code, employeeId);
        for (StayLinks.BookingLine l : lines) rooms.releaseBookedRoom(l.roomId);
    }

    // =====================================================================
    //  DỊCH VỤ SỬ DỤNG
    // =====================================================================

    /** Ghi dịch vụ cho lượt ở đang ACTIVE. Giá được chốt tại thời điểm ghi. */
    public ServiceUsage addServiceUsage(String stayId, String serviceId, double quantity, String employeeId) {
        requireText(employeeId, "Thiếu mã nhân viên thực hiện");
        StayRecord stay = requireActive(stayId);
        // mã dịch vụ không được rỗng
        requireText(serviceId, "Thiếu mã dịch vụ");
        if (!ServiceUsage.isValidQuantity(quantity)) throw new IllegalArgumentException("Số lượng phải là số dương");
        if (!services.isAvailable(serviceId)) throw new IllegalArgumentException("Dịch vụ " + serviceId + " không tồn tại hoặc đã ngưng cung cấp");
        ServiceUsage u = new ServiceUsage(stayId, serviceId, quantity, services.getCurrentUnitPrice(serviceId), now(), employeeId);
        stay.addServiceUsage(u);
        return u;
    }

    /** Sửa số lượng: chỉ khi lượt ở còn ACTIVE, dòng chưa hủy và chưa có hóa đơn. */
    public void updateServiceUsage(String stayId, String usageId, double newQuantity) {
        // mã dòng dịch vụ không được rỗng
        requireText(usageId, "Thiếu mã dòng dịch vụ");
        StayRecord stay = requireActive(stayId);
        ServiceUsage u = requireUsage(stay, usageId);
        if (u.isCancelled()) throw new IllegalStateException("Dòng dịch vụ đã hủy");
        if (invoices.hasInvoice(stayId)) throw new IllegalStateException("Đã lập hóa đơn, không sửa được");
        u.setQuantity(newQuantity);
    }

    /**
     * Hủy dòng dịch vụ. EMPLOYEE không được hủy sau khi đã lập hóa đơn; MANAGER và ADMIN được,
     * nhưng P5 phải xử lý hóa đơn và RefundRecord tương ứng (hàm trả về true nếu cần P5 xử lý).
     * @return true nếu hóa đơn đã tồn tại và P5 cần điều chỉnh
     */
    public boolean cancelServiceUsage(String stayId, String usageId, Role role) {
        // mã dòng dịch vụ không được rỗng
        requireText(usageId, "Thiếu mã dòng dịch vụ");
        // role null sẽ làm điều kiện phân quyền bên dưới sai, ai cũng hủy được
        if (role == null) throw new IllegalArgumentException("Thiếu quyền (Role) của người thực hiện");
        StayRecord stay = requireStay(stayId);
        ServiceUsage u = requireUsage(stay, usageId);
        if (u.isCancelled()) throw new IllegalStateException("Dòng dịch vụ đã hủy rồi");
        boolean invoiced = invoices.hasInvoice(stayId);
        if (invoiced && role == Role.EMPLOYEE)
            throw new SecurityException("Đã lập hóa đơn: cần quản lý hủy dòng này");
        stay.cancelServiceUsage(usageId);
        return invoiced;
    }

    // =====================================================================
    //  TRA CỨU, SẮP XẾP, THỐNG KÊ
    // =====================================================================

    @Override
    public ArrayList<StayRecord> getAll() {
        ArrayList<StayRecord> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(stays[i]);
        }
        return list;
    }

    public StayRecord findById(String stayId) {
        for (int i = 0; i < count; i++) if (stays[i].getStayId().equals(stayId)) return stays[i];
        return null;
    }

    public StayRecord findActiveByRoom(String roomId) {
        for (int i = 0; i < count; i++)
            if (stays[i].isActive() && stays[i].getRoomId().equals(roomId)) return stays[i];
        return null;
    }

    public StayRecord[] findByBooking(String bookingId) {
        return filter(s -> s.getBookingId().equals(bookingId));
    }

    /** P5 dùng: lấy mọi lượt ở của một dòng đặt phòng (kể cả các lượt ROOM_CHANGED) để lập hóa đơn gộp. */
    public StayRecord[] findByBookingDetail(String bookingDetailId) {
        return filter(s -> s.getBookingDetailId().equals(bookingDetailId));
    }

    public StayRecord[] findActive() {
        return filter(StayRecord::isActive);
    }

    /** Lịch sử lưu trú của một khách (Customer hoặc GuestMember). */
    public StayRecord[] findByGuest(String guestId) {
        return filter(s -> s.hasGuest(guestId));
    }

    /** Tìm nhiều khóa: khớp chính xác trước, nếu không có thì tìm gần đúng (chứa chuỗi, không phân biệt hoa thường). */
    public StayRecord[] search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return new StayRecord[0];
        final String k = keyword.trim().toLowerCase();
        StayRecord[] exact = filter(s -> matches(s, k, true));
        return exact.length > 0 ? exact : filter(s -> matches(s, k, false));
    }

    private static boolean matches(StayRecord s, String k, boolean exact) {
        String[] keys = new String[4 + s.getGuestCount()];
        keys[0] = s.getStayId();
        keys[1] = s.getBookingId();
        keys[2] = s.getBookingDetailId();
        keys[3] = s.getRoomId();
        String[] g = s.getGuestIds();
        System.arraycopy(g, 0, keys, 4, g.length);
        for (String key : keys) {
            String v = key.toLowerCase();
            if (exact ? v.equals(k) : v.contains(k)) return true;
        }
        return !exact && s.getStatus().name().toLowerCase().contains(k);
    }

    /** Sắp xếp theo giờ nhận (insertion sort tự viết, không đổi thứ tự trong manager). */
    public StayRecord[] sortedByCheckIn(boolean newestFirst) {
        StayRecord[] a = Arrays.copyOf(stays, count);
        for (int i = 1; i < a.length; i++) {
            StayRecord key = a[i];
            int j = i - 1;
            while (j >= 0 && compareCheckIn(a[j], key, newestFirst) > 0) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
        return a;
    }

    private static int compareCheckIn(StayRecord x, StayRecord y, boolean newestFirst) {
        LocalDateTime a = x.getActualCheckIn(), b = y.getActualCheckIn();
        int c;
        if (a == null && b == null) c = 0;
        else if (a == null) c = -1;
        else if (b == null) c = 1;
        else c = a.compareTo(b);
        return newestFirst ? -c : c;
    }

    public int countByStatus(StayStatus status) {
        int n = 0;
        for (int i = 0; i < count; i++) if (stays[i].getStatus() == status) n++;
        return n;
    }

    /** Tổng tiền dịch vụ (chưa hủy) của mọi lượt ở. */
    public double totalServiceAmount() {
        double sum = 0;
        for (int i = 0; i < count; i++) sum += stays[i].calculateServiceTotal();
        return sum;
    }

    public void printStatistics() {
        System.out.println("---------- THỐNG KÊ LƯU TRÚ ----------");
        for (StayStatus st : StayStatus.values())
            System.out.printf("%-16s: %d%n", st.getLabel(), countByStatus(st));
        System.out.printf("%-16s: %d%n", "Tổng lượt ở", count);
        System.out.printf("%-16s: %,.0f%n", "Tiền dịch vụ", totalServiceAmount());
    }

    public CheckInReceipt findCheckInReceipt(String stayId) {
        for (int i = 0; i < checkInCount; i++) if (checkIns[i].getStayId().equals(stayId)) return checkIns[i];
        return null;
    }

    public CheckOutReceipt findCheckOutReceipt(String stayId) {
        for (int i = 0; i < checkOutCount; i++) if (checkOuts[i].getStayId().equals(stayId)) return checkOuts[i];
        return null;
    }

    public StayLinks.BookingLine[] getLines(String code) {
        StayLinks.BookingLine[] l = bookings.getLines(code);
        return l == null ? new StayLinks.BookingLine[0] : l;
    }

    /** Dòng đặt phòng này đã có lượt ở ACTIVE hoặc CHECKED_OUT chưa. */
    public boolean hasStayForLine(String bookingDetailId) {
        for (int i = 0; i < count; i++) {
            if (stays[i].getBookingDetailId().equals(bookingDetailId) && stays[i].getStatus() != StayStatus.ROOM_CHANGED) return true;
        }
        return false;
    }

    // =====================================================================
    //  CRUD CHUNG (IManager)
    // =====================================================================

    /** Thêm lượt ở có sẵn (dùng khi nạp dữ liệu mẫu). Luồng thật dùng checkIn(). */
    @Override
    public boolean add(StayRecord s) {
        if (s == null || findById(s.getStayId()) != null) return false;
        appendStay(s);
        return true;
    }

    @Override
    public boolean update(StayRecord item) {
        if (item == null) return false;
        return update(item.getStayId(), item);
    }

    /** Sửa danh sách khách của lượt ở đang ACTIVE theo newData (mọi thứ khác giữ nguyên). */
    public boolean update(String stayId, StayRecord newData) {
        StayRecord s = findById(stayId);
        if (s == null || newData == null || !s.isActive() || newData.getGuestCount() == 0) return false;
        StayGuest[] copy = new StayGuest[newData.getGuestCount()];
        StayGuest[] src = newData.getGuests();
        for (int i = 0; i < copy.length; i++) copy[i] = new StayGuest(src[i], stayId);
        s.replaceGuests(copy);
        return true;
    }

    /** Xóa lượt ở: chỉ khi không ACTIVE và chưa có hóa đơn. Xóa luôn phiếu và dịch vụ kèm theo. */
    public boolean remove(String stayId) {
        int idx = indexOf(stayId);
        if (idx < 0 || stays[idx].isActive() || invoices.hasInvoice(stayId)) return false;
        for (int i = idx; i < count - 1; i++) stays[i] = stays[i + 1];
        stays[--count] = null;
        for (int i = 0; i < checkInCount; ) {
            if (checkIns[i].getStayId().equals(stayId)) {
                for (int j = i; j < checkInCount - 1; j++) checkIns[j] = checkIns[j + 1];
                checkIns[--checkInCount] = null;
            } else i++;
        }
        for (int i = 0; i < checkOutCount; ) {
            if (checkOuts[i].getStayId().equals(stayId)) {
                for (int j = i; j < checkOutCount - 1; j++) checkOuts[j] = checkOuts[j + 1];
                checkOuts[--checkOutCount] = null;
            } else i++;
        }
        return true;
    }

    public boolean updateCheckInNote(String stayId, String note) {
        CheckInReceipt r = findCheckInReceipt(stayId);
        if (r == null) return false;
        r.setNote(note);
        return true;
    }

    public boolean updateCheckOutNote(String stayId, String note) {
        CheckOutReceipt r = findCheckOutReceipt(stayId);
        if (r == null) return false;
        r.setNote(note);
        return true;
    }

    public int size() { return count; }

    public int getCheckInReceiptCount() { return checkInCount; }
    public int getCheckOutReceiptCount() { return checkOutCount; }

    public void displayAll() { displayList(Arrays.copyOf(stays, count)); }

    public void displayActiveGuests() { displayList(findActive()); }

    public void displayList(StayRecord[] list) {
        if (list.length == 0) {
            System.out.println("(Không có dữ liệu)");
            return;
        }
        System.out.printf("%-6s | %-8s | %-6s | %-14s | %-16s | %s%n", "Mã", "Booking", "Phòng", "Trạng thái", "Giờ nhận", "Khách");
        for (StayRecord s : list) {
            StringBuilder names = new StringBuilder();
            for (String id : s.getGuestIds()) {
                if (names.length() > 0) names.append(", ");
                String n = persons.getGuestName(id);
                names.append(n == null ? id : n + " (" + id + ")");
            }
            System.out.printf("%-6s | %-8s | %-6s | %-14s | %-16s | %s%n", s.getStayId(), s.getBookingId(), s.getRoomId(),
                    s.getStatus().getLabel(),
                    s.getActualCheckIn() == null ? "-" : s.getActualCheckIn().toLocalDate() + " " + s.getActualCheckIn().toLocalTime().withNano(0).toString().substring(0, 5),
                    names);
        }
    }

    public void displayUsages(String stayId) {
        StayRecord s = findById(stayId);
        if (s == null) { System.out.println("Không tìm thấy lượt ở " + stayId); return; }
        if (s.getUsageCount() == 0) { System.out.println("(Chưa có dịch vụ nào)"); return; }
        for (ServiceUsage u : s.getUsages()) System.out.println("  " + u);
        System.out.printf("  Tổng tiền dịch vụ: %,.0f%n", s.calculateServiceTotal());
    }

    // =====================================================================
    //  ĐỌC / GHI FILE
    // =====================================================================

    /**
     * Thứ tự nạp: stays, guests, usages, receipts. Bản ghi hỏng hoặc tham chiếu không tồn tại
     * thì bỏ qua và ghi vào danh sách lỗi (getLoadErrors), không dừng chương trình.
     * Phải gọi SAU khi Employee, Customer, Room, Service, Booking đã nạp (mục 6.1).
     */
    public void readFromFile() {
        count = checkInCount = checkOutCount = 0;
        stays = new StayRecord[INITIAL_CAPACITY];
        checkIns = new CheckInReceipt[INITIAL_CAPACITY];
        checkOuts = new CheckOutReceipt[INITIAL_CAPACITY];
        String[] errors = new String[0];

        try {
            for (String line : FileUtil.readAllLines(path(STAY_FILE))) {
                try {
                    StayRecord s = StayRecord.fromFileLine(line);
                    if (findById(s.getStayId()) != null) throw new IllegalArgumentException("Trùng mã " + s.getStayId());
                    if (bookings.getLine(s.getBookingDetailId()) == null)
                        throw new IllegalArgumentException("BookingDetail " + s.getBookingDetailId() + " không tồn tại");
                    appendStay(s);
                    StayRecord.updateCounterFromId(s.getStayId());
                } catch (RuntimeException e) {
                    errors = addError(errors, STAY_FILE, line, e);
                }
            }
            for (String line : FileUtil.readAllLines(path(GUEST_FILE))) {
                try {
                    StayGuest g = StayGuest.fromFileLine(line);
                    StayRecord s = findById(g.getStayId());
                    if (s == null) throw new IllegalArgumentException("Lượt ở " + g.getStayId() + " không tồn tại");
                    s.addGuest(g);
                    StayGuest.updateCounterFromId(g.getId());
                } catch (RuntimeException e) {
                    errors = addError(errors, GUEST_FILE, line, e);
                }
            }
            for (String line : FileUtil.readAllLines(path(USAGE_FILE))) {
                try {
                    ServiceUsage u = ServiceUsage.fromFileLine(line);
                    StayRecord s = findById(u.getStayId());
                    if (s == null) throw new IllegalArgumentException("Lượt ở " + u.getStayId() + " không tồn tại");
                    if (!u.isValidQuantity()) throw new IllegalArgumentException("Số lượng không hợp lệ");
                    s.loadUsage(u);
                    ServiceUsage.updateCounterFromId(u.getUsageId());
                } catch (RuntimeException e) {
                    errors = addError(errors, USAGE_FILE, line, e);
                }
            }
            for (String line : FileUtil.readAllLines(path(CHECKIN_FILE))) {
                try {
                    CheckInReceipt r = CheckInReceipt.fromFileLine(line);
                    if (findById(r.getStayId()) == null) throw new IllegalArgumentException("Lượt ở " + r.getStayId() + " không tồn tại");
                    appendCheckIn(r);
                    CheckInReceipt.updateCounterFromId(r.getReceiptId());
                } catch (RuntimeException e) {
                    errors = addError(errors, CHECKIN_FILE, line, e);
                }
            }
            for (String line : FileUtil.readAllLines(path(CHECKOUT_FILE))) {
                try {
                    CheckOutReceipt r = CheckOutReceipt.fromFileLine(line);
                    if (findById(r.getStayId()) == null) throw new IllegalArgumentException("Lượt ở " + r.getStayId() + " không tồn tại");
                    appendCheckOut(r);
                    CheckOutReceipt.updateCounterFromId(r.getReceiptId());
                } catch (RuntimeException e) {
                    errors = addError(errors, CHECKOUT_FILE, line, e);
                }
            }
            loadErrors = errors;
        } catch (IOException e) {
            System.out.println("Không đọc được dữ liệu lưu trú: " + e.getMessage());
        }
    }

    public void readFile() {
        readFromFile();
    }

    public void writeFile() {
        writeToFile();
    }

    public void writeToFile() {
        try {
            String[] lines = new String[count];
            for (int i = 0; i < count; i++) lines[i] = stays[i].toFileLine();
            FileUtil.writeAllLines(path(STAY_FILE), lines, count);

            String[] guestLines = new String[0];
            String[] usageLines = new String[0];
            int gn = 0, un = 0;
            for (int i = 0; i < count; i++) {
                for (StayGuest g : stays[i].getGuests()) {
                    if (gn == guestLines.length) guestLines = Arrays.copyOf(guestLines, Math.max(8, gn * 2));
                    guestLines[gn++] = g.toFileLine();
                }
                for (ServiceUsage u : stays[i].getUsages()) {
                    if (un == usageLines.length) usageLines = Arrays.copyOf(usageLines, Math.max(8, un * 2));
                    usageLines[un++] = u.toFileLine();
                }
            }
            FileUtil.writeAllLines(path(GUEST_FILE), guestLines, gn);
            FileUtil.writeAllLines(path(USAGE_FILE), usageLines, un);

            String[] ci = new String[checkInCount];
            for (int i = 0; i < checkInCount; i++) ci[i] = checkIns[i].toFileLine();
            FileUtil.writeAllLines(path(CHECKIN_FILE), ci, checkInCount);
            String[] co = new String[checkOutCount];
            for (int i = 0; i < checkOutCount; i++) co[i] = checkOuts[i].toFileLine();
            FileUtil.writeAllLines(path(CHECKOUT_FILE), co, checkOutCount);
        } catch (IOException e) {
            System.out.println("Không ghi được dữ liệu lưu trú: " + e.getMessage());
        }
    }

    public String[] getLoadErrors() { return loadErrors; }

    private String path(String name) { return dataDir + "/" + name; }

    private static String[] addError(String[] errors, String file, String line, RuntimeException e) {
        String[] r = Arrays.copyOf(errors, errors.length + 1);
        r[errors.length] = file + ": bỏ qua dòng [" + line + "] - " + e.getMessage();
        return r;
    }

    // =====================================================================
    //  HÀM NỘI BỘ
    // =====================================================================

    private interface StayFilter { boolean accept(StayRecord s); }

    private StayRecord[] filter(StayFilter f) {
        StayRecord[] r = new StayRecord[count];
        int n = 0;
        for (int i = 0; i < count; i++) if (f.accept(stays[i])) r[n++] = stays[i];
        return Arrays.copyOf(r, n);
    }

    private int indexOf(String stayId) {
        for (int i = 0; i < count; i++) if (stays[i].getStayId().equals(stayId)) return i;
        return -1;
    }

    private void appendStay(StayRecord s) {
        if (count == stays.length) stays = Arrays.copyOf(stays, count * 2);
        stays[count++] = s;
    }

    private void appendCheckIn(CheckInReceipt r) {
        if (checkInCount == checkIns.length) checkIns = Arrays.copyOf(checkIns, checkInCount * 2);
        checkIns[checkInCount++] = r;
    }

    private void appendCheckOut(CheckOutReceipt r) {
        if (checkOutCount == checkOuts.length) checkOuts = Arrays.copyOf(checkOuts, checkOutCount * 2);
        checkOuts[checkOutCount++] = r;
    }

    private StayLinks.BookingLine findLine(String code, String bookingDetailId) {
        StayLinks.BookingLine[] lines = bookings.getLines(code);
        if (lines == null || lines.length == 0) throw new IllegalArgumentException("Không tìm thấy phiếu " + code);
        for (StayLinks.BookingLine l : lines) if (l.bookingDetailId.equals(bookingDetailId)) return l;
        throw new IllegalArgumentException("Dòng " + bookingDetailId + " không thuộc phiếu " + code);
    }

    /** Kiểm tra danh sách khách: tồn tại, đủ giấy tờ, không trùng, không vượt số khách đã đặt. */
    private String[] validateGuests(String[] guestIds, int maxGuests) {
        String[] ids = new String[guestIds.length];
        for (int i = 0; i < guestIds.length; i++) {
            String id = guestIds[i] == null ? "" : guestIds[i].trim();
            if (id.isEmpty()) throw new IllegalArgumentException("Mã khách rỗng");
            for (int j = 0; j < i; j++) if (ids[j].equals(id)) throw new IllegalArgumentException("Khách " + id + " bị nhập trùng");
            if (!persons.isCustomer(id) && !persons.isGuestMember(id)) throw new IllegalArgumentException("Không tìm thấy khách " + id);
            if (!persons.isGuestInfoComplete(id)) throw new IllegalArgumentException("Khách " + id + " chưa đủ giấy tờ hoặc thông tin");
            ids[i] = id;
        }
        if (ids.length > maxGuests) throw new IllegalArgumentException("Số khách (" + ids.length + ") vượt số khách đã đặt (" + maxGuests + ")");
        return ids;
    }

    private boolean isBookingFullyCheckedOut(String bookingId) {
        StayLinks.BookingLine[] lines = bookings.getLines(bookingId);
        if (lines == null || lines.length == 0) return false;
        for (StayLinks.BookingLine l : lines) {
            boolean done = false;
            for (int i = 0; i < count && !done; i++)
                done = stays[i].getBookingDetailId().equals(l.bookingDetailId) && stays[i].getStatus() == StayStatus.CHECKED_OUT;
            if (!done) return false;
        }
        return true;
    }

    private StayRecord requireStay(String stayId) {
        StayRecord s = findById(stayId);
        if (s == null) throw new IllegalArgumentException("Không tìm thấy lượt ở " + stayId);
        return s;
    }

    private StayRecord requireActive(String stayId) {
        StayRecord s = requireStay(stayId);
        if (!s.isActive()) throw new IllegalStateException("Lượt ở " + stayId + " không còn ACTIVE (" + s.getStatus().getLabel() + ")");
        return s;
    }

    private ServiceUsage requireUsage(StayRecord s, String usageId) {
        ServiceUsage u = s.findUsage(usageId);
        if (u == null) throw new IllegalArgumentException("Không tìm thấy dòng dịch vụ " + usageId + " trong lượt ở " + s.getStayId());
        return u;
    }

    private static void requireText(String s, String message) {
        // đổi if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(message);
        CheckingExpression.checkNotNullOrEmpty(s, message);
    }
}



