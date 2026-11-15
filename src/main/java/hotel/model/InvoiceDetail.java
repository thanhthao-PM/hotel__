package hotel.model;

import hotel.util.CheckingExpression;

public class InvoiceDetail {
    // Thuộc tính private đóng gói an toàn
    private String detailId;
    private String invoiceId; // Dòng này thuộc về tờ hóa đơn nào?
    private Invoice.LineType lineType; // Loại dòng: Tiền phòng (ROOM) hay Dịch vụ (SERVICE)
    private String description; // Mô tả: "Phòng VIP 101", "Mát xa 60p"...
    private String roomId;
    private String serviceId;
    private String usageId; 
    private double quantity;  // Số lượng (VD: 3 đêm, 2 đĩa cơm)
    private double unitPrice; // Đơn giá (VD: 500k/đêm, 50k/đĩa)
    private double amount;    // Thành tiền = Số lượng * Đơn giá

    // Constructor
    public InvoiceDetail(String detailId, String invoiceId, Invoice.LineType lineType, String description, 
                         String roomId, String serviceId, String usageId, double quantity, double unitPrice) {
        // kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(detailId, "Mã dòng hóa đơn không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        CheckingExpression.checkNotNullOrEmpty(description, "Mô tả dòng hóa đơn không được để trống");

        this.detailId = detailId;
        this.invoiceId = invoiceId;
        this.description = description;
        this.usageId = usageId;
        
        // Gọi setter thay vì gán trực tiếp để tận dụng bộ kiểm tra lỗi
        setLineData(lineType, roomId, serviceId);
        setUnitPrice(unitPrice);
        setQuantity(quantity);

        //dòng dịch vụ phải có usageId, dòng tiền phòng thì usageId phải rỗng (theo kế hoạch)
        boolean hasUsage = usageId != null && !usageId.trim().isEmpty();
        if (lineType == Invoice.LineType.SERVICE && !hasUsage) {
            throw new IllegalArgumentException("Dòng dịch vụ phải có mã lượt dùng dịch vụ (usageId)");
        }
        if (lineType == Invoice.LineType.ROOM && hasUsage) {
            throw new IllegalArgumentException("Dòng tiền phòng không được có mã lượt dùng dịch vụ (usageId)");
        }
    }

    // Hàm gán loại dòng và kiểm tra logic chéo
    public void setLineData(Invoice.LineType lineType, String roomId, String serviceId) {
        // chặn null trước khi gọi lineType.name(), tránh NullPointerException
        if (lineType == null) {
            throw new IllegalArgumentException("Loại dòng hóa đơn không được null");
        }
        // [LOGIC NGHIỆP VỤ]: Nếu là dòng tính tiền phòng thì chỉ được có mã Phòng (roomId), không được có mã Dịch vụ. Và ngược lại.
        // Hàm này gọi file kiểm tra của nhóm để bắt lỗi nếu truyền sai.
        CheckingExpression.checkMutuallyExclusiveIds(
            hotel.model.enums.LineType.valueOf(lineType.name()), roomId, serviceId); 
        this.lineType = lineType;
        this.roomId = roomId;
        this.serviceId = serviceId;
    }

    public void setQuantity(double quantity) {
        CheckingExpression.checkPositiveDouble(quantity, "Số lượng phải lớn hơn 0"); 
        this.quantity = quantity;
        // Mỗi khi đổi số lượng, phải tính lại Thành tiền = Số lượng * Đơn giá
        this.amount = this.quantity * this.unitPrice; 
    }

    public void setUnitPrice(double unitPrice) {
        CheckingExpression.checkPositiveDouble(unitPrice, "Đơn giá phải lớn hơn 0"); 
        this.unitPrice = unitPrice;
        // Mỗi khi đổi đơn giá, cũng phải tính lại Thành tiền
        this.amount = this.quantity * this.unitPrice; 
    }

    // Getter lấy thành tiền cho hóa đơn mẹ (Invoice) tính tổng
    public double getAmount() { return amount; }
    
    // Getter lấy loại dòng (được giữ lại từ nhánh cũ để đảm bảo không lỗi)
    public Invoice.LineType getLineType() { return lineType; }
}