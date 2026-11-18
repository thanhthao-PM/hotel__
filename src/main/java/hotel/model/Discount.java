package hotel.model;

import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

public class Discount implements Invoice.InvoiceAdjustment {
    public static int nextNumber = 1;

    private String discountId;
    private String invoiceId;
    private Invoice.DiscountType discountType;
    private double value;
    private String reason;
    private String approvedBy;

    public Discount(String discountId, String invoiceId, Invoice.DiscountType type,
                    double value, String reason, String approvedBy) {
        CheckingExpression.checkNotNullOrEmpty(discountId, "Mã giảm giá không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        if (type == null) {
            throw new IllegalArgumentException("Loại giảm giá không được null");
        }
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do giảm giá không được để trống");
        CheckingExpression.checkNotNullOrEmpty(approvedBy, "Giảm giá phải có mã quản lý duyệt");

        this.discountId = discountId.trim();
        this.invoiceId = invoiceId.trim();
        this.discountType = type;
        setValue(value);
        this.reason = reason.trim();
        this.approvedBy = approvedBy.trim();
        updateNextNumber(this.discountId);
    }

    public static String generateId() {
        return String.format("DS%03d", nextNumber++);
    }

    public static void updateNextNumber(String id) {
        int num = FileUtil.extractNumber(id);
        if (num >= nextNumber) nextNumber = num + 1;
    }

    public void setValue(double value) {
        CheckingExpression.checkNonNegativeDouble(value, "Giảm giá không được là số âm");
        if (discountType == Invoice.DiscountType.PERCENT && value > 100) {
            throw new IllegalArgumentException("Giảm giá theo phần trăm không được vượt quá 100%");
        }
        this.value = value;
    }

    public String getDiscountId() { return discountId; }
    public String getInvoiceId() { return invoiceId; }
    public Invoice.DiscountType getDiscountType() { return discountType; }
    public double getValue() { return value; }
    public String getReason() { return reason; }
    public String getApprovedBy() { return approvedBy; }

    @Override
    public double getAdjustmentAmount() {
        return -value;
    }

    @Override
    public double getAdjustmentAmount(double baseAmount) {
        if (discountType == Invoice.DiscountType.PERCENT) {
            return -(baseAmount * value / 100.0);
        }
        return -value;
    }

    public String toFileString() {
        return FileUtil.join(discountId, invoiceId, discountType.name(), Invoice.formatMoney(value), FileUtil.clean(reason), approvedBy);
    }

    public static Discount fromFileString(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 6) {
            throw new IllegalArgumentException("Dòng giảm giá không hợp lệ: " + line);
        }
        return new Discount(
            p[0].trim(), p[1].trim(), Invoice.DiscountType.valueOf(p[2].trim()),
            Double.parseDouble(p[3].trim()), p[4].trim(), p[5].trim()
        );
    }

    public String getDiscountId() { return discountId; }
    public String getInvoiceId() { return invoiceId; }
    public Invoice.DiscountType getDiscountType() { return discountType; }
    public double getValue() { return value; }
    public String getReason() { return reason; }
    public String getApprovedBy() { return approvedBy; }
}