package hotel.model;

import hotel.util.CheckingExpression;

public class InvoiceDetail {
    private String detailId;
    private String invoiceId;
    private Invoice.LineType lineType; 
    private String description;
    private String roomId;
    private String serviceId;
    private String usageId; 
    private double quantity;
    private double unitPrice;
    private double amount;

    public InvoiceDetail(String detailId, String invoiceId, Invoice.LineType lineType, String description, 
                         String roomId, String serviceId, String usageId, double quantity, double unitPrice) {
        this.detailId = detailId;
        this.invoiceId = invoiceId;
        this.description = description;
        this.usageId = usageId;
        setLineData(lineType, roomId, serviceId);
        setUnitPrice(unitPrice);
        setQuantity(quantity);
    }

    public void setLineData(Invoice.LineType lineType, String roomId, String serviceId) {
        CheckingExpression.checkMutuallyExclusiveIds(
            hotel.model.enums.LineType.valueOf(lineType.name()), roomId, serviceId); 
        this.lineType = lineType;
        this.roomId = roomId;
        this.serviceId = serviceId;
    }

    public void setQuantity(double quantity) {
        CheckingExpression.checkPositiveDouble(quantity, "Số lượng phải > 0"); 
        this.quantity = quantity;
        this.amount = this.quantity * this.unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        CheckingExpression.checkPositiveDouble(unitPrice, "Đơn giá phải > 0"); 
        this.unitPrice = unitPrice;
        this.amount = this.quantity * this.unitPrice; 
    }

    public double getAmount() { return amount; }
    public Invoice.LineType getLineType() { return lineType; }
}