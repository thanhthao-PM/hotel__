package hotel.model;

import hotel.model.enums.DepositStatus;
import hotel.model.enums.PaymentMethod;
import hotel.util.CheckingExpression; 
import java.time.LocalDateTime;

public class Deposit {
    private String depositId;
    private String requestId;
    private String bookingId;
    private double amount;
    private PaymentMethod method;
    private LocalDateTime paidAt;
    private String receivedBy;
    private String referenceNo;
    private DepositStatus status;

    public Deposit(String depositId, String requestId, double amount, PaymentMethod method, 
                   String receivedBy, String referenceNo) {
        // THÊM: kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(depositId, "Mã tiền cọc không được để trống");
        CheckingExpression.checkNotNullOrEmpty(requestId, "Mã phiếu yêu cầu không được để trống");
        CheckingExpression.checkPositiveDouble(amount, "Số tiền cọc phải > 0");
        if (method == null) {
            throw new IllegalArgumentException("Phương thức thanh toán không được null");
        }
        CheckingExpression.checkNotNullOrEmpty(receivedBy, "Mã nhân viên thu cọc không được để trống");


        this.depositId = depositId;
        this.requestId = requestId;
        this.amount = amount;
        this.method = method;
        this.receivedBy = receivedBy;
        this.referenceNo = referenceNo;
        this.paidAt = LocalDateTime.now();
        this.status = DepositStatus.HELD;
    }

    public Deposit(String depositId, String requestId, String bookingId, double amount,
                   PaymentMethod method, LocalDateTime paidAt, String receivedBy,
                   String referenceNo, DepositStatus status) {
        CheckingExpression.checkNotNullOrEmpty(depositId, "Mã tiền cọc không được để trống");
        CheckingExpression.checkAtLeastOneNotNull(requestId, bookingId,
                "Tiền cọc phải gắn với yêu cầu hoặc đặt phòng");
        CheckingExpression.checkPositiveDouble(amount, "Số tiền cọc phải > 0");
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Số tiền cọc phải hữu hạn");
        if (method == null || status == null || paidAt == null) {
            throw new IllegalArgumentException("Thông tin thanh toán không được để trống");
        }
        CheckingExpression.checkNotNullOrEmpty(receivedBy, "Mã nhân viên thu cọc không được để trống");
        this.depositId = depositId;
        this.requestId = requestId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.paidAt = paidAt;
        this.receivedBy = receivedBy;
        this.referenceNo = referenceNo;
        this.status = status;
    }

    public void markApplied(String bookingId) {
        // phải có mã Booking để gán
        CheckingExpression.checkNotNullOrEmpty(bookingId, "Mã đặt phòng không được để trống");
        this.bookingId = bookingId;
        this.status = DepositStatus.APPLIED;
    }

    public void refund() {
        this.status = DepositStatus.REFUNDED;
    }

    public void forfeit() {
        this.status = DepositStatus.FORFEITED;
    }

    // Getters & Setters
    public String getDepositId() { return depositId; }
    public String getRequestId() { return requestId; }
    public String getBookingId() { return bookingId; }
    public double getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getReceivedBy() { return receivedBy; }
    public String getReferenceNo() { return referenceNo; }
    public DepositStatus getStatus() { return status; }

    public void setDepositId(String depositId) { this.depositId = depositId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public void setStatus(DepositStatus status) { this.status = status; }
}