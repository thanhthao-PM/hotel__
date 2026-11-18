package hotel.model;

import hotel.util.CheckingExpression;
import hotel.util.FileUtil;
import java.time.LocalDateTime;

public class Payment {
    public static int nextNumber = 1;

    private String paymentId;
    private String invoiceId;
    private double amount;
    private Invoice.PaymentMethod method;
    private LocalDateTime paidAt;
    private String receivedBy;
    private String referenceNo;

    public Payment(String paymentId, String invoiceId, double amount,
                   Invoice.PaymentMethod method, String receivedBy, String referenceNo) {
        this(paymentId, invoiceId, amount, method, LocalDateTime.now(), receivedBy, referenceNo);
    }

    public Payment(String paymentId, String invoiceId, double amount,
                   Invoice.PaymentMethod method, LocalDateTime paidAt, String receivedBy, String referenceNo) {
        CheckingExpression.checkNotNullOrEmpty(paymentId, "Mã thanh toán không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        if (method == null) {
            throw new IllegalArgumentException("Phương thức thanh toán không được null");
        }
        CheckingExpression.checkNotNullOrEmpty(receivedBy, "Mã nhân viên thu tiền không được để trống");

        this.paymentId = paymentId.trim();
        this.invoiceId = invoiceId.trim();
        setAmount(amount);
        this.method = method;
        this.paidAt = (paidAt != null) ? paidAt : LocalDateTime.now();
        this.receivedBy = receivedBy.trim();
        this.referenceNo = (referenceNo == null) ? "" : referenceNo.trim();
        updateNextNumber(this.paymentId);
    }

    public static String generateId() {
        return String.format("PM%03d", nextNumber++);
    }

    public static void updateNextNumber(String id) {
        int num = FileUtil.extractNumber(id);
        if (num >= nextNumber) nextNumber = num + 1;
    }

    public void setAmount(double amount) {
        CheckingExpression.checkPositiveDouble(amount, "Số tiền thanh toán phải > 0");
        this.amount = amount;
    }

    public String getPaymentId() { return paymentId; }
    public String getInvoiceId() { return invoiceId; }
    public double getAmount() { return amount; }
    public Invoice.PaymentMethod getMethod() { return method; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getReceivedBy() { return receivedBy; }
    public String getReferenceNo() { return referenceNo; }

    public String toFileString() {
        return FileUtil.join(
            paymentId, invoiceId, Invoice.formatMoney(amount), method.name(),
            paidAt.format(Invoice.FILE_DT_FORMAT), receivedBy, FileUtil.clean(referenceNo)
        );
    }

    public static Payment fromFileString(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 7) {
            throw new IllegalArgumentException("Dòng thanh toán không hợp lệ: " + line);
        }
        return new Payment(
            p[0].trim(), p[1].trim(), Double.parseDouble(p[2].trim()),
            Invoice.PaymentMethod.valueOf(p[3].trim()), LocalDateTime.parse(p[4].trim()),
            p[5].trim(), p[6].trim()
        );
    }

    public String getPaymentId() { return paymentId; }
    public String getInvoiceId() { return invoiceId; }
    public Invoice.PaymentMethod getMethod() { return method; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getReceivedBy() { return receivedBy; }
    public String getReferenceNo() { return referenceNo; }
}