
package hotel.model;

import hotel.model.enums.RoomStatus;
import hotel.util.FileUtil;
import hotel.util.CheckingExpression;

public class StandardRoom extends Room {

    private String bedType;
    private int bedCount;

    public StandardRoom(String roomId, String roomNumber, int floor,
                        int capacity, double basePrice,
                        RoomStatus status, boolean active,
                        String bedType, int bedCount) {

        super(roomId, roomNumber, floor, capacity,
                basePrice, status, active);

        CheckingExpression.checkNotNullOrEmpty(
                bedType, "Loại giường không được để trống");

        CheckingExpression.checkPositiveInt(
                bedCount, "Số giường phải > 0");

        if (bedCount > 2) {
            throw new IllegalArgumentException(
                    "Phòng Standard chỉ hỗ trợ tối đa 2 giường");
        }

        if (capacity > bedCount * 2) {
            throw new IllegalArgumentException(
                    "Phòng có " + bedCount
                    + " giường chỉ được tối đa "
                    + (bedCount * 2) + " người");
        }

        this.bedType = bedType;
        this.bedCount = bedCount;
    }

    @Override
    public double calculateRoomPrice(int nights) {
        CheckingExpression.checkPositiveInt(
                nights, "Số đêm phải > 0");

        return Math.round(getBasePrice() * nights);
    }

    @Override
    public String getRoomInfo() {
        return "[Standard] " + super.getRoomInfo()
                + String.format(" | Giường: %s x%d", bedType, bedCount);
    }

    @Override
    public String toFileString() {
        return FileUtil.join(
                "STD", baseFileString(),
                FileUtil.clean(bedType), bedCount);
    }

    public String getBedType() { return bedType; }

    public int getBedCount() { return bedCount; }

    public void setBedType(String bedType) {
        CheckingExpression.checkNotNullOrEmpty(
                bedType, "Loại giường không được để trống");

        this.bedType = bedType;
    }

    public void setBedCount(int bedCount) {
        CheckingExpression.checkPositiveInt(
                bedCount, "Số giường phải > 0");

        if (bedCount > 2) {
            throw new IllegalArgumentException(
                    "Phòng Standard chỉ hỗ trợ tối đa 2 giường");
        }

        if (getCapacity() > bedCount * 2) {
            throw new IllegalArgumentException(
                    "Số giường mới không đủ đáp ứng sức chứa hiện tại");
        }

        this.bedCount = bedCount;
    }

    @Override
    public void setCapacity(int capacity) {
        CheckingExpression.checkPositiveInt(
                capacity, "Sức chứa phòng phải > 0");

        if (capacity > bedCount * 2) {
            throw new IllegalArgumentException(
                    "Phòng có " + bedCount
                    + " giường chỉ được tối đa "
                    + (bedCount * 2) + " người");
        }

        super.setCapacity(capacity);
    }
}
