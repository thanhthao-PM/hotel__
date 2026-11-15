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
        this.discountId = discountId;
        this.invoiceId = invoiceId;
        this.discountType = type;
        setValue(value);
        this.reason = reason;
        this.approvedBy = approvedBy;
    }

    public void setValue(double value) {
        CheckingExpression.checkNonNegativeDouble(value, "Giảm giá không được là số âm"); 
        this.value = value;
    }

    // [OOP - ĐA HÌNH (@Override)]: Đây là lúc ta thực hiện lời hứa với cái hợp đồng (interface)
    // Đặc trưng của Giảm giá là trừ tiền đi, nên ta đặt dấu trừ (-) trước chữ value.
    @Override
    public double getAdjustmentAmount() {
        return -value; // Hàm Invoice.calculateTotal() khi gọi hàm này sẽ nhận được số âm để cộng vào (tức là làm giảm tổng tiền).
    }
}