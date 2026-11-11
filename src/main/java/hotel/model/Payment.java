package hotel.model;

import hotel.util.CheckingExpression;
import java.time.LocalDateTime;

// Class Ghi nhận Thanh toán
public class Payment {
    private String paymentId;
    private String invoiceId;
    private double amount; // Số tiền khách đưa
    private Invoice.PaymentMethod method; // Tiền mặt, Thẻ...
    private LocalDateTime paidAt; // Lúc mấy giờ
    private String receivedBy; // Tên nhân viên thu tiền
    private String referenceNo; // Mã giao dịch ngân hàng (nếu có)

    public Payment(String paymentId, String invoiceId, double amount, Invoice.PaymentMethod method, String receivedBy, String referenceNo) {
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        setAmount(amount);
        this.method = method;
        this.paidAt = LocalDateTime.now(); // Tự động ghi lại thời gian bấm thu tiền
        this.receivedBy = receivedBy;
        this.referenceNo = referenceNo;
    }

    public void setAmount(double amount) {
        CheckingExpression.checkPositiveDouble(amount, "Số tiền thanh toán phải > 0"); 
        this.amount = amount;
    }
    
    public double getAmount() { 
        return amount; 
    }
}