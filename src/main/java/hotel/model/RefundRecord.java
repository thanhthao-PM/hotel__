package hotel.model;

import hotel.util.CheckingExpression;
import hotel.util.FileUtil;
import java.time.LocalDateTime;

public class RefundRecord {
    public static int nextNumber = 1;

    private String refundId;
    private String invoiceId;
    private double amount;
    private String reason;
    private LocalDateTime refundedAt;
    private String approvedBy;

    public RefundRecord(String refundId, String invoiceId, double amount, String reason, String approvedBy) {
        this(refundId, invoiceId, amount, reason, LocalDateTime.now(), approvedBy);
    }

    public RefundRecord(String refundId, String invoiceId, double amount,
                        String reason, LocalDateTime refundedAt, String approvedBy) {
        CheckingExpression.checkNotNullOrEmpty(refundId, "Mã hoàn tiền không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do hoàn tiền không được để trống");
        CheckingExpression.checkNotNullOrEmpty(approvedBy, "Hoàn tiền phải có mã quản lý duyệt");

        this.refundId = refundId.trim();
        this.invoiceId = invoiceId.trim();
        setAmount(amount);
        this.reason = reason.trim();
        this.refundedAt = (refundedAt != null) ? refundedAt : LocalDateTime.now();
        this.approvedBy = approvedBy.trim();
        updateNextNumber(this.refundId);
    }

    public static String generateId() {
        return String.format("RF%03d", nextNumber++);
    }

    public static void updateNextNumber(String id) {
        int num = FileUtil.extractNumber(id);
        if (num >= nextNumber) nextNumber = num + 1;
    }

    public void setAmount(double amount) {
        CheckingExpression.checkPositiveDouble(amount, "Tiền hoàn phải > 0");
        this.amount = amount;
    }

    public String getRefundId() { return refundId; }
    public String getInvoiceId() { return invoiceId; }
    public double getAmount() { return amount; }
    public String getReason() { return reason; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public String getApprovedBy() { return approvedBy; }

    public String toFileString() {
        return FileUtil.join(
            refundId, invoiceId, Invoice.formatMoney(amount),
            FileUtil.clean(reason), refundedAt.format(Invoice.FILE_DT_FORMAT), approvedBy
        );
    }

    public static RefundRecord fromFileString(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 6) {
            throw new IllegalArgumentException("Dòng hoàn tiền không hợp lệ: " + line);
        }
        return new RefundRecord(
            p[0].trim(), p[1].trim(), Double.parseDouble(p[2].trim()),
            p[3].trim(), LocalDateTime.parse(p[4].trim()), p[5].trim()
        );
    }

    public String getRefundId() { return refundId; }
    public String getInvoiceId() { return invoiceId; }
    public String getReason() { return reason; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public String getApprovedBy() { return approvedBy; }
}