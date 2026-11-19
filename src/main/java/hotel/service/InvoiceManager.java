package hotel.service;

import hotel.model.CheckOutReceipt;
import hotel.model.Discount;
import hotel.model.ExtraCharge;
import hotel.model.Invoice;
import hotel.model.InvoiceDetail;
import hotel.model.Payment;
import hotel.model.RefundRecord;
import hotel.model.ServiceUsage;
import hotel.model.StayRecord;
import hotel.storage.IFileHandler;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;

public class InvoiceManager implements IFileHandler {

    private static final double LATE_CHECKOUT_FEE_PER_HOUR = 50000;

    private Invoice[] invoices;
    private int count;

    public InvoiceManager(int capacity) {
        CheckingExpression.checkPositiveInt(capacity, "Sức chứa danh sách hóa đơn phải > 0");
        this.invoices = new Invoice[capacity];
        this.count = 0;
    }

    public void addInvoice(Invoice invoice) {
        if (invoice == null) throw new IllegalArgumentException("Hóa đơn không được null");
        for (int i = 0; i < count; i++) {
            if (invoices[i].getStayId().equals(invoice.getStayId())) {
                throw new IllegalArgumentException("Lượt ở " + invoice.getStayId() + " đã có hóa đơn " + invoices[i].getInvoiceId());
            }
        }
        if (count >= invoices.length) {
            Invoice[] newArray = new Invoice[invoices.length * 2];
            System.arraycopy(invoices, 0, newArray, 0, count);
            invoices = newArray;
        }
        invoices[count++] = invoice;
    }

    public Invoice createInvoiceForStay(StayRecord stay, double roomUnitPrice,
                                        double depositShare, CheckOutReceipt receipt,
                                        String employeeId) {
        if (stay == null) throw new IllegalArgumentException("Lượt lưu trú không được null");
        CheckingExpression.checkPositiveDouble(roomUnitPrice, "Giá phòng phải > 0");
        CheckingExpression.checkNonNegativeDouble(depositShare, "Tiền cọc không được âm");

        Invoice existing = findByStayId(stay.getStayId());
        if (existing != null) return existing;

        Invoice invoice = new Invoice(stay.getStayId(), employeeId, depositShare);
        long nights = stay.calculateNights();
        invoice.addDetail(new InvoiceDetail(
                InvoiceDetail.generateId(), invoice.getInvoiceId(), Invoice.LineType.ROOM,
                "Phòng " + stay.getRoomId() + ", " + nights + " đêm",
                stay.getRoomId(), null, null, nights, roomUnitPrice));

        for (ServiceUsage usage : stay.getUsages()) {
            if (usage.isCancelled() || usage.getUnitPriceAtUse() <= 0) continue;
            invoice.addDetail(new InvoiceDetail(
                    InvoiceDetail.generateId(), invoice.getInvoiceId(), Invoice.LineType.SERVICE,
                    "Dịch vụ " + usage.getServiceId(), null, usage.getServiceId(),
                    usage.getUsageId(), usage.getQuantity(), usage.getUnitPriceAtUse()));
        }

        if (receipt != null && receipt.getLateHours() > 0) {
            double fee = receipt.getLateHours() * LATE_CHECKOUT_FEE_PER_HOUR;
            invoice.addExtraCharge(new ExtraCharge(
                    ExtraCharge.generateId(), invoice.getInvoiceId(),
                    "Trả phòng muộn " + receipt.getLateHours() + " giờ",
                    fee, employeeId));
        }

        addInvoice(invoice);
        return invoice;
    }

    public Invoice findByInvoiceId(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String target = id.trim();
        for (int i = 0; i < count; i++) {
            if (invoices[i].getInvoiceId().equalsIgnoreCase(target)) return invoices[i];
        }
        return null;
    }

    public Invoice findByStayId(String stayId) {
        if (stayId == null || stayId.trim().isEmpty()) return null;
        String target = stayId.trim();
        for (int i = 0; i < count; i++) {
            if (invoices[i].getStayId().equalsIgnoreCase(target)) return invoices[i];
        }
        return null;
    }

    public void processRefundIfNeeded(Invoice invoice, String managerId) {
        if (invoice == null) throw new IllegalArgumentException("Hóa đơn không được null");
        CheckingExpression.checkNotNullOrEmpty(managerId, "Hoàn tiền phải có mã quản lý duyệt");

        double remaining = invoice.calculateRemaining();
        if (remaining < 0) {
            double refundAmount = Math.abs(remaining);
            RefundRecord refund = new RefundRecord(
                RefundRecord.generateId(), invoice.getInvoiceId(), refundAmount, "Hoàn tiền cọc dư", managerId);
            invoice.addRefund(refund);
            System.out.println("Đã ghi nhận hoàn cọc dư: " + Invoice.formatMoney(refundAmount) + " VND");
        }
    }

