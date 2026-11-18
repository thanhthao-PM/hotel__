package hotel.model;

import hotel.util.CheckingExpression;

public class ExtraCharge implements Invoice.InvoiceAdjustment { 
    private String chargeId;
    private String invoiceId;
    private String reason;
    private double amount;
    private String createdBy;

    // Constructor
    public ExtraCharge(String chargeId, String invoiceId, String reason, double amount, String createdBy) {
        //kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(chargeId, "Mã phụ thu không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do phụ thu không được để trống");
        CheckingExpression.checkNotNullOrEmpty(createdBy, "Mã nhân viên lập phụ thu không được để trống");

        
        this.chargeId = chargeId;
        this.invoiceId = invoiceId;
        this.reason = reason;
        setAmount(amount);
        this.createdBy = createdBy;
    }

    public void setAmount(double amount) {
        CheckingExpression.checkPositiveDouble(amount, "Phụ thu phải lớn hơn 0"); 
        this.amount = amount;
    }

    // [OOP - ĐA HÌNH]: Phụ thu là lấy thêm tiền của khách, nên ta trả về số DƯƠNG.
    @Override
    public double getAdjustmentAmount() {
        return amount; 
    }

    public String getChargeId() { return chargeId; }
    public String getInvoiceId() { return invoiceId; }
    public String getReason() { return reason; }
    public double getAmount() { return amount; }
    public String getCreatedBy() { return createdBy; }
}