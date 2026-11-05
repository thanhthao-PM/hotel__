package hotel.model;

import hotel.model.enums.DepositStatus;
import hotel.model.enums.PaymentMethod;
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
        this.depositId = depositId;
        this.requestId = requestId;
        this.amount = amount;
        this.method = method;
        this.receivedBy = receivedBy;
        this.referenceNo = referenceNo;
        this.paidAt = LocalDateTime.now();
        this.status = DepositStatus.HELD;
    }

    public void markApplied(String bookingId) {
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
}