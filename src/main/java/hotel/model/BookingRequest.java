package hotel.model;

import hotel.model.enums.RequestStatus;
import hotel.util.CheckingExpression;  
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingRequest {
    private String requestId;
    private String customerId;
    private String bookingId;
    private String bookingType;             // "INDIVIDUAL" hoặc "GROUP"
    private String groupName;
    private LocalDate expectedCheckInDate;
    private LocalDate expectedCheckOutDate;
    private LocalDateTime depositDueAt;
    private String cancelReason;
    private boolean cancellationPending;
    private RequestStatus requestStatus;
    private String createdBy;
    private LocalDateTime createdAt;
    
    private BookingDetail[] details;
    private int detailCount;

    public BookingRequest(String requestId, String customerId, String bookingType, String groupName, 
                          LocalDate expectedCheckIn, LocalDate expectedCheckOut, 
                          String createdBy, int maxDetails) {
        // kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(requestId, "Mã phiếu yêu cầu không được để trống");
        CheckingExpression.checkNotNullOrEmpty(customerId, "Mã khách hàng không được để trống");
        CheckingExpression.checkNotNullOrEmpty(bookingType, "Loại đặt phòng không được để trống");
        if (!"INDIVIDUAL".equals(bookingType) && !"GROUP".equals(bookingType)) {
            throw new IllegalArgumentException("Loại đặt phòng phải là INDIVIDUAL hoặc GROUP");
        }
        if ("GROUP".equals(bookingType)) {
            CheckingExpression.checkNotNullOrEmpty(groupName, "Tên đoàn không được để trống khi đặt phòng đoàn");
        }
        CheckingExpression.checkValidDateRange(expectedCheckIn, expectedCheckOut);
        CheckingExpression.checkNotNullOrEmpty(createdBy, "Mã nhân viên nhận yêu cầu không được để trống");
        CheckingExpression.checkPositiveInt(maxDetails, "Số dòng chi tiết tối đa phải > 0");


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
        // không cho thêm dòng null
        if (detail == null) {
            throw new IllegalArgumentException("Chi tiết đặt phòng không được null");
        }

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
        // bắt buộc có lý do hủy
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do hủy không được để trống");
        this.cancelReason = reason;
        this.cancellationPending = false;
        this.requestStatus = RequestStatus.CANCELLED;
    }

    public void expire() {
        this.requestStatus = RequestStatus.EXPIRED;
    }

    public void convert() {
        this.requestStatus = RequestStatus.CONVERTED;
        this.cancellationPending = false;
    }

    public void requestCancellation(String reason) {
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do hủy không được để trống");
        if (!isActive()) {
            throw new IllegalStateException("Phiếu không còn hiệu lực để đề nghị hủy");
        }
        this.cancelReason = reason;
        this.cancellationPending = true;
    }

    public void rejectCancellation() {
        if (!cancellationPending) {
            throw new IllegalStateException("Phiếu không có đề nghị hủy đang chờ duyệt");
        }
        this.cancellationPending = false;
        this.cancelReason = null;
    }

    public boolean isActive() {
        return this.requestStatus == RequestStatus.PENDING_DEPOSIT || 
               this.requestStatus == RequestStatus.CONFIRMED;
    }

    public String getRequestId() { return requestId; }
    public String getCustomerId() { return customerId; }
    public String getBookingId() { return bookingId; }
    public String getBookingType() { return bookingType; }
    public String getGroupName() { return groupName; }
    public LocalDate getExpectedCheckInDate() { return expectedCheckInDate; }
    public LocalDate getExpectedCheckOutDate() { return expectedCheckOutDate; }
    public RequestStatus getRequestStatus() { return requestStatus; }
    public BookingDetail[] getDetails() { return details; }
    public int getDetailCount() { return detailCount; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDepositDueAt() { return depositDueAt; }
    public String getCancelReason() { return cancelReason; }
    public boolean isCancellationPending() { return cancellationPending; }

    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setExpectedCheckInDate(LocalDate expectedCheckInDate) { this.expectedCheckInDate = expectedCheckInDate; }
    public void setExpectedCheckOutDate(LocalDate expectedCheckOutDate) { this.expectedCheckOutDate = expectedCheckOutDate; }
    public void setDepositDueAt(LocalDateTime depositDueAt) { this.depositDueAt = depositDueAt; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public void setRequestStatus(RequestStatus requestStatus) { this.requestStatus = requestStatus; }
    public void setCancellationPending(boolean cancellationPending) { this.cancellationPending = cancellationPending; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDetails(BookingDetail[] details) { this.details = details; }
    public void setDetailCount(int detailCount) { this.detailCount = detailCount; }
}