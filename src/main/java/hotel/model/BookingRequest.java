package hotel.model;

import hotel.model.enums.RequestStatus;
import hotel.util.CheckingExpression;  
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