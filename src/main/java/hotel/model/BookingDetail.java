package hotel.model;

import java.time.LocalDate;

public class BookingDetail {
    private String bookingDetailId; // Mã chi tiết
    private String requestId;       // Mã BookingRequest liên kết
    private String bookingId;       // Mã Booking liên kết (để rỗng/null khi mới chỉ là Request)
    private String originalRoomId;  // Mã phòng ban đầu đặt
    private int guestCount;         // Số lượng khách của phòng này
    private double bookingUnitPrice;// Giá phòng tại thời điểm đặt
    private double depositShare;    // Tiền cọc phân bổ riêng cho phòng này
    private LocalDate checkInDate;  // Ngày nhận phòng
    private LocalDate checkOutDate; // Ngày trả phòng
    private String notes;           // Ghi chú

    public BookingDetail(String bookingDetailId, String requestId, String originalRoomId, 
                         int guestCount, double bookingUnitPrice, double depositShare, 
                         LocalDate checkInDate, LocalDate checkOutDate, String notes) {
        this.bookingDetailId = bookingDetailId;
        this.requestId = requestId;
        this.bookingId = null; // Mặc định rỗng khi mới ở giai đoạn Request
        this.originalRoomId = originalRoomId;
        this.guestCount = guestCount;
        this.bookingUnitPrice = bookingUnitPrice;
        this.depositShare = depositShare;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.notes = notes;
    }

    /**
     * Tính tổng tiền phòng của dòng chi tiết này
     */
    public double calculateRoomAmount() {
        if (checkInDate == null || checkOutDate == null) return 0;
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return nights * bookingUnitPrice;
    }

    /**
     * Nâng phòng lúc nhận phòng (Upgrade room)
     */
    public void upgradeRoom(String newRoomId, double newPrice) {
        this.originalRoomId = newRoomId;
        this.bookingUnitPrice = newPrice;
    }

    // --- GETTERS & SETTERS ---
    public String getBookingDetailId() { return bookingDetailId; }
    public String getRequestId() { return requestId; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getOriginalRoomId() { return originalRoomId; }
    public int getGuestCount() { return guestCount; }
    public double getBookingUnitPrice() { return bookingUnitPrice; }
    public double getDepositShare() { return depositShare; }
    public void setDepositShare(double depositShare) { this.depositShare = depositShare; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public String getNotes() { return notes; }
}