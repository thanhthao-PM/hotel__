package hotel.model;

import hotel.model.enums.StayStatus;
import hotel.util.CheckingExpression;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * Một lượt ở của MỘT phòng (mục 2, dòng 4 của kế hoạch). Một Booking có 1..n StayRecord.
 *
 * Khác kế hoạch: thuộc tính guestIds (String[]) được thay bằng mảng guests (StayGuest[])
 * theo ERD; getGuestIds() vẫn trả về mảng mã nên các module khác dùng như cũ.
 */
public class StayRecord {
    // ---------- static ----------
    public static final LocalTime STANDARD_CHECK_IN_TIME = LocalTime.of(14, 0);
    public static final LocalTime STANDARD_CHECK_OUT_TIME = LocalTime.of(12, 0);
    private static final String PREFIX = "ST";
    private static final int INITIAL_CAPACITY = 4;
    private static int nextNumber = 1;

    private final String stayId;
    private final String bookingId;
    private final String bookingDetailId;
    private final String roomId;
    private StayGuest[] guests;
    private int guestCount;
    private LocalDateTime actualCheckIn;
    private LocalDateTime actualCheckOut;
    private StayStatus status;
    private ServiceUsage[] usages;
    private int usageCount;

    /** Tạo lượt ở mới (tự sinh mã). Gọi checkIn() ngay sau đó. */
    public StayRecord(String bookingId, String bookingDetailId, String roomId) {
        this(generateId(), bookingId, bookingDetailId, roomId, null, null, StayStatus.ACTIVE);
    }

    /** Constructor đầy đủ, dùng khi nạp file. */
    public StayRecord(String stayId, String bookingId, String bookingDetailId, String roomId,
                      LocalDateTime actualCheckIn, LocalDateTime actualCheckOut, StayStatus status) {
        // kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(stayId, "Mã lượt ở không được để trống");
        CheckingExpression.checkNotNullOrEmpty(bookingId, "Mã đặt phòng không được để trống");
        CheckingExpression.checkNotNullOrEmpty(bookingDetailId, "Mã dòng đặt phòng không được để trống");
        CheckingExpression.checkNotNullOrEmpty(roomId, "Mã phòng không được để trống");
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái lượt ở không được null");
        }
        // giờ trả (nếu có) không được trước giờ nhận
        if (actualCheckOut != null) {
            if (actualCheckIn == null) {
                throw new IllegalArgumentException("Có giờ trả nhưng chưa có giờ nhận");
            }
            if (actualCheckOut.isBefore(actualCheckIn)) {
                throw new IllegalArgumentException("Giờ trả trước giờ nhận");
            }
        }
        
