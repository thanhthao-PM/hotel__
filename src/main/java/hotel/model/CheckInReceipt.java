package hotel.model;

import java.time.LocalDateTime;
import hotel.util.CheckingExpression; 

/**
 * Phiếu nhận phòng. earlyHours là thuộc tính BỔ SUNG so với kế hoạch: số giờ nhận sớm
 * (trước 14:00 đúng ngày nhận) để P5 tính ExtraCharge.
 */
public class CheckInReceipt {
    private static final String PREFIX = "CI";
    private static int nextNumber = 1;

    private final String receiptId;
    private final String stayId;
    private final LocalDateTime checkInTime;
    private final int guestCount;
    private final int earlyHours;
    private final String handledBy;
    private String note;

    public CheckInReceipt(String stayId, LocalDateTime checkInTime, int guestCount,
                          int earlyHours, String handledBy, String note) {
        this(generateId(), stayId, checkInTime, guestCount, earlyHours, handledBy, note);
    }

    public CheckInReceipt(String receiptId, String stayId, LocalDateTime checkInTime, int guestCount,
                          int earlyHours, String handledBy, String note) {
        //  kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(receiptId, "Mã phiếu nhận phòng không được để trống");
        CheckingExpression.checkNotNullOrEmpty(stayId, "Mã lượt ở không được để trống");
        if (checkInTime == null) {
            throw new IllegalArgumentException("Giờ nhận phòng không được null");
        }
        CheckingExpression.checkPositiveInt(guestCount, "Số khách phải > 0");
        CheckingExpression.checkNonNegativeInt(earlyHours, "Số giờ nhận sớm không được âm");
        CheckingExpression.checkNotNullOrEmpty(handledBy, "Mã nhân viên xử lý không được để trống");

        
        this.receiptId = receiptId;
        this.stayId = stayId;
        this.checkInTime = checkInTime;
        this.guestCount = guestCount;
        this.earlyHours = earlyHours;
        this.handledBy = handledBy;
        this.note = StayRecord.sanitize(note);
    }

    public CheckInReceipt(CheckInReceipt o) {
        this(o.receiptId, o.stayId, o.checkInTime, o.guestCount, o.earlyHours, o.handledBy, o.note);
    }

    public static String generateId() { return String.format("%s%03d", PREFIX, nextNumber++); }
    public static void updateCounterFromId(String id) {
        nextNumber = Math.max(nextNumber, ServiceUsage.parseNumber(id, PREFIX) + 1);
    }
    public static void resetCounter() { nextNumber = 1; }

    public String getReceiptInfo() {
        return String.format("%s | Lượt ở %s | %s | %d khách | nhận sớm %d giờ | NV %s%s",
                receiptId, stayId, checkInTime.toLocalDate() + " " + checkInTime.toLocalTime().withNano(0),
                guestCount, earlyHours, handledBy, note.isEmpty() ? "" : " | " + note);
    }

    /** In phiếu ra console, căn cột. */
    public void printReceipt() {
        System.out.println("==================== PHIẾU NHẬN PHÒNG ====================");
        System.out.printf("%-14s: %s%n", "Mã phiếu", receiptId);
        System.out.printf("%-14s: %s%n", "Lượt ở", stayId);
        System.out.printf("%-14s: %s %s%n", "Giờ nhận", checkInTime.toLocalDate(), checkInTime.toLocalTime().withNano(0));
        System.out.printf("%-14s: %d%n", "Số khách", guestCount);
        System.out.printf("%-14s: %d giờ%n", "Nhận sớm", earlyHours);
        System.out.printf("%-14s: %s%n", "Nhân viên", handledBy);
        if (!note.isEmpty()) System.out.printf("%-14s: %s%n", "Ghi chú", note);
        System.out.println("==========================================================");
    }

    public String getReceiptId() { return receiptId; }
    public String getStayId() { return stayId; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public int getGuestCount() { return guestCount; }
    public int getEarlyHours() { return earlyHours; }
    public String getHandledBy() { return handledBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = StayRecord.sanitize(note); }

    public String toFileLine() {
        return String.join("|", receiptId, stayId, checkInTime.toString(), Integer.toString(guestCount),
                handledBy, note);
    }

    public String toFileString() {
        return toFileLine();
    }

    public static CheckInReceipt fromFileLine(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length == 6) {
            if (f[0].trim().isEmpty() || f[1].trim().isEmpty()) throw new IllegalArgumentException("Thiếu mã");
            LocalDateTime time = LocalDateTime.parse(f[2].trim());
            int guests = Integer.parseInt(f[3].trim());
            String handler = f[4].trim();
            String noteText = f[5].trim();
            LocalDateTime standardCheckIn = LocalDateTime.of(time.toLocalDate(), StayRecord.STANDARD_CHECK_IN_TIME);
            int early = time.isBefore(standardCheckIn) ? StayRecord.ceilHours(time, standardCheckIn) : 0;
            return new CheckInReceipt(f[0].trim(), f[1].trim(), time, guests, early, handler, noteText);
        } else if (f.length == 7) {
            if (f[0].trim().isEmpty() || f[1].trim().isEmpty()) throw new IllegalArgumentException("Thiếu mã");
            return new CheckInReceipt(f[0].trim(), f[1].trim(), LocalDateTime.parse(f[2].trim()), Integer.parseInt(f[3].trim()),
                    Integer.parseInt(f[4].trim()), f[5].trim(), f[6].trim());
        }
        throw new IllegalArgumentException("Sai số trường (cần 6 hoặc 7, có " + f.length + ")");
    }

    public static CheckInReceipt fromFileString(String line) {
        return fromFileLine(line);
    }
}

