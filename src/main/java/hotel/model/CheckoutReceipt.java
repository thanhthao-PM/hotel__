package hotel.model;

import java.time.LocalDateTime;
import hotel.util.CheckingExpression;    

/**
 * Phiếu trả phòng. lateHours dùng để P5 tính ExtraCharge.
 * dueCheckOut là thuộc tính BỔ SUNG so với kế hoạch: giờ trả phòng quy định
 * (ngày trả trong BookingDetail + 12:00), để calculateLateHours() tự tính được.
 */
public class CheckOutReceipt {
    private static final String PREFIX = "CO";
    private static int nextNumber = 1;

    private final String receiptId;
    private final String stayId;
    private final LocalDateTime checkOutTime;
    private final String roomCondition;
    private final LocalDateTime dueCheckOut; // có thể null (ví dụ phiếu của lượt đổi phòng)
    private final int lateHours;
    private final String handledBy;
    private String note;

    public CheckOutReceipt(String stayId, LocalDateTime checkOutTime, String roomCondition,
                           LocalDateTime dueCheckOut, String handledBy, String note) {
        this(generateId(), stayId, checkOutTime, roomCondition, dueCheckOut, -1, handledBy, note);
    }

    /** lateHours âm nghĩa là tự tính từ checkOutTime và dueCheckOut. */
    public CheckOutReceipt(String receiptId, String stayId, LocalDateTime checkOutTime, String roomCondition,
                           LocalDateTime dueCheckOut, int lateHours, String handledBy, String note) {
        // kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(receiptId, "Mã phiếu trả phòng không được để trống");
        CheckingExpression.checkNotNullOrEmpty(stayId, "Mã lượt ở không được để trống");
        if (checkOutTime == null) {
            throw new IllegalArgumentException("Giờ trả phòng không được null");
        }
        CheckingExpression.checkNotNullOrEmpty(handledBy, "Mã nhân viên xử lý không được để trống");


        this.receiptId = receiptId;
        this.stayId = stayId;
        this.checkOutTime = checkOutTime;
        this.roomCondition = StayRecord.sanitize(roomCondition);
        this.dueCheckOut = dueCheckOut;
        this.handledBy = handledBy;
        this.note = StayRecord.sanitize(note);
        this.lateHours = lateHours >= 0 ? lateHours : calculateLateHours();
    }

    public CheckOutReceipt(CheckOutReceipt o) {
        this(o.receiptId, o.stayId, o.checkOutTime, o.roomCondition, o.dueCheckOut, o.lateHours, o.handledBy, o.note);
    }

    public static String generateId() { return String.format("%s%03d", PREFIX, nextNumber++); }
    public static void updateCounterFromId(String id) {
        nextNumber = Math.max(nextNumber, ServiceUsage.parseNumber(id, PREFIX) + 1);
    }
    public static void resetCounter() { nextNumber = 1; }

    /** Số giờ trả muộn, làm tròn lên theo giờ. Trả đúng hạn hoặc sớm thì 0. */
    public int calculateLateHours() {
        if (dueCheckOut == null) return 0;
        return StayRecord.ceilHours(dueCheckOut, checkOutTime);
    }

    public String getReceiptInfo() {
        return String.format("%s | Lượt ở %s | %s | tình trạng: %s | trả muộn %d giờ | NV %s%s",
                receiptId, stayId, checkOutTime.toLocalDate() + " " + checkOutTime.toLocalTime().withNano(0),
                roomCondition.isEmpty() ? "-" : roomCondition, lateHours, handledBy,
                note.isEmpty() ? "" : " | " + note);
    }

    public void printReceipt() {
        System.out.println("==================== PHIẾU TRẢ PHÒNG =====================");
        System.out.printf("%-14s: %s%n", "Mã phiếu", receiptId);
        System.out.printf("%-14s: %s%n", "Lượt ở", stayId);
        System.out.printf("%-14s: %s %s%n", "Giờ trả", checkOutTime.toLocalDate(), checkOutTime.toLocalTime().withNano(0));
        System.out.printf("%-14s: %s%n", "Tình trạng", roomCondition.isEmpty() ? "-" : roomCondition);
        System.out.printf("%-14s: %d giờ%n", "Trả muộn", lateHours);
        System.out.printf("%-14s: %s%n", "Nhân viên", handledBy);
        if (!note.isEmpty()) System.out.printf("%-14s: %s%n", "Ghi chú", note);
        System.out.println("==========================================================");
    }

    public String getReceiptId() { return receiptId; }
    public String getStayId() { return stayId; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public String getRoomCondition() { return roomCondition; }
    public LocalDateTime getDueCheckOut() { return dueCheckOut; }
    public int getLateHours() { return lateHours; }
    public String getHandledBy() { return handledBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = StayRecord.sanitize(note); }

    public String toFileLine() {
        return String.join("|", receiptId, stayId, checkOutTime.toString(), roomCondition,
                dueCheckOut == null ? "" : dueCheckOut.toString(), Integer.toString(lateHours), handledBy, note);
    }

    public static CheckOutReceipt fromFileLine(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length != 8) throw new IllegalArgumentException("Sai số trường (cần 8, có " + f.length + ")");
        if (f[0].isEmpty() || f[1].isEmpty()) throw new IllegalArgumentException("Thiếu mã");
        return new CheckOutReceipt(f[0], f[1], LocalDateTime.parse(f[2]), f[3],
                f[4].isEmpty() ? null : LocalDateTime.parse(f[4]), Integer.parseInt(f[5]), f[6], f[7]);
    }
}