        this.stayId = stayId;
        this.bookingId = bookingId;
        this.bookingDetailId = bookingDetailId;
        this.roomId = roomId;
        this.actualCheckIn = actualCheckIn;
        this.actualCheckOut = actualCheckOut;
        this.status = status;
        this.guests = new StayGuest[INITIAL_CAPACITY];
        this.usages = new ServiceUsage[INITIAL_CAPACITY];
    }

    /** Constructor sao chép (sao chép sâu: khách và dịch vụ cũng được sao chép). */
    public StayRecord(StayRecord o) {
        this(o.stayId, o.bookingId, o.bookingDetailId, o.roomId, o.actualCheckIn, o.actualCheckOut, o.status);
        for (int i = 0; i < o.guestCount; i++) loadGuest(new StayGuest(o.guests[i].getId(), o.guests[i].getStayId(),
                o.guests[i].getCustomerId(), o.guests[i].getGuestMemberId()));
        for (int i = 0; i < o.usageCount; i++) loadUsage(new ServiceUsage(o.usages[i]));
    }

    public static String generateId() { return String.format("%s%03d", PREFIX, nextNumber++); }
    public static void updateCounterFromId(String id) {
        nextNumber = Math.max(nextNumber, ServiceUsage.parseNumber(id, PREFIX) + 1);
    }
    public static void resetCounter() { nextNumber = 1; }

    /** Làm tròn lên theo giờ; to không sau from thì 0. */
    public static int ceilHours(LocalDateTime from, LocalDateTime to) {
        // chặn null, tránh NullPointerException ở to.isAfter(from)
        if (from == null || to == null) {
            throw new IllegalArgumentException("Mốc thời gian không được null");
        }
        if (!to.isAfter(from)) return 0;
        long minutes = Duration.between(from, to).toMinutes();
        if (minutes == 0) return 1; // lệch dưới 1 phút nhưng vẫn muộn
        return (int) ((minutes + 59) / 60);
    }

    /** Dữ liệu ghi file không được chứa ký tự ngăn cách '|' hay xuống dòng: thay khi nhập. */
    public static String sanitize(String s) {
        if (s == null) return "";
        return s.replace('|', '/').replace('\r', ' ').replace('\n', ' ').trim();
    }

    // ---------- nghiệp vụ ----------
    public void checkIn() { checkIn(LocalDateTime.now()); }

    public void checkIn(LocalDateTime when) {
        // giờ nhận không được null
        if (when == null) throw new IllegalArgumentException("Giờ nhận phòng không được null");
        if (actualCheckIn != null) throw new IllegalStateException("Lượt ở " + stayId + " đã nhận phòng");
        if (guestCount == 0) throw new IllegalStateException("Chưa có khách nào trong lượt ở " + stayId);
        this.actualCheckIn = when;
        this.status = StayStatus.ACTIVE;
    }

    public void checkOut() { checkOut(LocalDateTime.now()); }

    public void checkOut(LocalDateTime when) {
        close(when, StayStatus.CHECKED_OUT);
    }

    /** Đóng lượt ở vì khách chuyển sang phòng khác (dùng khi đổi phòng giữa kỳ). */
    public void closeForRoomChange(LocalDateTime when) {
        close(when, StayStatus.ROOM_CHANGED);
    }

    private void close(LocalDateTime when, StayStatus newStatus) {
        // giờ trả không được null (trước đó when.isBefore(...) sẽ ném NullPointerException)
        if (when == null) throw new IllegalArgumentException("Giờ trả phòng không được null");
        if (status != StayStatus.ACTIVE) throw new IllegalStateException("Lượt ở " + stayId + " không còn ở trạng thái ACTIVE");
        if (actualCheckIn == null) throw new IllegalStateException("Lượt ở " + stayId + " chưa nhận phòng");
        if (when.isBefore(actualCheckIn)) throw new IllegalArgumentException("Giờ trả trước giờ nhận");
        this.actualCheckOut = when;
        this.status = newStatus;
    }

    /** Chỉ hợp lệ khi lượt ở đang ACTIVE và số lượng dương. */
    public void addServiceUsage(ServiceUsage u) {
        if (u == null) throw new IllegalArgumentException("Dòng dịch vụ rỗng");
        if (status != StayStatus.ACTIVE) throw new IllegalStateException("Chỉ ghi dịch vụ cho lượt ở đang ACTIVE");
        if (!u.getStayId().equals(stayId)) throw new IllegalArgumentException("Dòng dịch vụ thuộc lượt ở khác");
        if (!u.isValidQuantity()) throw new IllegalArgumentException("Số lượng phải là số dương");
        loadUsage(u);
    }

    /** Hủy dòng dịch vụ ghi nhầm. Trả về false nếu không có hoặc đã hủy rồi. */
    public boolean cancelServiceUsage(String usageId) {
        ServiceUsage u = findUsage(usageId);
        if (u == null || u.isCancelled()) return false;
        u.cancel();
        return true;
    }

    /** Số đêm: nhận chiều ngày 1, trả sáng ngày 2 là 1 đêm; cùng ngày tính tối thiểu 1 đêm. */
    public long calculateNights() {
        return calculateNights(actualCheckOut != null ? actualCheckOut : LocalDateTime.now());
    }

    public long calculateNights(LocalDateTime until) {
        if (actualCheckIn == null) return 0;
        // until null sẽ gây NullPointerException ở until.toLocalDate()
        if (until == null) throw new IllegalArgumentException("Mốc thời gian tính số đêm không được null");
        long days = ChronoUnit.DAYS.between(actualCheckIn.toLocalDate(), until.toLocalDate());
        return Math.max(1, days);
    }

    /** Tổng tiền các dòng dịch vụ chưa hủy. */
    public double calculateServiceTotal() {
        double sum = 0;
        for (int i = 0; i < usageCount; i++) sum += usages[i].calculateCost();
        return sum;
    }

    // ---------- khách ----------
    public void addGuest(StayGuest g) {
        if (g == null || !g.getStayId().equals(stayId)) throw new IllegalArgumentException("Khách không thuộc lượt ở này");
        if (hasGuest(g.getGuestId())) throw new IllegalArgumentException("Khách " + g.getGuestId() + " đã có trong lượt ở");
        loadGuest(g);
    }

    /** Dùng khi nạp file, không kiểm tra trạng thái. */
    void loadGuest(StayGuest g) {
        if (guestCount == guests.length) guests = Arrays.copyOf(guests, guests.length * 2);
        guests[guestCount++] = g;
    }

    public void loadUsage(ServiceUsage u) {
        if (usageCount == usages.length) usages = Arrays.copyOf(usages, usages.length * 2);
        usages[usageCount++] = u;
    }

    public boolean hasGuest(String guestId) {
        for (int i = 0; i < guestCount; i++) if (guests[i].getGuestId().equals(guestId)) return true;
        return false;
    }

    /** Thay toàn bộ danh sách khách (dùng khi sửa lượt ở đang ACTIVE). */
    public void replaceGuests(StayGuest[] newGuests) {
        guests = new StayGuest[Math.max(INITIAL_CAPACITY, newGuests.length)];
        guestCount = 0;
        for (StayGuest g : newGuests) loadGuest(g);
    }

    public String[] getGuestIds() {
        String[] ids = new String[guestCount];
        for (int i = 0; i < guestCount; i++) ids[i] = guests[i].getGuestId();
        return ids;
    }

    public StayGuest[] getGuests() { return Arrays.copyOf(guests, guestCount); }
    public int getGuestCount() { return guestCount; }

    // ---------- dịch vụ ----------
    public ServiceUsage findUsage(String usageId) {
        for (int i = 0; i < usageCount; i++) if (usages[i].getUsageId().equals(usageId)) return usages[i];
        return null;
    }

    public ServiceUsage[] getUsages() { return Arrays.copyOf(usages, usageCount); }
    public int getUsageCount() { return usageCount; }

    // ---------- getter ----------
    public String getStayId() { return stayId; }
    public String getBookingId() { return bookingId; }
    public String getBookingDetailId() { return bookingDetailId; }
    public String getRoomId() { return roomId; }
    public LocalDateTime getActualCheckIn() { return actualCheckIn; }
    public LocalDateTime getActualCheckOut() { return actualCheckOut; }
    public StayStatus getStatus() { return status; }
    public boolean isActive() { return status == StayStatus.ACTIVE; }

    // ---------- file (khách và dịch vụ có file riêng) ----------
    public String toFileLine() {
        return String.join("|", stayId, bookingId, bookingDetailId, roomId,
                actualCheckIn == null ? "" : actualCheckIn.toString(),
                actualCheckOut == null ? "" : actualCheckOut.toString(), status.name());
    }

    public static StayRecord fromFileLine(String line) {
        // dòng file không được null hoặc rỗng
        CheckingExpression.checkNotNullOrEmpty(line, "Dòng dữ liệu StayRecord không được để trống");
        String[] f = line.split("\\|", -1);
        if (f.length != 7) throw new IllegalArgumentException("Sai số trường (cần 7, có " + f.length + ")");
        for (int i = 0; i < 4; i++) if (f[i].isEmpty()) throw new IllegalArgumentException("Thiếu mã ở trường " + (i + 1));
        return new StayRecord(f[0], f[1], f[2], f[3],
                f[4].isEmpty() ? null : LocalDateTime.parse(f[4]),
                f[5].isEmpty() ? null : LocalDateTime.parse(f[5]), StayStatus.valueOf(f[6]));
    }

    public String getInfo() {
        return String.format("%-6s | Booking %-6s | Phòng %-6s | %-13s | nhận %s | trả %s | %d khách | %d dịch vụ",
                stayId, bookingId, roomId, status.getLabel(),
                actualCheckIn == null ? "-" : actualCheckIn.toLocalDate() + " " + actualCheckIn.toLocalTime().withNano(0),
                actualCheckOut == null ? "-" : actualCheckOut.toLocalDate() + " " + actualCheckOut.toLocalTime().withNano(0),
                guestCount, usageCount);
    }

    @Override
    public String toString() { return getInfo(); }
}

