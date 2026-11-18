package hotel.model;

import hotel.util.CheckingExpression;

// Lệnh 'implements' báo rằng: Class Discount cam kết sẽ code cái hàm có trong giao diện InvoiceAdjustment
public class Discount implements Invoice.InvoiceAdjustment { 
    private String discountId;
    private String invoiceId;
    private Invoice.DiscountType discountType;
    private double value; // Giá trị giảm (VD: 100.000)
    private String reason;
    private String approvedBy; // Phải có chữ ký quản lý mới được giảm

    // Constructor
    public Discount(String discountId, String invoiceId, Invoice.DiscountType type, double value, String reason, String approvedBy) {
        //kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(discountId, "Mã giảm giá không được để trống");
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được để trống");
        if (type == null) {
            throw new IllegalArgumentException("Loại giảm giá không được null");
        }
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do giảm giá không được để trống");
        CheckingExpression.checkNotNullOrEmpty(approvedBy, "Giảm giá phải có mã quản lý duyệt");


        this.discountId = discountId;
        this.invoiceId = invoiceId;
        this.discountType = type;
        setValue(value);
        this.reason = reason;
        this.approvedBy = approvedBy;
    }

    public void setValue(double value) {
        CheckingExpression.checkNonNegativeDouble(value, "Giảm giá không được là số âm"); 
        // giảm theo phần trăm không được vượt 100%
        if (discountType == Invoice.DiscountType.PERCENT && value > 100) {
            throw new IllegalArgumentException("Giảm giá theo phần trăm không được vượt quá 100%");
        }

        this.value = value;
    }

    // [OOP - ĐA HÌNH (@Override)]: Đây là lúc ta thực hiện lời hứa với cái hợp đồng (interface)
    // Đặc trưng của Giảm giá là trừ tiền đi, nên ta đặt dấu trừ (-) trước chữ value.
    @Override
    public double getAdjustmentAmount() {
        return -value; // Hàm Invoice.calculateTotal() khi gọi hàm này sẽ nhận được số âm để cộng vào (tức là làm giảm tổng tiền).
    }

    public String getDiscountId() { return discountId; }
    public String getInvoiceId() { return invoiceId; }
    public Invoice.DiscountType getDiscountType() { return discountType; }
    public double getValue() { return value; }
    public String getReason() { return reason; }
    public String getApprovedBy() { return approvedBy; }
}