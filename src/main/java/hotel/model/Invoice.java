package hotel.model;

import hotel.util.CheckingExpression;
import java.time.LocalDateTime;

// [OOP - CLASS]: Đây là bản vẽ để tạo ra các đối tượng Hóa đơn.
public class Invoice {

    // ----------------------------------------------------------------------
    // 1. NESTED ENUM & INTERFACE (Khai báo lồng bên trong class để không tạo file mới)
    // ----------------------------------------------------------------------
    
    // Enum giống như một "Danh sách các lựa chọn cố định". 
    // Trạng thái hóa đơn chỉ được phép là 1 trong 4 chữ này, không được nhập bậy bạ.
    public enum InvoiceStatus { OPEN, PARTIALLY_PAID, PAID, CANCELLED } 
    
    // Phương thức thanh toán: Tiền mặt, Thẻ, Chuyển khoản
    public enum PaymentMethod { CASH, CARD, TRANSFER } 
    
    public enum DiscountType { PERCENT, FIXED } 
    public enum LineType { ROOM, SERVICE } 

    // [OOP - INTERFACE]: Interface giống như một "Bản hợp đồng".
    // Ai ký bản hợp đồng này (implements) thì BẮT BUỘC phải có hàm getAdjustmentAmount().
    // Lát nữa Giảm giá (Discount) và Phụ thu (ExtraCharge) sẽ ký hợp đồng này.
    public interface InvoiceAdjustment { 
        double getAdjustmentAmount(); 
    } 


    // ----------------------------------------------------------------------
    // 2. THUỘC TÍNH (ATTRIBUTES) - Những thông tin mà Hóa đơn lưu giữ
    // ----------------------------------------------------------------------
    
    // [OOP - STATIC]: Biến static là biến "dùng chung" cho toàn bộ các hóa đơn.
    // Khi tạo hóa đơn thứ 1, nó lấy số 1 rồi tăng lên 2. Hóa đơn thứ 2 sẽ lấy số 2.
    public static int nextNumber = 1; 
    
    // [OOP - ĐÓNG GÓI (Encapsulation)]: Các biến dùng chữ 'private' (riêng tư).
    // Giống như việc cất tiền vào két sắt. Người ngoài không thể tự do sửa mã hóa đơn được.
    private String invoiceId;
    private String stayId;
    private LocalDateTime issuedAt; // Ngày giờ xuất hóa đơn
    private String issuedBy;
    private InvoiceStatus status;
    private double depositApplied;  // Tiền cọc đã áp dụng

    // [OOP - MẢNG ĐỐI TƯỢNG (Array)]: Một tờ hóa đơn thực tế có nhiều dòng ghi tiền phòng, dịch vụ.
    // Mảng (Array) giống như một cái kệ có 20 ngăn, mỗi ngăn chứa 1 dòng chi tiết hóa đơn (InvoiceDetail).
    private InvoiceDetail[] details = new InvoiceDetail[20];
    private int detailCount = 0; // Biến này đếm xem cái kệ đã để được bao nhiêu dòng rồi.

    private Discount[] discounts = new Discount[5];
    private int discountCount = 0;

    private ExtraCharge[] extraCharges = new ExtraCharge[5];
    private int extraChargeCount = 0;

    private Payment[] payments = new Payment[10];
    private int paymentCount = 0;

    private RefundRecord[] refunds = new RefundRecord[5];
    private int refundCount = 0;


    // ----------------------------------------------------------------------
    // 3. CONSTRUCTOR (HÀM TẠO) - Hàm chạy đầu tiên khi "đúc" ra đối tượng
    // ----------------------------------------------------------------------
    // Khi bạn gõ từ khóa 'new Invoice(...)', hàm này sẽ chạy để thiết lập thông tin ban đầu.
    public Invoice(String stayId, String issuedBy, double depositApplied) {
        // Trước khi tạo, ta gọi người bảo vệ (CheckingExpression) ra kiểm tra dữ liệu xem có rỗng không.
        CheckingExpression.checkNotNullOrEmpty(stayId, "Mã lưu trú không được trống"); 
        //phải biết nhân viên nào lập hóa đơn (đặt trước dòng tăng nextNumber để không nhảy mã khi lỗi)
        CheckingExpression.checkNotNullOrEmpty(issuedBy, "Mã nhân viên lập hóa đơn không được trống");
        
        this.invoiceId = "INV" + (nextNumber++); // Tự động tạo mã: INV1, INV2...
        this.stayId = stayId;
        this.issuedAt = LocalDateTime.now(); // Lấy giờ hiện tại của máy tính
        this.issuedBy = issuedBy;
        this.status = InvoiceStatus.OPEN; // Hóa đơn mới tạo mặc định trạng thái là OPEN (Đang mở)
        
        // Thay vì gán thẳng (this.depositApplied = depositApplied), 
        // ta gọi hàm setter bên dưới để nó kiểm tra tiền cọc có bị âm hay không.
        setDepositApplied(depositApplied); 
    }


