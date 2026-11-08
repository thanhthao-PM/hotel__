package hotel.model;

import hotel.util.CheckingExpression;
import java.time.LocalDateTime;

public class Payment {
    private String paymentId;
    private String invoiceId;
    private double amount;
    private Invoice.PaymentMethod method;
    private LocalDateTime paidAt;
    private String receivedBy;
    private String referenceNo;

    public Payment(String paymentId, String invoiceId, double amount, Invoice.PaymentMethod method, String receivedBy, String referenceNo) {
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        setAmount(amount);
        this.method = method;
        this.paidAt = LocalDateTime.now();
        this.receivedBy = receivedBy;
        this.referenceNo = referenceNo;
    }

    public void setAmount(double amount) {
        CheckingExpression.checkPositiveDouble(amount, "Số tiền thanh toán phải > 0"); 
        this.amount = amount;
    }
    
    public double getAmount() { return amount; }
}