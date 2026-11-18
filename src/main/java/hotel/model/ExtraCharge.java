package hotel.model;

import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

public class ExtraCharge implements Invoice.InvoiceAdjustment {
    public static int nextNumber = 1;

    private String chargeId;
    private String invoiceId;
    private String reason;
    private double amount;
    private String createdBy;

    public ExtraCharge(String chargeId, String invoiceId, String reason, double amount, String createdBy) {
        CheckingExpression.checkNotNullOrEmpty(chargeId, "Mã phụ thu không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do phụ thu không được để trống");
        CheckingExpression.checkNotNullOrEmpty(createdBy, "Mã nhân viên lập phụ thu không được để trống");

        this.chargeId = chargeId.trim();
        this.invoiceId = invoiceId.trim();
        this.reason = reason.trim();
        setAmount(amount);
        this.createdBy = createdBy.trim();
        updateNextNumber(this.chargeId);
    }

    public static String generateId() {
        return String.format("EC%03d", nextNumber++);
    }

    public static void updateNextNumber(String id) {
        int num = FileUtil.extractNumber(id);
        if (num >= nextNumber) nextNumber = num + 1;
    }

    public void setAmount(double amount) {
        CheckingExpression.checkPositiveDouble(amount, "Phụ thu phải lớn hơn 0");
        this.amount = amount;
    }

    public String getChargeId() { return chargeId; }
    public String getInvoiceId() { return invoiceId; }
    public String getReason() { return reason; }
    public double getAmount() { return amount; }
    public String getCreatedBy() { return createdBy; }

    @Override
    public double getAdjustmentAmount() {
        return amount;
    }

    public String toFileString() {
        return FileUtil.join(chargeId, invoiceId, FileUtil.clean(reason), Invoice.formatMoney(amount), createdBy);
    }

    public static ExtraCharge fromFileString(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 5) {
            throw new IllegalArgumentException("Dòng phụ thu không hợp lệ: " + line);
        }
        return new ExtraCharge(p[0].trim(), p[1].trim(), p[2].trim(), Double.parseDouble(p[3].trim()), p[4].trim());
    }

    public String getChargeId() { return chargeId; }
    public String getInvoiceId() { return invoiceId; }
    public String getReason() { return reason; }
    public double getAmount() { return amount; }
    public String getCreatedBy() { return createdBy; }
}