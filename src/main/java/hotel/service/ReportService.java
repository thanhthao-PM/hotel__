package hotel.service;

import hotel.model.Invoice;
import hotel.util.CheckingExpression;

public class ReportService {
    // Bộ phận thống kê cần mượn cuốn sổ của ông Quản Lý Hóa Đơn (InvoiceManager) để có dữ liệu mà tính
    private InvoiceManager invoiceManager;

    public ReportService(InvoiceManager invoiceManager) {
        // invoiceManager null sẽ làm mọi hàm thống kê bị NullPointerException
        if (invoiceManager == null) {
            throw new IllegalArgumentException("InvoiceManager không được null");
        }

        this.invoiceManager = invoiceManager;
    }

    // Hàm tính Tổng Doanh Thu
    public double calculateTotalRevenue() {
        double total = 0;
        
        // Xin lấy toàn bộ sổ hóa đơn và số lượng hóa đơn hiện có
        Invoice[] invoices = invoiceManager.getAllInvoices();
        int count = invoiceManager.getCount();
        
        // Chạy qua từng hóa đơn để cộng tiền
        for (int i = 0; i < count; i++) {
            
            // [LOGIC]: Chỉ cộng tiền nếu hóa đơn KHÔNG bị hủy (bị hủy rồi thì đâu tính là doanh thu nữa)
            if (invoices[i].getStatus() != Invoice.InvoiceStatus.CANCELLED) {
                // Doanh thu thực tế = Tổng tiền khách Đã Trả (calculatePaid) + Tiền Cọc Đã Trừ (depositApplied)
                total += invoices[i].calculatePaid() + invoices[i].getDepositApplied();
            }
        }
        return total;
    }

    // Tính công suất trung bình 
    public void calculateAverageOccupancy(int totalRooms, int days) {
        // số phòng và số ngày không được âm (checkDivideByZero chỉ chặn đúng bằng 0)
        CheckingExpression.checkPositiveInt(totalRooms, "Tổng số phòng phải > 0");
        CheckingExpression.checkPositiveInt(days, "Số ngày phải > 0");

        CheckingExpression.checkDivideByZero(totalRooms, "Tổng số phòng không thể bằng 0"); 
        CheckingExpression.checkDivideByZero(days, "Số ngày không thể bằng 0"); 
        // Logic thống kê sẽ phối hợp với StayRecord của P4
    }
}