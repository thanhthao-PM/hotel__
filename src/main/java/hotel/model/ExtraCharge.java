package hotel.model;

import hotel.util.CheckingExpression;

public class ExtraCharge implements Invoice.InvoiceAdjustment { 
    private String chargeId;
    private String invoiceId;
    private String reason;
    private double amount;
    private String createdBy;

    public ExtraCharge(String chargeId, String invoiceId, String reason, double amount, String createdBy) {
        this.chargeId = chargeId;
        this.invoiceId = invoiceId;
        this.reason = reason;
        setAmount(amount);
        this.createdBy = createdBy;
    }

    public void setAmount(double amount) {
        CheckingExpression.checkPositiveDouble(amount, "Phụ thu phải > 0"); 
        this.amount = amount;
    }

    @Override
    public double getAdjustmentAmount() {
        return amount; 
    }
}