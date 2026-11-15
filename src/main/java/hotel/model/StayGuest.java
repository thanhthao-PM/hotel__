package hotel.model;

/**
 * Khách đang ở trong một lượt ở (StayRecord): hoặc là Customer, hoặc là GuestMember.
 * Class này là "KhachLuuTru / StayGuest" trong ERD. Bản kế hoạch mới bị mất class này
 * (xem file so sánh, mục 7), nên P4 khôi phục lại theo ERD.
 * Đúng một trong customerId hoặc guestMemberId có giá trị.
 */
public class StayGuest {
    private static final String PREFIX = "SG";
    private static int nextNumber = 1;

    private final String id;
    private final String stayId;
    private final String customerId;     // rỗng nếu là GuestMember
    private final String guestMemberId;  // rỗng nếu là Customer

    public StayGuest(String stayId, String guestId, boolean isCustomer) {
        this(generateId(), stayId, isCustomer ? guestId : "", isCustomer ? "" : guestId);
    }

    public StayGuest(String id, String stayId, String customerId, String guestMemberId) {
        boolean hasCustomer = customerId != null && !customerId.isEmpty();
        boolean hasMember = guestMemberId != null && !guestMemberId.isEmpty();
        if (hasCustomer == hasMember) {
            throw new IllegalArgumentException("Phải có đúng một trong customerId hoặc guestMemberId");
        }
        this.id = id;
        this.stayId = stayId;
        this.customerId = hasCustomer ? customerId : "";
        this.guestMemberId = hasMember ? guestMemberId : "";
    }

    /** Constructor sao chép nhưng gắn sang lượt ở khác (dùng khi đổi phòng) và sinh mã mới. */
    public StayGuest(StayGuest other, String newStayId) {
        this(generateId(), newStayId, other.customerId, other.guestMemberId);
    }

    public static String generateId() {
        return String.format("%s%03d", PREFIX, nextNumber++);
    }

    public static void updateCounterFromId(String id) {
        nextNumber = Math.max(nextNumber, ServiceUsage.parseNumber(id, PREFIX) + 1);
    }

    public static void resetCounter() {
        nextNumber = 1;
    }

    public boolean isCustomer() { return !customerId.isEmpty(); }
    /** Mã của người ở, bất kể là Customer hay GuestMember. */
    public String getGuestId() { return isCustomer() ? customerId : guestMemberId; }
    public String getId() { return id; }
    public String getStayId() { return stayId; }
    public String getCustomerId() { return customerId; }
    public String getGuestMemberId() { return guestMemberId; }

    public String toFileLine() {
        return String.join("|", id, stayId, customerId, guestMemberId);
    }

    public static StayGuest fromFileLine(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length != 4) throw new IllegalArgumentException("Sai số trường (cần 4, có " + f.length + ")");
        if (f[0].isEmpty() || f[1].isEmpty()) throw new IllegalArgumentException("Thiếu mã");
        return new StayGuest(f[0], f[1], f[2], f[3]);
    }

    @Override
    public String toString() {
        return (isCustomer() ? "Khách " : "Thành viên ") + getGuestId();
    }
}


