package hotel.model;

import hotel.util.CheckingExpression;
import java.time.LocalDateTime;

// Class Ghi nhận Hoàn tiền (Ví dụ khách đặt cọc 1 triệu, nhưng tiền phòng có 800k, phải hoàn lại 200k)
public class RefundRecord {
    private String refundId;
    private String invoiceId;
    private double amount;
    private String reason;
    private LocalDateTime refundedAt;
    private String approvedBy;

    public RefundRecord(String refundId, String invoiceId, double amount, String reason, String approvedBy) {
        //kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(refundId, "Mã hoàn tiền không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do hoàn tiền không được để trống");
        CheckingExpression.checkNotNullOrEmpty(approvedBy, "Hoàn tiền phải có mã quản lý duyệt");

        this.refundId = refundId;
        this.invoiceId = invoiceId;
        setAmount(amount);
        this.reason = reason;
        this.refundedAt = LocalDateTime.now();
        this.approvedBy = approvedBy;
    }

    public void setAmount(double amount) {
        CheckingExpression.checkPositiveDouble(amount, "Tiền hoàn phải > 0"); 
        this.amount = amount;
    }
    
    public double getAmount() { 
        return amount; 
    }

    public String getRefundId() { return refundId; }
    public String getInvoiceId() { return invoiceId; }
    public String getReason() { return reason; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public String getApprovedBy() { return approvedBy; }
}