package hotel.model;

import java.time.LocalDateTime;
import hotel.util.CheckingExpression;  

/**
 * Một dòng dịch vụ khách dùng trong một lượt ở. Không tính vào các class chính.
 * unitPriceAtUse là giá tại thời điểm dùng: đổi giá dịch vụ sau này không làm đổi hóa đơn cũ.
 */
public class ServiceUsage {
    private static final String PREFIX = "SU";
    private static int nextNumber = 1; // bộ đếm sinh mã, nạp lại từ file khi khởi động

    private final String usageId;
    private final String stayId;
    private final String serviceId;
    private double quantity;
    private final double unitPriceAtUse;
    private final LocalDateTime usedAt;
    private final String recordedBy;
    private boolean cancelled;

    /** Tạo dòng dịch vụ mới (tự sinh mã). */
    public ServiceUsage(String stayId, String serviceId, double quantity,
                        double unitPriceAtUse, LocalDateTime usedAt, String recordedBy) {
        this(generateId(), stayId, serviceId, quantity, unitPriceAtUse, usedAt, recordedBy, false);
    }

    /** Constructor đầy đủ, dùng khi nạp từ file. */
    public ServiceUsage(String usageId, String stayId, String serviceId, double quantity,
                        double unitPriceAtUse, LocalDateTime usedAt, String recordedBy, boolean cancelled) {
        // kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(usageId, "Mã lượt dùng dịch vụ không được để trống");
        CheckingExpression.checkNotNullOrEmpty(stayId, "Mã lượt ở không được để trống");
        CheckingExpression.checkNotNullOrEmpty(serviceId, "Mã dịch vụ không được để trống");
        if (!isValidQuantity(quantity)) {
            throw new IllegalArgumentException("Số lượng phải là số dương");
        }
        CheckingExpression.checkNonNegativeDouble(unitPriceAtUse, "Giá tại thời điểm dùng không được âm");
        if (usedAt == null) {
            throw new IllegalArgumentException("Thời điểm dùng dịch vụ không được null");
        }
        CheckingExpression.checkNotNullOrEmpty(recordedBy, "Mã nhân viên ghi nhận không được để trống");

        this.usageId = usageId;
        this.stayId = stayId;
        this.serviceId = serviceId;
        this.quantity = quantity;
        this.unitPriceAtUse = unitPriceAtUse;
        this.usedAt = usedAt;
        this.recordedBy = recordedBy;
        this.cancelled = cancelled;
    }

    /** Constructor sao chép. */
    public ServiceUsage(ServiceUsage other) {
        this(other.usageId, other.stayId, other.serviceId, other.quantity,
                other.unitPriceAtUse, other.usedAt, other.recordedBy, other.cancelled);
    }

    // ---------- static: bộ đếm mã ----------
    public static String generateId() {
        return String.format("%s%03d", PREFIX, nextNumber++);
    }

    /** Gọi sau khi nạp file để bộ đếm không sinh mã trùng. */
    public static void updateCounterFromId(String id) {
        nextNumber = Math.max(nextNumber, parseNumber(id, PREFIX) + 1);
    }

    public static void resetCounter() {
        nextNumber = 1;
    }

    static int parseNumber(String id, String prefix) {
        try {
            if (id != null && id.startsWith(prefix)) return Integer.parseInt(id.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            // mã lạ thì bỏ qua
        }
        return 0;
    }

    // ---------- nghiệp vụ ----------
    /** Thành tiền = số lượng x giá tại lúc dùng. Dòng đã hủy tính 0. */
    public double calculateCost() {
        return cancelled ? 0 : quantity * unitPriceAtUse;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isValidQuantity() {
        return isValidQuantity(quantity);
    }

    public static boolean isValidQuantity(double q) {
        return q > 0 && !Double.isNaN(q) && !Double.isInfinite(q);
    }

    // ---------- getter / setter ----------
    public String getUsageId() { return usageId; }
    public String getStayId() { return stayId; }
    public String getServiceId() { return serviceId; }
    public double getQuantity() { return quantity; }
    public double getUnitPriceAtUse() { return unitPriceAtUse; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public String getRecordedBy() { return recordedBy; }
    public boolean isCancelled() { return cancelled; }

    public void setQuantity(double quantity) {
        if (!isValidQuantity(quantity)) throw new IllegalArgumentException("Số lượng phải là số dương");
        this.quantity = quantity;
    }

    // ---------- file ----------
    public String toFileLine() {
        String qtyStr = (quantity == (long) quantity) ? String.valueOf((long) quantity) : Double.toString(quantity);
        String priceStr = (unitPriceAtUse == (long) unitPriceAtUse) ? String.valueOf((long) unitPriceAtUse) : Double.toString(unitPriceAtUse);
        return String.join("|", usageId, stayId, serviceId, qtyStr, priceStr, usedAt.toString(), recordedBy, Boolean.toString(cancelled));
    }

    public String toFileString() {
        return toFileLine();
    }

    public static ServiceUsage fromFileLine(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length != 8) throw new IllegalArgumentException("Sai số trường (cần 8, có " + f.length + ")");
        ServiceUsage u = new ServiceUsage(f[0].trim(), f[1].trim(), f[2].trim(), Double.parseDouble(f[3].trim()),
                Double.parseDouble(f[4].trim()), LocalDateTime.parse(f[5].trim()), f[6].trim(), Boolean.parseBoolean(f[7].trim()));
        if (u.usageId.isEmpty() || u.stayId.isEmpty() || u.serviceId.isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã");
        }
        return u;
    }

    public static ServiceUsage fromFileString(String line) {
        return fromFileLine(line);
    }

    @Override
    public String toString() {
        return String.format("%-6s %-8s SL=%-6.1f Giá=%,.0f Thành tiền=%,.0f %s%s",
                usageId, serviceId, quantity, unitPriceAtUse, calculateCost(),
                usedAt.toLocalDate() + " " + usedAt.toLocalTime().withNano(0), cancelled ? " [ĐÃ HỦY]" : "");
    }
}

