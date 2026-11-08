package hotel.model;

import hotel.util.CheckingExpression;

public class Discount implements Invoice.InvoiceAdjustment { 
    private String discountId;
    private String invoiceId;
    private Invoice.DiscountType discountType;
    private double value;
    private String reason;
    private String approvedBy;

    public Discount(String discountId, String invoiceId, Invoice.DiscountType type, double value, String reason, String approvedBy) {
        this.discountId = discountId;
        this.invoiceId = invoiceId;
        this.discountType = type;
        setValue(value);
        this.reason = reason;
        this.approvedBy = approvedBy;
    }

    public void setValue(double value) {
        CheckingExpression.checkNonNegativeDouble(value, "Giảm giá không được âm"); 
        this.value = value;
    }

    @Override
    public double getAdjustmentAmount() {
        return -value; 
    }
}