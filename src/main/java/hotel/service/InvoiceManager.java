package hotel.service;

import hotel.model.Invoice;
import hotel.model.RefundRecord;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil; // IMPORT THÊM FILE CÔNG CỤ CỦA NHÓM TRƯỞNG

import java.io.IOException;

public class InvoiceManager {
    // Mảng này là cuốn sổ tay chứa TẤT CẢ hóa đơn của khách sạn.
    private Invoice[] invoices; 
    private int count; // Biến đếm xem khách sạn đang có bao nhiêu hóa đơn rồi
    
    // Đường dẫn file để lưu vào ổ cứng (chứ tắt chương trình là mất hết)
    private static final String FILE_PATH = "data/invoices.txt"; //[cite: 7]

    // Hàm tạo (Constructor) khởi tạo cuốn sổ với số trang (capacity) do mình định trước
    public InvoiceManager(int capacity) {
        // sức chứa phải > 0, nếu không new Invoice[0] rồi nhân đôi vẫn ra 0 và addInvoice bị lỗi
        CheckingExpression.checkPositiveInt(capacity, "Sức chứa danh sách hóa đơn phải > 0");
        this.invoices = new Invoice[capacity];
        this.count = 0; // Lúc mới mở hệ thống thì chưa có hóa đơn nào
    }

    // Hàm Thêm hóa đơn mới vào sổ
    public void addInvoice(Invoice invoice) {
        // không cho thêm hóa đơn null (sau này findByInvoiceId sẽ bị NullPointerException)
        if (invoice == null) {
            throw new IllegalArgumentException("Hóa đơn không được null");
        }
        // không tạo hai Invoice cho cùng một StayRecord (kế hoạch mục 5.5)
        for (int i = 0; i < count; i++) {
            if (invoices[i].getStayId().equals(invoice.getStayId())) {
                throw new IllegalArgumentException("Lượt ở " + invoice.getStayId() + " đã có hóa đơn " + invoices[i].getInvoiceId());
            }
        }

        // [THUẬT TOÁN]: Nếu sổ đầy (count >= sức chứa của mảng), ta phải mua sổ mới to gấp đôi và chép dữ liệu cũ sang.
        if (count >= invoices.length) {
            Invoice[] newArray = new Invoice[invoices.length * 2]; // Tạo mảng mới bự gấp đôi
            System.arraycopy(invoices, 0, newArray, 0, count); // Copy các tờ hóa đơn từ mảng cũ sang mảng mới
            invoices = newArray; // Dùng mảng mới làm mảng chính
        }
        
        // Cất hóa đơn vào vị trí tiếp theo và tăng biến đếm lên 1.
        invoices[count++] = invoice; 
    }

    // Hàm tìm kiếm hóa đơn theo Mã Hóa Đơn
    public Invoice findByInvoiceId(String id) {
        // mã rỗng thì trả null luôn, giống các Manager khác
        if (id == null || id.trim().isEmpty()) return null;
        // Lật từng trang sổ (vòng lặp) để tìm xem mã nào khớp với chữ mình nhập vào
        for (int i = 0; i < count; i++) {
            if (invoices[i].getInvoiceId().equals(id)) return invoices[i];
        }
        return null; // Không tìm thấy
    }

    // Xử lý logic Hoàn Cọc (Nếu cọc lớn hơn tổng hóa đơn)
    public void processRefundIfNeeded(Invoice invoice, String managerId) {
        // hóa đơn null sẽ gây NullPointerException; RefundRecord cũng bắt buộc có mã quản lý duyệt
        if (invoice == null) {
            throw new IllegalArgumentException("Hóa đơn không được null");
        }
        CheckingExpression.checkNotNullOrEmpty(managerId, "Hoàn tiền phải có mã quản lý duyệt");
        
        double remaining = invoice.calculateRemaining(); // Tính tiền nợ
        if (remaining < 0) { // Nếu nợ là số âm -> tức là Khách đưa dư tiền cọc -> Khách sạn phải trả lại
            double refundAmount = Math.abs(remaining); // Đổi số âm thành số dương bằng hàm Toán học Tuyệt đối (Math.abs)
            
            // Tạo 1 tờ giấy ghi chú hoàn tiền
            RefundRecord refund = new RefundRecord(
                "REF" + System.currentTimeMillis(), invoice.getInvoiceId(), refundAmount, "Hoàn tiền cọc dư", managerId);
            
            invoice.addRefund(refund); // Kẹp tờ giấy hoàn tiền đó vào kẹp hồ sơ của tờ hóa đơn
            System.out.println("Đã ghi nhận hoàn cọc dư: " + refundAmount);
        }
    }

    // [OOP - File I/O]: SỬ DỤNG CÔNG CỤ FileUtil CỦA NHÓM TRƯỞNG
    public void writeFile() {
        // Tạo một mảng String để chứa từng dòng text sẽ in ra file
        String[] lines = new String[count];
        for (int i = 0; i < count; i++) {
            Invoice inv = invoices[i];
            // Hàm join sẽ tự động nối các dữ liệu lại với nhau bằng dấu "|" chuẩn của nhóm
            lines[i] = FileUtil.join(inv.getInvoiceId(), inv.getStayId(), inv.getStatus(), inv.getDepositApplied());
        }
        
        try {
            // Hàm writeAllLines sẽ tự sinh ra file backup (.bak) rồi mới ghi đè file mới
            FileUtil.writeAllLines(FILE_PATH, lines, count);
        } catch (IOException e) {
            System.out.println("Lỗi ghi file hóa đơn: " + e.getMessage());
        }
    }
    
    // Getter lấy toàn bộ cuốn sổ ra ngoài để làm Thống kê
    public Invoice[] getAllInvoices() {
        return invoices;
    }
    
    public int getCount() { return count; }
}