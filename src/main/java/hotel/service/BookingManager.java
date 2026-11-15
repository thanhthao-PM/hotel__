package hotel.service;

import hotel.model.Booking;
import hotel.model.BookingDetail;
import hotel.model.BookingList;
import hotel.model.BookingRequest;
import hotel.model.BookingRequestList;
import hotel.model.Deposit;
import hotel.model.IndividualBooking;
import hotel.model.GroupBooking;
import hotel.model.enums.BookingStatus;
// import hotel.model.enums.DepositStatus;
import hotel.model.enums.PaymentMethod;
import hotel.model.enums.RequestStatus;
import hotel.util.CheckingExpression;

import java.time.LocalDate;
import java.util.List;

/**
 * Lớp Quản lý các đơn Đặt phòng, Yêu cầu đặt phòng và Tiền cọc (Module P3).
 * Cập nhật theo luồng nghiệp vụ mới nhất: BookingRequest -> Deposit -> Booking.
 */
public class BookingManager {
    private final BookingList bookingList;
    private final BookingRequestList requestList;

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
        if (roomId == null || checkIn == null || checkOut == null) return false;
        if (!checkOut.isAfter(checkIn)) return false;

        // 1. Kiểm tra trên danh sách BookingRequest đang giữ chỗ (PENDING_DEPOSIT hoặc CONFIRMED)
        List<BookingRequest> allRequests = requestList.getAll();
        for (BookingRequest req : allRequests) {
            if (req.isActive()) {
                BookingDetail[] details = req.getDetails();
                for (int i = 0; i < req.getDetailCount(); i++) {
                    BookingDetail d = details[i];
                    if (d != null && roomId.equals(d.getOriginalRoomId())) {
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
                containsRoom = roomId.equals(((IndividualBooking) b).getRoomId());
            } else if (b instanceof GroupBooking) {
                String[] roomIds = ((GroupBooking) b).getRoomIds();
                if (roomIds != null) {
                    for (String rId : roomIds) {
                        if (roomId.equals(rId)) {
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
        if (request == null) return false;
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
        // số tiền cọc phải dương (chặn sớm, trước khi tạo mã cọc và đổi dữ liệu)
        CheckingExpression.checkPositiveDouble(amount, "Số tiền cọc phải > 0");
        // phiếu chưa có dòng phòng nào thì chia cọc sẽ ra Infinity hoặc NaN
        CheckingExpression.checkDivideByZero(req.getDetailCount(), "Phiếu yêu cầu chưa có phòng nào để chia tiền cọc");
        if (req == null || req.getRequestStatus() != RequestStatus.PENDING_DEPOSIT) {
            return null;
        }

        // Tạo phiếu cọc mới
        String depositId = generateDepositId();
        Deposit deposit = new Deposit(depositId, requestId, amount, method, receivedBy, referenceNo);
        
        // Phân bổ tiền cọc vào các dòng BookingDetail của Request
        double depositPerRoom = amount / req.getDetailCount();
        BookingDetail[] details = req.getDetails();
        for (int i = 0; i < req.getDetailCount(); i++) {
            if (details[i] != null) {
                details[i].setDepositShare(depositPerRoom);
            }
        }

        // Cập nhật trạng thái Request sang CONFIRMED
        req.confirmDeposit();
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
     * Hủy Yêu cầu đặt phòng (Duyệt bởi Quản lý)
     */
    public boolean cancelBookingRequest(String requestId, String reason, boolean refundDeposit, Deposit deposit) {
        BookingRequest req = findRequestById(requestId);
        if (req == null || !req.isActive()) return false;

        req.cancel(reason);
        if (deposit != null) {
            if (refundDeposit) {
                deposit.refund();
            } else {
                deposit.forfeit(); // Tịch thu cọc (mất cọc)
            }
        }
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
        if (req == null || req.getRequestStatus() != RequestStatus.CONFIRMED) {
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
                details[i].setBookingId(bookingId);
            }
            // Truyền đúng 10 tham số chuẩn của GroupBooking
            booking = new GroupBooking(bookingId, req.getRequestId(), req.getCustomerId(), 
                    req.getExpectedCheckInDate(), req.getExpectedCheckOutDate(), 
                    employeeId, note, req.getGroupName(), roomIds, 20);
        } else {
            // chặn truy cập details[0] khi phiếu chưa có dòng nào (tránh NullPointerException)
            CheckingExpression.checkValidArrayIndex(0, req.getDetailCount());
            BookingDetail detail = req.getDetails()[0];
            detail.setBookingId(bookingId);
            // Truyền đúng 9 tham số chuẩn của IndividualBooking
            booking = new IndividualBooking(bookingId, req.getRequestId(), req.getCustomerId(), 
                    req.getExpectedCheckInDate(), req.getExpectedCheckOutDate(), 
                    employeeId, note, detail.getOriginalRoomId(), detail.getGuestCount());
        }

        if (addBooking(booking)) {
            req.expire();
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