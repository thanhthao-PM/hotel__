package hotel.model;

import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

public class InvoiceDetail {
    public static int nextNumber = 1;

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
        CheckingExpression.checkNotNullOrEmpty(detailId, "Mã dòng hóa đơn không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        CheckingExpression.checkNotNullOrEmpty(description, "Mô tả dòng hóa đơn không được để trống");

        this.detailId = detailId.trim();
        this.invoiceId = invoiceId.trim();
        this.description = description.trim();
        this.usageId = (usageId == null) ? "" : usageId.trim();

        setLineData(lineType, roomId, serviceId);
        setUnitPrice(unitPrice);
        setQuantity(quantity);

        boolean hasUsage = !this.usageId.isEmpty();
        if (lineType == Invoice.LineType.SERVICE && !hasUsage) {
            throw new IllegalArgumentException("Dòng dịch vụ phải có mã lượt dùng dịch vụ (usageId)");
        }
        if (lineType == Invoice.LineType.ROOM && hasUsage) {
            throw new IllegalArgumentException("Dòng tiền phòng không được có mã lượt dùng dịch vụ (usageId)");
        }
        updateNextNumber(this.detailId);
    }

    public static String generateId() {
        return String.format("ID%03d", nextNumber++);
    }

    public static void updateNextNumber(String id) {
        int num = FileUtil.extractNumber(id);
        if (num >= nextNumber) nextNumber = num + 1;
    }

    public void setLineData(Invoice.LineType lineType, String roomId, String serviceId) {
        if (lineType == null) {
            throw new IllegalArgumentException("Loại dòng hóa đơn không được null");
        }
        String rId = (roomId == null) ? "" : roomId.trim();
        String sId = (serviceId == null) ? "" : serviceId.trim();
        CheckingExpression.checkMutuallyExclusiveIds(
            hotel.model.enums.LineType.valueOf(lineType.name()), rId, sId);
        this.lineType = lineType;
        this.roomId = rId;
        this.serviceId = sId;
    }

    public void setQuantity(double quantity) {
        CheckingExpression.checkPositiveDouble(quantity, "Số lượng phải lớn hơn 0");
        this.quantity = quantity;
        this.amount = this.quantity * this.unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        CheckingExpression.checkPositiveDouble(unitPrice, "Đơn giá phải lớn hơn 0");
        this.unitPrice = unitPrice;
        this.amount = this.quantity * this.unitPrice;
    }

    public String getDetailId() { return detailId; }
    public String getInvoiceId() { return invoiceId; }
    public Invoice.LineType getLineType() { return lineType; }
    public String getDescription() { return description; }
    public String getRoomId() { return roomId; }
    public String getServiceId() { return serviceId; }
    public String getUsageId() { return usageId; }
    public double getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getAmount() { return amount; }

    public String toFileString() {
        return FileUtil.join(
            detailId, invoiceId, lineType.name(), FileUtil.clean(description),
            roomId, serviceId, usageId,
            Invoice.formatMoney(quantity), Invoice.formatMoney(unitPrice), Invoice.formatMoney(amount)
        );
    }

    public static InvoiceDetail fromFileString(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 10) {
            throw new IllegalArgumentException("Dòng chi tiết hóa đơn không hợp lệ: " + line);
        }
        return new InvoiceDetail(
            p[0].trim(), p[1].trim(), Invoice.LineType.valueOf(p[2].trim()),
            p[3].trim(), p[4].trim(), p[5].trim(), p[6].trim(),
            Double.parseDouble(p[7].trim()), Double.parseDouble(p[8].trim())
        );
    }
}