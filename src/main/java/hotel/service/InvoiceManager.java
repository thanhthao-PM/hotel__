package hotel.service;

import hotel.model.Invoice;
import hotel.model.RefundRecord;
import hotel.util.CheckingExpression;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class InvoiceManager {
    private Invoice[] invoices;
    private int count;
    private static final String FILE_PATH = "data/invoices.txt"; 

    public InvoiceManager(int capacity) {
        this.invoices = new Invoice[capacity];
        this.count = 0;
    }

    public void addInvoice(Invoice invoice) {
        if (count >= invoices.length) {
            Invoice[] newArray = new Invoice[invoices.length * 2];
            System.arraycopy(invoices, 0, newArray, 0, count);
            invoices = newArray;
        }
        invoices[count++] = invoice;
    }

    public Invoice findByInvoiceId(String id) {
        for (int i = 0; i < count; i++) {
            if (invoices[i].getInvoiceId().equals(id)) return invoices[i];
        }
        return null;
    }

    public void processRefundIfNeeded(Invoice invoice, String managerId) {
        double remaining = invoice.calculateRemaining();
        if (remaining < 0) {
            double refundAmount = Math.abs(remaining);
            RefundRecord refund = new RefundRecord(
                "REF" + System.currentTimeMillis(), invoice.getInvoiceId(), refundAmount, "Hoàn tiền cọc dư", managerId);
            invoice.addRefund(refund);
            System.out.println("Đã ghi nhận hoàn cọc dư: " + refundAmount);
        }
    }

    public void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FILE_PATH), StandardCharsets.UTF_8))) {
            for (int i = 0; i < count; i++) {
                Invoice inv = invoices[i];
                writer.println(inv.getInvoiceId() + "," + inv.getStayId() + "," + inv.getStatus() + "," + inv.getDepositApplied());
            }
        } catch (IOException e) {
            System.out.println("Lỗi ghi file hóa đơn: " + e.getMessage());
        }
    }
    
    public Invoice[] getAllInvoices() {
        return invoices;
    }
    
    public int getCount() { return count; }
}