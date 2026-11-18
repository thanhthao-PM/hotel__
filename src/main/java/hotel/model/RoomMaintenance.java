package hotel.model;

import hotel.util.CheckingExpression;   
import hotel.util.FileUtil;
import hotel.util.Validator;
import java.time.LocalDate;

// Bản ghi bảo trì của một phòng (không phải một loại phòng)
public class RoomMaintenance {

    // Bộ đếm sinh mã (static)
    private static int nextNumber = 1;

    private String maintenanceId;
    private String roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String reportedBy;  // mã Employee báo hỏng
    private boolean closed;

    // Constructor đầy đủ
    public RoomMaintenance(String maintenanceId, String roomId, LocalDate startDate,
                           LocalDate endDate, String reason, String reportedBy, boolean closed) {
        // kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(maintenanceId, "Mã bảo trì không được để trống");
        CheckingExpression.checkNotNullOrEmpty(roomId, "Mã phòng không được để trống");
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc bảo trì không được null");
        }
        // Cho phép bảo trì trong đúng một ngày (khớp với validatePeriod bên dưới)
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc bảo trì không được trước ngày bắt đầu");
        }
        CheckingExpression.checkNotNullOrEmpty(reason, "Lý do bảo trì không được để trống");
        CheckingExpression.checkNotNullOrEmpty(reportedBy, "Mã nhân viên báo hỏng không được để trống");

        this.maintenanceId = maintenanceId;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.reportedBy = reportedBy;
        this.closed = closed;
    }

    // Sinh mã bảo trì mới dạng MT001, MT002...
    public static String generateId() {
        String id = String.format("MT%03d", nextNumber);
        nextNumber++;
        return id;
    }

    // Nạp lại bộ đếm sau khi đọc file
    public static void syncCounter(String loadedId) {
        int n = FileUtil.extractNumber(loadedId);
        if (n >= nextNumber) {
            nextNumber = n + 1;
        }
    }

    // Đóng bản ghi bảo trì (bảo trì xong)
    public void closeMaintenance() {
        this.closed = true;
    }

    // Kiểm tra bản ghi còn hiệu lực vào một ngày cụ thể (chưa đóng và ngày nằm trong khoảng)
    public boolean isEffectiveOn(LocalDate date) {
        if (closed || date == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    // Kiểm tra khoảng bảo trì hợp lệ (có đủ ngày và ngày kết thúc không trước ngày bắt đầu)
    public boolean validatePeriod() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    // Ghi một dòng file
    public String toFileString() {
        return FileUtil.join(maintenanceId, roomId, startDate, endDate,
                FileUtil.clean(reason), reportedBy, closed);
    }

    // Đọc một dòng file tạo đối tượng RoomMaintenance
    public static RoomMaintenance fromFileString(String line) {
        String[] p = FileUtil.split(line);
        if (p.length < 7) {
            throw new IllegalArgumentException("Dòng dữ liệu bảo trì không đủ số trường: " + line);
        }
        return new RoomMaintenance(
                p[0].trim(),
                p[1].trim(),
                LocalDate.parse(p[2].trim()),
                LocalDate.parse(p[3].trim()),
                p[4].trim(),
                p[5].trim(),
                Boolean.parseBoolean(p[6].trim())
        );
    }

    // Thông tin in ra bảng
    public String getInfo() {
        return String.format("%-6s | Phòng %-6s | %s -> %s | %-9s | Báo bởi: %s | Lý do: %s",
                maintenanceId, roomId,
                startDate.format(Validator.DATE_FORMAT), endDate.format(Validator.DATE_FORMAT),
                closed ? "Đã đóng" : "Đang mở", reportedBy, reason);
    }

    public String getMaintenanceId() { return maintenanceId; }
    public String getRoomId() { return roomId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public String getReportedBy() { return reportedBy; }
    public boolean isClosed() { return closed; }
}