package hotel.service;

import hotel.model.Invoice;
import hotel.model.InvoiceDetail;
import hotel.util.CheckingExpression;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReportService {
    private InvoiceManager invoiceManager;

    public ReportService(InvoiceManager invoiceManager) {
        if (invoiceManager == null) {
            throw new IllegalArgumentException("InvoiceManager không được null");
        }
        this.invoiceManager = invoiceManager;
    }

    // 1. Tổng doanh thu thực thu toàn hệ thống (không tính hóa đơn đã hủy)
    public double calculateTotalRevenue() {
        double total = 0;
        Invoice[] invoices = invoiceManager.getAllInvoices();
        int count = invoiceManager.getCount();

        for (int i = 0; i < count; i++) {
            if (invoices[i].getStatus() != Invoice.InvoiceStatus.CANCELLED) {
                total += invoices[i].calculatePaid() + invoices[i].getDepositApplied() - invoices[i].calculateRefunded();
            }
        }
        return total;
    }

    // 2. Doanh thu theo kỳ (từ ngày from đến ngày to, bao gồm cả 2 đầu)
    public double calculateRevenueByPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được null");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu");
        }

        double total = 0;
        Invoice[] invoices = invoiceManager.getAllInvoices();
        int count = invoiceManager.getCount();

        for (int i = 0; i < count; i++) {
            Invoice inv = invoices[i];
            if (inv.getStatus() == Invoice.InvoiceStatus.CANCELLED) continue;
            LocalDate d = inv.getIssuedAt().toLocalDate();
            if (!d.isBefore(from) && !d.isAfter(to)) {
                total += inv.calculatePaid() + inv.getDepositApplied() - inv.calculateRefunded();
            }
        }
        return total;
    }

    // 3. Công suất phòng (%) trong khoảng ngày dựa trên số đêm phòng đã bán trong hóa đơn
    public double calculateAverageOccupancy(int totalRooms, LocalDate from, LocalDate to) {
        CheckingExpression.checkPositiveInt(totalRooms, "Tổng số phòng phải > 0");
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        CheckingExpression.checkDivideByZero(totalRooms * days, "Mẫu số công suất phòng không thể bằng 0");

        double soldRoomNights = 0;
        Invoice[] invoices = invoiceManager.getAllInvoices();
        int count = invoiceManager.getCount();

        for (int i = 0; i < count; i++) {
            Invoice inv = invoices[i];
            if (inv.getStatus() == Invoice.InvoiceStatus.CANCELLED) continue;
            LocalDate d = inv.getIssuedAt().toLocalDate();
            if (d.isBefore(from) || d.isAfter(to)) continue;

            for (int j = 0; j < inv.getDetailCount(); j++) {
                InvoiceDetail detail = inv.getDetails()[j];
                if (detail.getLineType() == Invoice.LineType.ROOM) {
                    soldRoomNights += detail.getQuantity();
                }
            }
        }
        return (soldRoomNights / (totalRooms * days)) * 100.0;
    }

    // 4. Thống kê dịch vụ bán chạy (dùng mảng thuần OOP, sắp xếp giảm dần theo doanh thu)
    public void printTopServicesReport() {
        String[] serviceIds = new String[50];
        String[] serviceNames = new String[50];
        double[] totalQty = new double[50];
        double[] totalRev = new double[50];
        int svcCount = 0;

        Invoice[] invoices = invoiceManager.getAllInvoices();
        int count = invoiceManager.getCount();

        for (int i = 0; i < count; i++) {
            Invoice inv = invoices[i];
            if (inv.getStatus() == Invoice.InvoiceStatus.CANCELLED) continue;

            for (int j = 0; j < inv.getDetailCount(); j++) {
                InvoiceDetail d = inv.getDetails()[j];
                if (d.getLineType() != Invoice.LineType.SERVICE) continue;

                int idx = -1;
                for (int k = 0; k < svcCount; k++) {
                    if (serviceIds[k].equals(d.getServiceId())) {
                        idx = k;
                        break;
                    }
                }
                if (idx == -1 && svcCount < serviceIds.length) {
                    idx = svcCount++;
                    serviceIds[idx] = d.getServiceId();
                    serviceNames[idx] = d.getDescription();
                }
                if (idx != -1) {
                    totalQty[idx] += d.getQuantity();
                    totalRev[idx] += d.getAmount();
                }
            }
        }

        if (svcCount == 0) {
            System.out.println("=> Chưa có dữ liệu dịch vụ nào trong hóa đơn.");
            return;
        }

        // Sắp xếp giảm dần theo doanh thu
        for (int i = 0; i < svcCount - 1; i++) {
            for (int j = i + 1; j < svcCount; j++) {
                if (totalRev[j] > totalRev[i]) {
                    String tmpId = serviceIds[i]; serviceIds[i] = serviceIds[j]; serviceIds[j] = tmpId;
                    String tmpName = serviceNames[i]; serviceNames[i] = serviceNames[j]; serviceNames[j] = tmpName;
                    double tmpQ = totalQty[i]; totalQty[i] = totalQty[j]; totalQty[j] = tmpQ;
                    double tmpR = totalRev[i]; totalRev[i] = totalRev[j]; totalRev[j] = tmpR;
                }
            }
        }

        System.out.println("--- TOP DỊCH VỤ BÁN CHẠY ---");
        System.out.printf("%-6s | %-25s | %-8s | %s%n", "Mã DV", "Tên dịch vụ", "Số lượng", "Doanh thu (VND)");
        System.out.println("----------------------------------------------------------------");
        for (int i = 0; i < svcCount; i++) {
            System.out.printf("%-6s | %-25s | %-8s | %s%n",
                serviceIds[i], serviceNames[i], Invoice.formatMoney(totalQty[i]), Invoice.formatMoney(totalRev[i]));
        }
    }
}