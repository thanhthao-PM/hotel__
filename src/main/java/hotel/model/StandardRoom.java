package hotel.model;

import hotel.model.enums.RoomStatus;
import hotel.util.FileUtil;


// Phòng thường: kế thừa Room, tính giá theo giá gốc
public class StandardRoom extends Room {

    private String bedType;  // loại giường (đơn, đôi...)
    private int bedCount;    // số giường

    // Constructor đầy đủ
    public StandardRoom(String roomId, String roomNumber, int floor, int capacity,
                        double basePrice, RoomStatus status, boolean active,
                        String bedType, int bedCount) {
        super(roomId, roomNumber, floor, capacity, basePrice, status, active);
        this.bedType = bedType;
        this.bedCount = bedCount;
    }

    // Ghi đè: giá = giá gốc x số đêm, làm tròn đến đồng
    @Override
    public double calculateRoomPrice(int nights) {
        return Math.round(getBasePrice() * nights);
    }

    // Ghi đè: thêm thông tin riêng của phòng thường
    @Override
    public String getRoomInfo() {
        return "[Standard] " + super.getRoomInfo()
                + String.format(" | Giường: %s x%d", bedType, bedCount);
    }

    // Ghi đè: một dòng file của phòng thường
    @Override
    public String toFileString() {
        return FileUtil.join("STANDARD", baseFileString(), FileUtil.clean(bedType), bedCount);
    }

    public String getBedType() { return bedType; }
    public int getBedCount() { return bedCount; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public void setBedCount(int bedCount) { this.bedCount = bedCount; }
}