    // ----------------------------------------------------------------------
    // 4. GETTER & SETTER - Các "Cửa ngõ" để giao tiếp với Két sắt (private)
    // ----------------------------------------------------------------------
    // Hàm 'set' để thay đổi giá trị (có kiểm tra an toàn)
    public void setDepositApplied(double depositApplied) {
        // Bảo vệ chặn không cho nhập tiền âm. Nếu nhập âm, nó sẽ báo lỗi và dừng chương trình ngay.
        CheckingExpression.checkNonNegativeDouble(depositApplied, "Tiền cọc áp dụng không được âm"); 
        this.depositApplied = depositApplied;
    }

    // Hàm 'get' để lấy giá trị ra xem (nhưng không được sửa)
    public String getInvoiceId() { return invoiceId; }
    public String getStayId() { return stayId; }
    public InvoiceStatus getStatus() { return status; }
    public double getDepositApplied() { return depositApplied; }


    // ----------------------------------------------------------------------
    // 5. CÁC HÀM XỬ LÝ NGHIỆP VỤ & ĐA HÌNH (POLYMORPHISM)
    // ----------------------------------------------------------------------
    
    // Hàm tính TỔNG TIỀN hóa đơn
    public double calculateTotal() {
        double total = 0;
        
        // 1. Dùng vòng lặp (for) chạy qua từng ngăn trên kệ details để cộng tiền lại. (Lưu ý: Mảng bắt đầu từ 0)
        for (int i = 0; i < detailCount; i++) {
            total += details[i].getAmount();
        }
        
        // 2. [OOP - ĐA HÌNH]: Đây là điểm ăn tiền của đồ án!
        // ExtraCharge (Phụ thu) và Discount (Giảm giá) đều ký bản hợp đồng InvoiceAdjustment.
        // Ta chỉ việc gọi hàm getAdjustmentAmount().
        // Thằng Phụ thu đã viết sẵn bên trong nó là trả về SỐ DƯƠNG (cộng thêm tiền).
        for (int i = 0; i < extraChargeCount; i++) {
            total += extraCharges[i].getAdjustmentAmount(); 
        }
        
        // Thằng Giảm giá đã viết sẵn bên trong nó là trả về SỐ ÂM (trừ bớt tiền).
        for (int i = 0; i < discountCount; i++) {
            total += discounts[i].getAdjustmentAmount(); 
        }
        return total;
    }

    // Hàm tính tổng số tiền khách ĐÃ TRẢ (khách có thể trả làm nhiều lần)
    public double calculatePaid() {
        double paid = 0;
        for (int i = 0; i < paymentCount; i++) {
            paid += payments[i].getAmount();
        }
        return paid;
    }

    // Hàm tính số tiền khách CÒN NỢ (Phải thu)
    public double calculateRemaining() {
        // Tiền còn nợ = Tổng tiền - Tiền cọc (đã đưa trước đó) - Tiền đã thanh toán
        return calculateTotal() - depositApplied - calculatePaid(); 
    }

    // Hàm thêm một Lần thanh toán vào mảng
    public void addPayment(Payment payment) {
        // không cho thêm phiếu thanh toán null
        checkNotNullObject(payment, "Phiếu thanh toán không được null");
        // Kiểm tra xem kệ (mảng) còn chỗ trống không
        if (paymentCount < payments.length) {
            payments[paymentCount++] = payment; // Đặt phiếu thanh toán vào ngăn trống và tăng biến đếm
            
            // Logic cập nhật trạng thái: Nếu nợ <= 0 thì đã trả xong (PAID), ngược lại là mới trả một phần (PARTIALLY_PAID)
            if (calculateRemaining() <= 0) {
                this.status = InvoiceStatus.PAID;
            } else {
                this.status = InvoiceStatus.PARTIALLY_PAID;
            }
        }
    }

    // Các hàm thêm khác vào mảng (tương tự addPayment)
    public void addDetail(InvoiceDetail detail) {
        checkNotNullObject(detail, "Dòng chi tiết hóa đơn không được null");
        if (detailCount < details.length) details[detailCount++] = detail;
    }
    public void addDiscount(Discount discount) {
        checkNotNullObject(discount, "Giảm giá không được null");
        if (discountCount < discounts.length) discounts[discountCount++] = discount;
    }
    public void addExtraCharge(ExtraCharge charge) {
        checkNotNullObject(charge, "Phụ thu không được null");
        if (extraChargeCount < extraCharges.length) extraCharges[extraChargeCount++] = charge;
    }
    public void addRefund(RefundRecord refund) {
        checkNotNullObject(refund, "Phiếu hoàn tiền không được null");
        if (refundCount < refunds.length) refunds[refundCount++] = refund;
    }
    // kiểm tra đối tượng khác null (CheckingExpression chưa có hàm này)
    private static void checkNotNullObject(Object obj, String errorMessage) {
        if (obj == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    // Hàm Hủy hóa đơn
    public void cancel(String reason) {
        CheckingExpression.checkNotNullOrEmpty(reason, "Phải có lý do hủy hóa đơn"); 
        this.status = InvoiceStatus.CANCELLED;
    }
}