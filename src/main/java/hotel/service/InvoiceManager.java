package hotel.service;

import hotel.model.CheckOutReceipt;
import hotel.model.Discount;
import hotel.model.ExtraCharge;
import hotel.model.Invoice;
import hotel.model.InvoiceDetail;
import hotel.model.Payment;
import hotel.model.RefundRecord;
import hotel.model.ServiceUsage;
import hotel.model.StayLinks;
import hotel.model.StayRecord;
import hotel.storage.IFileHandler;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;

public class InvoiceManager implements IFileHandler, StayLinks.InvoiceGateway {

    // Thư mục chứa 6 file txt của nhóm (nếu sau này đổi sang "data" chỉ cần sửa dòng này)
    public static final String DATA_DIR = "src/main/java/hotel/file";
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

    // Cổng kết nối cho P4 (StayManager) gọi khi khách trả phòng
    @Override
    public String createInvoiceForStay(StayRecord stay, CheckOutReceipt receipt, String employeeId) {
        if (stay == null) throw new IllegalArgumentException("StayRecord không được null");
        Invoice existing = findByStayId(stay.getStayId());
        if (existing != null) return existing.getInvoiceId();

        Invoice inv = new Invoice(stay.getStayId(), employeeId, 0);

        // Chuyển các lượt dùng dịch vụ của P4 sang dòng chi tiết hóa đơn
        ServiceUsage[] usages = stay.getUsages();
        if (usages != null) {
            for (ServiceUsage u : usages) {
                if (u == null) continue;
                InvoiceDetail detail = new InvoiceDetail(
                    InvoiceDetail.generateId(),
                    inv.getInvoiceId(),
                    Invoice.LineType.SERVICE,
                    "Dịch vụ " + u.getServiceId(),
                    "",
                    u.getServiceId(),
                    u.getUsageId(),
                    u.getQuantity(),
                    u.getUnitPriceAtUse()
                );
                inv.addDetail(detail);
            }
        }

        // Nếu trả phòng muộn -> tự động thêm phụ thu
        if (receipt != null && receipt.getLateHours() > 0) {
            double fee = receipt.getLateHours() * LATE_CHECKOUT_FEE_PER_HOUR;
            ExtraCharge lateCharge = new ExtraCharge(
                ExtraCharge.generateId(),
                inv.getInvoiceId(),
                "Trả phòng muộn " + receipt.getLateHours() + " giờ",
                fee,
                employeeId
            );
            inv.addExtraCharge(lateCharge);
        }

        addInvoice(inv);
        return inv.getInvoiceId();
    }

    @Override
    public boolean hasInvoice(String stayId) {
        return findByStayId(stayId) != null;
    }

    // ĐỌC ĐỒNG LOẠT TỪ 6 FILE TXT (tự bỏ qua dòng ghi chú bắt đầu bằng #)
    @Override
    public void readFile() {
        this.count = 0;
        try {
            for (String line : readDataLines(DATA_DIR + "/invoices.txt")) {
                addInvoice(Invoice.fromFileString(line));
            }
            for (String line : readDataLines(DATA_DIR + "/invoice_details.txt")) {
                InvoiceDetail d = InvoiceDetail.fromFileString(line);
                Invoice inv = findByInvoiceId(d.getInvoiceId());
                if (inv != null) inv.addDetailFromFile(d);
            }
            for (String line : readDataLines(DATA_DIR + "/extra_charges.txt")) {
                ExtraCharge ec = ExtraCharge.fromFileString(line);
                Invoice inv = findByInvoiceId(ec.getInvoiceId());
                if (inv != null) inv.addExtraChargeFromFile(ec);
            }
            for (String line : readDataLines(DATA_DIR + "/discounts.txt")) {
                Discount dc = Discount.fromFileString(line);
                Invoice inv = findByInvoiceId(dc.getInvoiceId());
                if (inv != null) inv.addDiscountFromFile(dc);
            }
            for (String line : readDataLines(DATA_DIR + "/payments.txt")) {
                Payment pm = Payment.fromFileString(line);
                Invoice inv = findByInvoiceId(pm.getInvoiceId());
                if (inv != null) inv.addPaymentFromFile(pm);
            }
            for (String line : readDataLines(DATA_DIR + "/refunds.txt")) {
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
            FileUtil.writeAllLines(DATA_DIR + "/invoices.txt", invLines, count);
            FileUtil.writeAllLines(DATA_DIR + "/invoice_details.txt", dLines, dCount);
            FileUtil.writeAllLines(DATA_DIR + "/extra_charges.txt", ecLines, ecCount);
            FileUtil.writeAllLines(DATA_DIR + "/discounts.txt", dcLines, dcCount);
            FileUtil.writeAllLines(DATA_DIR + "/payments.txt", pLines, pCount);
            FileUtil.writeAllLines(DATA_DIR + "/refunds.txt", rLines, rCount);
        } catch (IOException e) {
            System.out.println("Lỗi ghi file hóa đơn (P5): " + e.getMessage());
        }
    }

    public Invoice[] getAllInvoices() { return invoices; }
    public int getCount() { return count; }
}