    // ĐỌC ĐỒNG LOẠT TỪ 6 FILE TXT (tự bỏ qua dòng ghi chú bắt đầu bằng #)
    @Override
    public void readFile() {
        this.count = 0;

        try {
            for (String line : readDataLines("src/main/java/hotel/file/invoices.txt")) {
                addInvoice(Invoice.fromFileString(line));
            }

            for (String line : readDataLines("src/main/java/hotel/file/invoice_details.txt")) {
                InvoiceDetail d = InvoiceDetail.fromFileString(line);
                Invoice inv = findByInvoiceId(d.getInvoiceId());
                if (inv != null) inv.addDetailFromFile(d);
            }

            for (String line : readDataLines("src/main/java/hotel/file/extra_charges.txt")) {
                ExtraCharge ec = ExtraCharge.fromFileString(line);
                Invoice inv = findByInvoiceId(ec.getInvoiceId());
                if (inv != null) inv.addExtraChargeFromFile(ec);
            }

            for (String line : readDataLines("src/main/java/hotel/file/discounts.txt")) {
                Discount dc = Discount.fromFileString(line);
                Invoice inv = findByInvoiceId(dc.getInvoiceId());
                if (inv != null) inv.addDiscountFromFile(dc);
            }

            for (String line : readDataLines("src/main/java/hotel/file/payments.txt")) {
                Payment pm = Payment.fromFileString(line);
                Invoice inv = findByInvoiceId(pm.getInvoiceId());
                if (inv != null) inv.addPaymentFromFile(pm);
            }

            for (String line : readDataLines("src/main/java/hotel/file/refunds.txt")) {
                RefundRecord rf = RefundRecord.fromFileString(line);
                Invoice inv = findByInvoiceId(rf.getInvoiceId());
                if (inv != null) inv.addRefundFromFile(rf);
            }

        } catch (IOException | RuntimeException e) {
            System.out.println("Lỗi đọc file hóa đơn (P5): " + e.getMessage());
        }
    }

    private ArrayList<String> readDataLines(String path) throws IOException {
        ArrayList<String> valid = new ArrayList<>();
        for (String line : FileUtil.readLines(path)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                valid.add(trimmed);
            }
        }
        return valid;
    }

    // GHI ĐỒNG LOẠT RA 6 FILE TXT QUA FileUtil
    @Override
    public void writeFile() {
        int dCount = 0, ecCount = 0, dcCount = 0, pCount = 0, rCount = 0;
        for (int i = 0; i < count; i++) {
            Invoice inv = invoices[i];
            dCount += inv.getDetailCount();
            ecCount += inv.getExtraChargeCount();
            dcCount += inv.getDiscountCount();
            pCount += inv.getPaymentCount();
            rCount += inv.getRefundCount();
        }

        String[] invLines = new String[count];
        String[] dLines = new String[dCount];
        String[] ecLines = new String[ecCount];
        String[] dcLines = new String[dcCount];
        String[] pLines = new String[pCount];
        String[] rLines = new String[rCount];

        int dIdx = 0, ecIdx = 0, dcIdx = 0, pIdx = 0, rIdx = 0;

        for (int i = 0; i < count; i++) {
            Invoice inv = invoices[i];
            invLines[i] = inv.toFileString();

            for (int j = 0; j < inv.getDetailCount(); j++) dLines[dIdx++] = inv.getDetails()[j].toFileString();
            for (int j = 0; j < inv.getExtraChargeCount(); j++) ecLines[ecIdx++] = inv.getExtraCharges()[j].toFileString();
            for (int j = 0; j < inv.getDiscountCount(); j++) dcLines[dcIdx++] = inv.getDiscounts()[j].toFileString();
            for (int j = 0; j < inv.getPaymentCount(); j++) pLines[pIdx++] = inv.getPayments()[j].toFileString();
            for (int j = 0; j < inv.getRefundCount(); j++) rLines[rIdx++] = inv.getRefunds()[j].toFileString();
        }

        try {
            FileUtil.writeAllLines("src/main/java/hotel/file/invoices.txt", invLines, count);
            FileUtil.writeAllLines("src/main/java/hotel/file/invoice_details.txt", dLines, dCount);
            FileUtil.writeAllLines("src/main/java/hotel/file/extra_charges.txt", ecLines, ecCount);
            FileUtil.writeAllLines("src/main/java/hotel/file/discounts.txt", dcLines, dcCount);
            FileUtil.writeAllLines("src/main/java/hotel/file/payments.txt", pLines, pCount);
            FileUtil.writeAllLines("src/main/java/hotel/file/refunds.txt", rLines, rCount);
        } catch (IOException e) {
            System.out.println("Lỗi ghi file hóa đơn (P5): " + e.getMessage());
        }
    }

    public Invoice[] getAllInvoices() { return invoices; }
    public int getCount() { return count; }
}