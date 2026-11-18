
package hotel.model;

import hotel.model.enums.RoomStatus;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

public abstract class Room {

    private static int nextNumber = 1;

    private String roomId;
    private String roomNumber;
    private int floor;
    private int capacity;
    private double basePrice;
    private RoomStatus status;
    private boolean active;

    public Room(String roomId, String roomNumber, int floor, int capacity,
                double basePrice, RoomStatus status, boolean active) {

        CheckingExpression.checkNotNullOrEmpty(
                roomId, "Mã phòng không được để trống");

        validateRoomNumber(roomNumber, floor);

        if (floor < 1 || floor > 4) {
            throw new IllegalArgumentException("Khách sạn chỉ có từ tầng 1 đến tầng 4");
        }

        CheckingExpression.checkPositiveInt(
                capacity, "Sức chứa phòng phải > 0");

        CheckingExpression.checkPositiveDouble(
                basePrice, "Giá phòng một đêm phải > 0");

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

    private static void validateRoomNumber(String roomNumber, int floor) {
        CheckingExpression.checkNotNullOrEmpty(
                roomNumber, "Số phòng không được để trống");

        if (!roomNumber.matches("[1-4][0-9]{2}")) {
            throw new IllegalArgumentException(
                    "Số phòng phải có dạng 101 đến 499");
        }

        if (roomNumber.charAt(0) - '0' != floor) {
            throw new IllegalArgumentException(
                    "Tầng không khớp với số phòng");
        }
    }

    public static String generateId(String roomNumber) {
        CheckingExpression.checkNotNullOrEmpty(
                roomNumber, "Số phòng không được để trống");

        return "RO" + roomNumber;
    }

    // Giữ lại hàm này để tránh lỗi nếu nơi khác vẫn gọi generateId()
    public static String generateId() {
        String id = String.format("RO%03d", nextNumber);
        nextNumber++;
        return id;
    }

    public static void syncCounter(String loadedId) {
        int n = FileUtil.extractNumber(loadedId);
        if (n >= nextNumber) {
            nextNumber = n + 1;
        }
    }

    public abstract double calculateRoomPrice(int nights);

    public abstract String toFileString();

    public static Room fromFileString(String line) {
        String[] p = FileUtil.split(line);

        if (p.length < 8) {
            throw new IllegalArgumentException(
                    "Dòng dữ liệu phòng không đủ số trường: " + line);
        }

        String type = p[0].trim();
        String id = p[1].trim();
        String number = p[2].trim();
        int floor = Integer.parseInt(p[3].trim());
        int capacity = Integer.parseInt(p[4].trim());
        double price = Double.parseDouble(p[5].trim());
        RoomStatus status = RoomStatus.valueOf(p[6].trim());
        boolean active = Boolean.parseBoolean(p[7].trim());

        if ("VIP".equalsIgnoreCase(type)) {
            if (p.length < 11) {
                throw new IllegalArgumentException(
                        "Dòng phòng VIP không đủ trường: " + line);
            }

            double surcharge = Double.parseDouble(p[8].trim());
            String amenities = p[9].trim();
            String viewType = p[10].trim();

            return new VipRoom(
                    id, number, floor, capacity, price,
                    status, active, surcharge, amenities, viewType);

        } else if ("STD".equalsIgnoreCase(type)
                || "STANDARD".equalsIgnoreCase(type)) {

            if (p.length < 10) {
                throw new IllegalArgumentException(
                        "Dòng phòng Standard không đủ trường: " + line);
            }

            String bedType = p[8].trim();
            int bedCount = Integer.parseInt(p[9].trim());

            return new StandardRoom(
                    id, number, floor, capacity, price,
                    status, active, bedType, bedCount);
        }

        throw new IllegalArgumentException("Loại phòng không hợp lệ: " + type);
    }

    public static boolean isValidTransition(RoomStatus from, RoomStatus to) {
        if (from == to) return false;

        switch (from) {
            case AVAILABLE:
                return to == RoomStatus.BOOKED
                        || to == RoomStatus.OCCUPIED
                        || to == RoomStatus.MAINTENANCE;

            case BOOKED:
                return to == RoomStatus.AVAILABLE
                        || to == RoomStatus.OCCUPIED
                        || to == RoomStatus.MAINTENANCE;

            case OCCUPIED:
                return to == RoomStatus.CLEANING;

            case CLEANING:
                return to == RoomStatus.AVAILABLE
                        || to == RoomStatus.MAINTENANCE;

            case MAINTENANCE:
                return to == RoomStatus.AVAILABLE;

            default:
                return false;
        }
    }

    public boolean changeStatus(RoomStatus newStatus) {
        if (newStatus == null || !isValidTransition(this.status, newStatus)) {
            return false;
        }

        this.status = newStatus;
        return true;
    }

    public String getRoomInfo() {
        return String.format(
                "%-6s | Phòng %-5s | Tầng %-2d | Sức chứa %-2d | Giá gốc %,12.0f | %-11s | %s",
                roomId, roomNumber, floor, capacity, basePrice, status,
                active ? "Hoạt động" : "Đã xóa");
    }

    protected String baseFileString() {
        return FileUtil.join(
                roomId, roomNumber, floor, capacity,
                basePrice, status, active);
    }

    public String getRoomId() { return roomId; }

    public String getRoomNumber() { return roomNumber; }

    public int getFloor() { return floor; }

    public int getCapacity() { return capacity; }

    public double getBasePrice() { return basePrice; }

    public RoomStatus getStatus() { return status; }

    public boolean isActive() { return active; }

    public void setRoomNumber(String roomNumber) {
        validateRoomNumber(roomNumber, floor);
        this.roomNumber = roomNumber;
    }

    public void setFloor(int floor) {
        if (floor < 1 || floor > 4) {
            throw new IllegalArgumentException(
                    "Khách sạn chỉ có từ tầng 1 đến tầng 4");
        }

        validateRoomNumber(roomNumber, floor);
        this.floor = floor;
    }

    public void setCapacity(int capacity) {
        CheckingExpression.checkPositiveInt(
                capacity, "Sức chứa phòng phải > 0");
        this.capacity = capacity;
    }

    public void setBasePrice(double basePrice) {
        CheckingExpression.checkPositiveDouble(
                basePrice, "Giá phòng một đêm phải > 0");
        this.basePrice = basePrice;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
