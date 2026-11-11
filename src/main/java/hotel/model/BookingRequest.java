package hotel.model;

import hotel.model.enums.RequestStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingRequest {
    private String requestId;
    private String customerId;
    private String bookingType;             // "INDIVIDUAL" hoặc "GROUP"
    private String groupName;
    private LocalDate expectedCheckInDate;
    private LocalDate expectedCheckOutDate;
    private LocalDateTime depositDueAt;
    private String cancelReason;
    private RequestStatus requestStatus;
    private String createdBy;
    private LocalDateTime createdAt;
    
    private BookingDetail[] details;
    private int detailCount;

    public BookingRequest(String requestId, String customerId, String bookingType, String groupName, 
                          LocalDate expectedCheckIn, LocalDate expectedCheckOut, 
                          String createdBy, int maxDetails) {
        this.requestId = requestId;
        this.customerId = customerId;
        this.bookingType = bookingType;
        this.groupName = groupName;
        this.expectedCheckInDate = expectedCheckIn;
        this.expectedCheckOutDate = expectedCheckOut;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.depositDueAt = this.createdAt.plusHours(24);
        this.requestStatus = RequestStatus.PENDING_DEPOSIT;
        this.details = new BookingDetail[maxDetails];
        this.detailCount = 0;
    }

    public void addDetail(BookingDetail detail) {
        if (detailCount < details.length) {
            details[detailCount++] = detail;
        }
    }

    public double calculateRequiredDeposit() {
        double total = 0;
        for (int i = 0; i < detailCount; i++) {
            if (details[i] != null) {
                total += details[i].calculateRoomAmount();
            }
        }
        return total * 0.3; // Yêu cầu cọc 30%
    }

    public void confirmDeposit() {
        this.requestStatus = RequestStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        this.cancelReason = reason;
        this.requestStatus = RequestStatus.CANCELLED;
    }

    public void expire() {
        this.requestStatus = RequestStatus.EXPIRED;
    }

    public boolean isActive() {
        return this.requestStatus == RequestStatus.PENDING_DEPOSIT || 
               this.requestStatus == RequestStatus.CONFIRMED;
    }

    // --- BỔ SUNG CÁC GETTER BỊ THIẾU ĐỂ HẾT LỖI TRONG BOOKINGMANAGER ---
    public String getRequestId() { return requestId; }
    public String getCustomerId() { return customerId; }
    public String getBookingType() { return bookingType; }               // Sửa lỗi getBookingType()
    public String getGroupName() { return groupName; }                   // Sửa lỗi getGroupName()
    public LocalDate getExpectedCheckInDate() { return expectedCheckInDate; } // Sửa lỗi getExpectedCheckInDate()
    public LocalDate getExpectedCheckOutDate() { return expectedCheckOutDate; } // Sửa lỗi getExpectedCheckOutDate()
    public RequestStatus getRequestStatus() { return requestStatus; }
    public BookingDetail[] getDetails() { return details; }
    public int getDetailCount() { return detailCount; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDepositDueAt() { return depositDueAt; }
    public String getCancelReason() { return cancelReason; }
}