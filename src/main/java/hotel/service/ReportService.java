package hotel.service;

import hotel.model.Invoice;
import hotel.util.CheckingExpression;

public class ReportService {
    private InvoiceManager invoiceManager;

    public ReportService(InvoiceManager invoiceManager) {
        this.invoiceManager = invoiceManager;
    }

    public double calculateTotalRevenue() {
        double total = 0;
        Invoice[] invoices = invoiceManager.getAllInvoices();
        int count = invoiceManager.getCount();
        
        for (int i = 0; i < count; i++) {
            if (invoices[i].getStatus() != Invoice.InvoiceStatus.CANCELLED) {
                total += invoices[i].calculatePaid() + invoices[i].getDepositApplied();
            }
        }
        return total;
    }
    public void calculateAverageOccupancy(int totalRooms, int days) {
        CheckingExpression.checkDivideByZero(totalRooms, "Tổng số phòng không thể bằng 0"); 
        CheckingExpression.checkDivideByZero(days, "Số ngày không thể bằng 0"); 
    }
}