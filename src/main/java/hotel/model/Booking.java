package hotel.model;

import hotel.model.enums.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class Booking {
    // Thuộc tính protected để các lớp con (IndividualBooking, GroupBooking) truy cập trực tiếp
    protected String bookingId;
    protected String customerId;             // Mã Customer đại diện (dùng khi lưu/đọc file)
    protected LocalDate checkInDate;         // Ngày nhận phòng dự kiến
    protected LocalDate expectedCheckOutDate;// Ngày trả phòng dự kiến
    protected BookingStatus status;          // Trạng thái đơn đặt
    protected double deposit;                // Tiền đặt cọc
    protected LocalDateTime createdAt;       // Thời điểm tạo đơn
    protected String createdBy;              // Mã Employee tạo đơn
    protected String note;                   // Ghi chú bổ sung

    // Constructor khởi tạo đầy đủ tham số
    public Booking(String bookingId, String customerId, LocalDate checkInDate, 
                   LocalDate expectedCheckOutDate, double deposit, String createdBy, String note) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.checkInDate = checkInDate;
        this.expectedCheckOutDate = expectedCheckOutDate;
        this.deposit = deposit;
        this.createdBy = createdBy;
        this.note = note;
        this.status = BookingStatus.PENDING; // Mặc định khi vừa tạo là PENDING
        this.createdAt = LocalDateTime.now(); // Tự động lấy thời gian hiện tại của hệ thống
    }

    /**
     * Phương thức trừu tượng quan trọng nhất: Lấy danh sách phòng thuộc đơn đặt này.
     * Đơn cá nhân trả về 1 phòng, Đơn đoàn trả về mảng nhiều phòng.
     */
    public abstract Room[] getRooms();

    /**
     * Tính số đêm lưu trú dựa trên ngày nhận và ngày trả dự kiến.
     */
    public long calculateNights() {
        if (checkInDate == null || expectedCheckOutDate == null) {
            return 0;
        }
        // Sử dụng ChronoUnit.DAYS để tính khoảng cách số ngày giữa 2 LocalDate
        return java.time.temporal.ChronoUnit.DAYS.between(checkInDate, expectedCheckOutDate);
    }

    /**
     * Chuyển trạng thái đơn sang CANCELLED
     */
    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    /**
     * Đổi trạng thái đơn đặt phòng có kiểm soát
     */
    public boolean changeStatus(BookingStatus newStatus) {
        if (newStatus == null) return false;
        this.status = newStatus;
        return true;
    }

    // --- GETTER & SETTER ---
    public String getBookingId() { return bookingId; }
    public String getCustomerId() { return customerId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getExpectedCheckOutDate() { return expectedCheckOutDate; }
    public BookingStatus getStatus() { return status; }
    public double getDeposit() { return deposit; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public String getNote() { return note; }

    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public void setExpectedCheckOutDate(LocalDate expectedCheckOutDate) { this.expectedCheckOutDate = expectedCheckOutDate; }
    public void setDeposit(double deposit) { this.deposit = deposit; }
    public void setNote(String note) { this.note = note; }
}