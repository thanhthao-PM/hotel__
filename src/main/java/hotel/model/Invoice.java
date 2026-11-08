package hotel.model;

import hotel.util.CheckingExpression;
import java.time.LocalDateTime;

public class Invoice {
    public enum InvoiceStatus { OPEN, PARTIALLY_PAID, PAID, CANCELLED }
    public enum PaymentMethod { CASH, CARD, TRANSFER }
    public enum DiscountType { PERCENT, FIXED}
    public enum LineType { ROOM, SERVICE}

    public interface InvoiceAdjustment { 
        double getAdjustmentAmount();
    }

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

    public Invoice(String stayId, String issuedBy, double depositApplied) {

        CheckingExpression.checkNotNullOrEmpty(stayId, "Mã lưu trú không được trống");

        this.invoiceId = "INV" + (nextNumber++);
        this.stayId = stayId;
        this.issuedAt = LocalDateTime.now();
        this.issuedBy = issuedBy;
        this.status = InvoiceStatus.OPEN;

        setDepositApplied(depositApplied);
    }

    public void setDepositApplied(double depositApplied) {
        CheckingExpression.checkNonNegativeDouble(depositApplied, "Tiền đặt cọc áp dụng không được âm");
        this.depositApplied = depositApplied;
    }

    public String getInvoiceId() { return invoiceId; }
    public String getStayId() { return stayId; }
    public InvoiceStatus getStatus() { return status; }
    public double getDepositApplied() { return depositApplied; }

// Hàm tính tổng tiền hóa đơn
    public double calculateTotal() {
        double total = 0;

        for (int i = 1; i < detailCount; i++) {
            total += details[i].getAmount();
        }

        for (int i = 0; i < extraChargeCount; i++) {
            total += extraCharges[i].getAdijustmentAmount();
        }

        for (int i = 0; i < discountCount; i++) {
            total += discounts[i].getAdjustmentAmount();
        }
        return total;
    }
//hàm tính tổng số tiền khách đã trả
    public double calculatePaid() {
        double paid = 0;
        for (int i = 0; i < paymentCount; i++) {
            paid += payments[i].getAmount();
        }
        return paid;
    }

//khách còn nợ
    public double calculateRemaining() {
        return calculateTotal() - depositApplied - calculatePaid();
    }

    public void addPayment(Payment payment) {
        if (paymentCount < payments.length) {
            payments[paymentCount++] = payment;

            if (calculateRemaining() <= 0) {
                this.status = InvoiceStatus.PAID;
            } else {
                this.status = InvoiceStatus.PARTIALLY_PAID;
            }
        }
    }

    public void addDetail(InvoiceDetail detail) {
        if (detailCount < details.length) details[detailCount++] = detail;
    }

    public void addDiscount(Discount discount) {
        if (discountCount < discounts.length) discounts[discountCount++] = discount;
    }

    public void addExtraCharge(ExtraCharge charge) {
        if (extraChargeCount < extraCharges.length) extraCharges[extraChargeCount++] = charge;
    }

    public void addRefund(RefundRecord refund) {
        if (refundCount < refunds.length) refunds[refundCount++] = refund;
    }

    public void cancel(String reason) {
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do hủy hóa đơn không được trống");
        this.status = InvoiceStatus.CANCELLED;
    }
}
