package hotel.model;

import hotel.model.enums.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class Booking {
    protected String bookingId;
    protected String requestId;              // Mã BookingRequest gốc chuyển sang[cite: 9]
    protected String customerId;
    protected LocalDate checkInDate;         // Lấy từ ngày sớm nhất của các BookingDetail[cite: 9]
    protected LocalDate expectedCheckOutDate;// Lấy từ ngày muộn nhất của các BookingDetail[cite: 9]
    protected BookingStatus status;
    protected LocalDateTime createdAt;
    protected String createdBy;
    protected String note;

    public Booking(String bookingId, String requestId, String customerId, 
                   LocalDate checkInDate, LocalDate expectedCheckOutDate, 
                   String createdBy, String note) {
        this.bookingId = bookingId;
        this.requestId = requestId;
        this.customerId = customerId;
        this.checkInDate = checkInDate;
        this.expectedCheckOutDate = expectedCheckOutDate;
        this.createdBy = createdBy;
        this.note = note;
        this.status = BookingStatus.CONFIRMED; // Khi tạo Booking chính thức là đã CONFIRMED[cite: 9]
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Lấy danh sách đối tượng Room lấy từ các BookingDetail[cite: 9]
     */
    public abstract Room[] getRooms();

    public long calculateNights() {
        if (checkInDate == null || expectedCheckOutDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(checkInDate, expectedCheckOutDate);
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    // --- GETTERS & SETTERS ---
    public String getBookingId() { return bookingId; }
    public String getRequestId() { return requestId; }
    public String getCustomerId() { return customerId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getExpectedCheckOutDate() { return expectedCheckOutDate; }
    public BookingStatus getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getNote() { return note; }

    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setRequestID(String requestId) { this.requestId = requestId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public void setExpectedCheckOutDate(LocalDate expectedCheckOutDate) { this.expectedCheckOutDate = expectedCheckOutDate; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setNote(String note) { this.note = note; }
}