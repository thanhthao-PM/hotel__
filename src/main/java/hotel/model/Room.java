package hotel.model;

import hotel.model.enums.RoomStatus;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

// Lớp trừu tượng cha của các loại phòng
public abstract class Room {

    // Bộ đếm sinh mã tự động (static), nạp lại từ file khi khởi động
    private static int nextNumber = 1;

    private String roomId;
    private String roomNumber;
    private int floor;
    private int capacity;
    private double basePrice;      // giá một đêm
    private RoomStatus status;     // chỉ đổi qua changeStatus()
    private boolean active;        // false khi xóa mềm

    // Constructor đầy đủ (dùng cho cả tạo mới và nạp từ file)
    public Room(String roomId, String roomNumber, int floor, int capacity,
                double basePrice, RoomStatus status, boolean active) {
        // kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(roomId, "Mã phòng không được để trống");
        CheckingExpression.checkNotNullOrEmpty(roomNumber, "Số phòng không được để trống");
        CheckingExpression.checkPositiveInt(floor, "Tầng phải > 0");
        CheckingExpression.checkPositiveInt(capacity, "Sức chứa phòng phải > 0");
        CheckingExpression.checkPositiveDouble(basePrice, "Giá phòng một đêm phải > 0");
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái phòng không được null");
        }

        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.capacity = capacity;
        this.basePrice = basePrice;
        this.status = status;
        this.active = active;
    }

    // Sinh mã phòng mới dạng R001, R002...
    public static String generateId() {
        String id = String.format("R%03d", nextNumber);
        nextNumber++;
        return id;
    }

    // Nạp lại bộ đếm sau khi đọc file để không sinh trùng mã
    public static void syncCounter(String loadedId) {
        int n = FileUtil.extractNumber(loadedId);
        if (n >= nextNumber) {
            nextNumber = n + 1;
        }
    }

    // Tính tiền phòng cho số đêm cho trước (mỗi loại phòng tính khác nhau)
    public abstract double calculateRoomPrice(int nights);

    // Chuyển thành một dòng để ghi file (mỗi loại phòng tự ghi thêm thuộc tính riêng)
    public abstract String toFileString();

    // Kiểm tra việc chuyển trạng thái từ "from" sang "to" có hợp lệ không
    // AVAILABLE -> BOOKED, OCCUPIED, MAINTENANCE
    // BOOKED -> AVAILABLE, OCCUPIED, MAINTENANCE
    // OCCUPIED -> CLEANING (không được sang MAINTENANCE)
    // CLEANING -> AVAILABLE, MAINTENANCE
    // MAINTENANCE -> AVAILABLE
    public static boolean isValidTransition(RoomStatus from, RoomStatus to) {
        if (from == to) {
            return false;
        }
        switch (from) {
            case AVAILABLE:
                return to == RoomStatus.BOOKED || to == RoomStatus.OCCUPIED
                        || to == RoomStatus.MAINTENANCE;
            case BOOKED:
                return to == RoomStatus.AVAILABLE || to == RoomStatus.OCCUPIED
                        || to == RoomStatus.MAINTENANCE;
            case OCCUPIED:
                return to == RoomStatus.CLEANING;
            case CLEANING:
                return to == RoomStatus.AVAILABLE || to == RoomStatus.MAINTENANCE;
            case MAINTENANCE:
                return to == RoomStatus.AVAILABLE;
            default:
                return false;
        }
    }

    // Đổi trạng thái phòng có kiểm soát: sai quy tắc thì không đổi và trả false
    public boolean changeStatus(RoomStatus newStatus) {
        if (newStatus == null || !isValidTransition(this.status, newStatus)) {
            return false;
        }
        this.status = newStatus;
        return true;
    }

    // Trả về chuỗi thông tin phòng để in ra bảng
    public String getRoomInfo() {
        return String.format("%-6s | Phòng %-5s | Tầng %-2d | Sức chứa %-2d | Giá gốc %,12.0f | %-11s | %s",
                roomId, roomNumber, floor, capacity, basePrice, status,
                active ? "Hoạt động" : "Đã xóa");
    }

    // Phần ghi file chung của mọi loại phòng
    protected String baseFileString() {
        return FileUtil.join(roomId, roomNumber, floor, capacity, basePrice, status, active);
    }

    // Getter
    public String getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public int getFloor() { return floor; }
    public int getCapacity() { return capacity; }
    public double getBasePrice() { return basePrice; }
    public RoomStatus getStatus() { return status; }
    public boolean isActive() { return active; }

    // Setter (không có setter cho status để module khác không đổi trực tiếp)
    public void setRoomNumber(String roomNumber) { 
        CheckingExpression.checkNotNullOrEmpty(roomNumber, "Số phòng không được để trống");
        this.roomNumber = roomNumber; }
    public void setFloor(int floor) { 
        CheckingExpression.checkPositiveInt(floor, "Tầng phải > 0");
        this.floor = floor; }
    public void setCapacity(int capacity) { 
        CheckingExpression.checkPositiveInt(capacity, "Sức chứa phòng phải > 0");
        this.capacity = capacity; }
    public void setBasePrice(double basePrice) { 
        CheckingExpression.checkPositiveDouble(basePrice, "Giá phòng một đêm phải > 0");
        this.basePrice = basePrice; }
    public void setActive(boolean active) { this.active = active; }
}