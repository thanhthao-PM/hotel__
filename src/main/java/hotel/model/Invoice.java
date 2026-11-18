package hotel.model;

import hotel.util.CheckingExpression;
import hotel.util.FileUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Invoice {

    public enum InvoiceStatus { OPEN, PARTIALLY_PAID, PAID, CANCELLED }
    public enum PaymentMethod { CASH, CARD, TRANSFER }
    public enum DiscountType { PERCENT, FIXED }
    public enum LineType { ROOM, SERVICE }

    public interface InvoiceAdjustment {
        double getAdjustmentAmount();
        default double getAdjustmentAmount(double baseAmount) {
            return getAdjustmentAmount();
        }
    }

    public static final DateTimeFormatter FILE_DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public static int nextNumber = 1;

    private String invoiceId;
    private String stayId;
    private LocalDateTime issuedAt;
    private String issuedBy;
    private InvoiceStatus status;
    private double depositApplied;

    private InvoiceDetail[] details = new InvoiceDetail[20];
    private int detailCount = 0;

    private Discount[] discounts = new Discount[5];
    private int discountCount = 0;

    private ExtraCharge[] extraCharges = new ExtraCharge[5];
    private int extraChargeCount = 0;

    private Payment[] payments = new Payment[10];
    private int paymentCount = 0;

    private RefundRecord[] refunds = new RefundRecord[5];
    private int refundCount = 0;

    // Constructor tạo hóa đơn mới
    public Invoice(String stayId, String issuedBy, double depositApplied) {
        CheckingExpression.checkNotNullOrEmpty(stayId, "Mã lưu trú không được trống");
        CheckingExpression.checkNotNullOrEmpty(issuedBy, "Mã nhân viên lập hóa đơn không được trống");
        CheckingExpression.checkNonNegativeDouble(depositApplied, "Tiền cọc áp dụng không được âm");

        this.invoiceId = String.format("IV%03d", nextNumber++);
        this.stayId = stayId.trim();
        this.issuedAt = LocalDateTime.now();
        this.issuedBy = issuedBy.trim();
        this.status = InvoiceStatus.OPEN;
        this.depositApplied = depositApplied;
    }

    // Constructor nạp từ file
    public Invoice(String invoiceId, String stayId, LocalDateTime issuedAt,
                   String issuedBy, InvoiceStatus status, double depositApplied) {
        CheckingExpression.checkNotNullOrEmpty(invoiceId, "Mã hóa đơn không được trống");
        CheckingExpression.checkNotNullOrEmpty(stayId, "Mã lưu trú không được trống");
        CheckingExpression.checkNotNullOrEmpty(issuedBy, "Mã nhân viên lập hóa đơn không được trống");
        CheckingExpression.checkNonNegativeDouble(depositApplied, "Tiền cọc áp dụng không được âm");
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái hóa đơn không được null");
        }

        this.invoiceId = invoiceId.trim();
        this.stayId = stayId.trim();
        this.issuedAt = (issuedAt != null) ? issuedAt : LocalDateTime.now();
        this.issuedBy = issuedBy.trim();
        this.status = status;
        this.depositApplied = depositApplied;
        updateNextNumber(this.invoiceId);
    }

    public static void updateNextNumber(String id) {
        int num = FileUtil.extractNumber(id);
        if (num >= nextNumber) {
            nextNumber = num + 1;
        }
    }

    public static String formatMoney(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.format("%.0f", value);
        }
        return String.valueOf(value);
    }

    public void setDepositApplied(double depositApplied) {
        CheckingExpression.checkNonNegativeDouble(depositApplied, "Tiền cọc áp dụng không được âm");
        this.depositApplied = depositApplied;
    }

    public String getInvoiceId() { return invoiceId; }
    public String getStayId() { return stayId; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public String getIssuedBy() { return issuedBy; }
    public InvoiceStatus getStatus() { return status; }
    public double getDepositApplied() { return depositApplied; }

    // Tiền phòng + Tiền dịch vụ
    public double calculateDetailsTotal() {
        double sum = 0;
        for (int i = 0; i < detailCount; i++) {
            sum += details[i].getAmount();
        }
        return sum;
    }

    // Tổng phụ thu (số dương)
    public double calculateExtraChargeTotal() {
        double sum = 0;
        for (int i = 0; i < extraChargeCount; i++) {
            sum += extraCharges[i].getAdjustmentAmount();
        }
        return sum;
    }

    // Tổng giảm giá (trả về số âm nhờ đa hình; PERCENT tính trên tiền phòng + dịch vụ + phụ thu)
    public double calculateDiscountTotal() {
        double base = calculateDetailsTotal() + calculateExtraChargeTotal();
        double sum = 0;
        for (int i = 0; i < discountCount; i++) {
            sum += discounts[i].getAdjustmentAmount(base);
        }
        return sum;
    }

    // Tổng tiền hóa đơn = (tiền phòng + dịch vụ + phụ thu) + giảm giá (số âm)
    public double calculateTotal() {
        double base = calculateDetailsTotal() + calculateExtraChargeTotal();
        double total = base;
        for (int i = 0; i < discountCount; i++) {
            total += discounts[i].getAdjustmentAmount(base);
        }
        return Math.max(0, total);
    }

    // Tổng số tiền khách đã thanh toán
    public double calculatePaid() {
        double paid = 0;
        for (int i = 0; i < paymentCount; i++) {
            paid += payments[i].getAmount();
        }
        return paid;
    }

    // Tổng số tiền đã hoàn lại cho khách
    public double calculateRefunded() {
        double refunded = 0;
        for (int i = 0; i < refundCount; i++) {
            refunded += refunds[i].getAmount();
        }
        return refunded;
    }

    // Số tiền khách còn nợ (âm nghĩa là khách cọc/trả dư -> cần hoàn tiền)
    public double calculateRemaining() {
        return calculateTotal() - depositApplied - calculatePaid() + calculateRefunded();
    }

    private void ensureNotCancelled() {
        if (this.status == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Hóa đơn " + invoiceId + " đã bị hủy, không thể thao tác thêm");
        }
    }

    private static void checkNotNullObject(Object obj, String errorMessage) {
        if (obj == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public void addDetail(InvoiceDetail detail) {
        ensureNotCancelled();
        addDetailFromFile(detail);
    }

    public void addDetailFromFile(InvoiceDetail detail) {
        checkNotNullObject(detail, "Dòng chi tiết hóa đơn không được null");
        if (detailCount >= details.length) {
            InvoiceDetail[] grown = new InvoiceDetail[details.length * 2];
            System.arraycopy(details, 0, grown, 0, detailCount);
            details = grown;
        }
        details[detailCount++] = detail;
    }

    public void addExtraCharge(ExtraCharge charge) {
        ensureNotCancelled();
        addExtraChargeFromFile(charge);
    }

    public void addExtraChargeFromFile(ExtraCharge charge) {
        checkNotNullObject(charge, "Phụ thu không được null");
        if (extraChargeCount >= extraCharges.length) {
            ExtraCharge[] grown = new ExtraCharge[extraCharges.length * 2];
            System.arraycopy(extraCharges, 0, grown, 0, extraChargeCount);
            extraCharges = grown;
        }
        extraCharges[extraChargeCount++] = charge;
    }

    public void addDiscount(Discount discount) {
        ensureNotCancelled();
        addDiscountFromFile(discount);
    }

    public void addDiscountFromFile(Discount discount) {
        checkNotNullObject(discount, "Giảm giá không được null");
        if (discountCount >= discounts.length) {
            Discount[] grown = new Discount[discounts.length * 2];
            System.arraycopy(discounts, 0, grown, 0, discountCount);
            discounts = grown;
        }
        discounts[discountCount++] = discount;
    }

    public void addPayment(Payment payment) {
        ensureNotCancelled();
        addPaymentFromFile(payment);
        if (calculateRemaining() <= 0) {
            this.status = InvoiceStatus.PAID;
        } else {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public void addPaymentFromFile(Payment payment) {
        checkNotNullObject(payment, "Phiếu thanh toán không được null");
        if (paymentCount >= payments.length) {
            Payment[] grown = new Payment[payments.length * 2];
            System.arraycopy(payments, 0, grown, 0, paymentCount);
            payments = grown;
        }
        payments[paymentCount++] = payment;
    }

    public void addRefund(RefundRecord refund) {
        checkNotNullObject(refund, "Phiếu hoàn tiền không được null");
        double maxRefundable = calculatePaid() + depositApplied - calculateRefunded();
        if (refund.getAmount() > maxRefundable) {
            throw new IllegalArgumentException(
                "Số tiền hoàn (" + formatMoney(refund.getAmount()) + ") vượt quá tổng có thể hoàn (" + formatMoney(maxRefundable) + ")");
        }
        addRefundFromFile(refund);
        if (this.status != InvoiceStatus.CANCELLED && calculateRemaining() <= 0) {
            this.status = InvoiceStatus.PAID;
        }
    }

    public void addRefundFromFile(RefundRecord refund) {
        checkNotNullObject(refund, "Phiếu hoàn tiền không được null");
        if (refundCount >= refunds.length) {
            RefundRecord[] grown = new RefundRecord[refunds.length * 2];
            System.arraycopy(refunds, 0, grown, 0, refundCount);
            refunds = grown;
        }
        refunds[refundCount++] = refund;
    }

    public void cancel(String reason) {
        CheckingExpression.checkNotNullOrEmpty(reason, "Phải có lý do hủy hóa đơn");
        if (this.status == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Hóa đơn đã bị hủy trước đó");
        }
        this.status = InvoiceStatus.CANCELLED;
    }

    public String toFileString() {
        return FileUtil.join(
            invoiceId,
            stayId,
            issuedAt.format(FILE_DT_FORMAT),
            issuedBy,
            status.name(),
            formatMoney(depositApplied)
        );
    }

    public static Invoice fromFileString(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 6) {
            throw new IllegalArgumentException("Dòng dữ liệu hóa đơn không hợp lệ: " + line);
        }
        LocalDateTime time = LocalDateTime.parse(p[2].trim());
        InvoiceStatus st = InvoiceStatus.valueOf(p[4].trim());
        double deposit = Double.parseDouble(p[5].trim());
        return new Invoice(p[0].trim(), p[1].trim(), time, p[3].trim(), st, deposit);
    }

    public void printReceipt() {
        System.out.println("\n==================== HÓA ĐƠN [" + invoiceId + "] ====================");
        System.out.println("Mã lưu trú  : " + stayId);
        System.out.println("Ngày lập    : " + issuedAt.format(FILE_DT_FORMAT));
        System.out.println("Nhân viên   : " + issuedBy);
        System.out.println("Trạng thái  : " + status);

        System.out.println("\n--- Chi tiết tiền phòng & dịch vụ ---");
        if (detailCount == 0) {
            System.out.println("  (Chưa có dòng chi tiết)");
        } else {
            for (int i = 0; i < detailCount; i++) {
                InvoiceDetail d = details[i];
                System.out.printf("  [%s] %-28s | SL: %-4s | ĐG: %10s | TT: %10s%n",
                    d.getLineType(), d.getDescription(), formatMoney(d.getQuantity()),
                    formatMoney(d.getUnitPrice()), formatMoney(d.getAmount()));
            }
        }

        if (extraChargeCount > 0) {
            System.out.println("\n--- Phụ thu ---");
            for (int i = 0; i < extraChargeCount; i++) {
                ExtraCharge ec = extraCharges[i];
                System.out.printf("  [%s] %-35s : +%s VND%n",
                    ec.getChargeId(), ec.getReason(), formatMoney(ec.getAdjustmentAmount()));
            }
        }

        if (discountCount > 0) {
            double base = calculateDetailsTotal() + calculateExtraChargeTotal();
            System.out.println("\n--- Giảm giá ---");
            for (int i = 0; i < discountCount; i++) {
                Discount dc = discounts[i];
                System.out.printf("  [%s] %-35s : %s VND%n",
                    dc.getDiscountId(), dc.getReason(), formatMoney(dc.getAdjustmentAmount(base)));
            }
        }

        if (paymentCount > 0) {
            System.out.println("\n--- Lịch sử thanh toán ---");
            for (int i = 0; i < paymentCount; i++) {
                Payment pm = payments[i];
                System.out.printf("  [%s] %s | %-8s | %s VND (NV: %s)%n",
                    pm.getPaymentId(), pm.getPaidAt().format(FILE_DT_FORMAT),
                    pm.getMethod(), formatMoney(pm.getAmount()), pm.getReceivedBy());
            }
        }

        if (refundCount > 0) {
            System.out.println("\n--- Lịch sử hoàn tiền ---");
            for (int i = 0; i < refundCount; i++) {
                RefundRecord rf = refunds[i];
                System.out.printf("  [%s] %s | %s VND | Lý do: %s (Duyệt: %s)%n",
                    rf.getRefundId(), rf.getRefundedAt().format(FILE_DT_FORMAT),
                    formatMoney(rf.getAmount()), rf.getReason(), rf.getApprovedBy());
            }
        }

        System.out.println("------------------------------------------------------------");
        System.out.println("Tổng hóa đơn        : " + formatMoney(calculateTotal()) + " VND");
        System.out.println("Tiền cọc đã trừ     : " + formatMoney(depositApplied) + " VND");
        System.out.println("Khách đã thanh toán : " + formatMoney(calculatePaid()) + " VND");
        if (refundCount > 0) {
            System.out.println("Đã hoàn tiền        : " + formatMoney(calculateRefunded()) + " VND");
        }
        System.out.println("=> CÒN PHẢI THU     : " + formatMoney(calculateRemaining()) + " VND");
        System.out.println("============================================================\n");
    }

    public InvoiceDetail[] getDetails() { return details; }
    public int getDetailCount() { return detailCount; }
    public ExtraCharge[] getExtraCharges() { return extraCharges; }
    public int getExtraChargeCount() { return extraChargeCount; }
    public Discount[] getDiscounts() { return discounts; }
    public int getDiscountCount() { return discountCount; }
    public Payment[] getPayments() { return payments; }
    public int getPaymentCount() { return paymentCount; }
    public RefundRecord[] getRefunds() { return refunds; }
    public int getRefundCount() { return refundCount; }
}