
package hotel.model;

import hotel.model.enums.RoomStatus;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

// Phòng VIP: có 1 giường, tối đa 2 người, giá có phụ thu
public class VipRoom extends Room {

    private double surchargeRate;  // Hệ số phụ thu, ví dụ 0.3 = 30%
    private String amenities;      // Tiện nghi
    private String viewType;       // Hướng nhìn

    // Phòng VIP luôn có 1 giường
    private static final int BED_COUNT = 1;

    // Constructor đầy đủ
    public VipRoom(String roomId, String roomNumber, int floor,
                   int capacity, double basePrice,
                   RoomStatus status, boolean active,
                   double surchargeRate, String amenities,
                   String viewType) {

        super(roomId, roomNumber, floor, capacity,
                basePrice, status, active);

        // Phòng VIP tối đa 2 người
        if (capacity > 2) {
            throw new IllegalArgumentException(
                    "Phòng VIP chỉ được tối đa 2 người");
        }

        // Hệ số phụ thu không được âm
        CheckingExpression.checkNonNegativeDouble(
                surchargeRate, "Hệ số phụ thu không được âm");

        // Hướng nhìn không được để trống
        CheckingExpression.checkNotNullOrEmpty(
                viewType, "Hướng nhìn không được để trống");

        this.surchargeRate = surchargeRate;
        this.amenities = FileUtil.clean(amenities);
        this.viewType = FileUtil.clean(viewType);
    }

    // Giá phòng = giá gốc x (1 + phụ thu) x số đêm
    @Override
    public double calculateRoomPrice(int nights) {
        CheckingExpression.checkPositiveInt(
                nights, "Số đêm phải > 0");

        return Math.round(
                getBasePrice() * (1 + surchargeRate) * nights);
    }

    // Hiển thị thông tin phòng VIP
    @Override
    public String getRoomInfo() {
        return "[VIP]      " + super.getRoomInfo()
                + String.format(
                    " | Giường: %d | Phụ thu %.0f%% | View: %s | Tiện nghi: %s",
                    BED_COUNT,
                    surchargeRate * 100,
                    viewType,
                    amenities
                );
    }

    // Ghi thông tin phòng VIP vào file
    // Giữ nguyên cấu trúc dữ liệu rooms.txt
    @Override
    public String toFileString() {
        return FileUtil.join(
                "VIP",
                baseFileString(),
                surchargeRate,
                FileUtil.clean(amenities),
                FileUtil.clean(viewType)
        );
    }

    // Getter
    public double getSurchargeRate() {
        return surchargeRate;
    }

    public String getAmenities() {
        return amenities;
    }

    public String getViewType() {
        return viewType;
    }

    public int getBedCount() {
        return BED_COUNT;
    }

    // Setter
    public void setSurchargeRate(double surchargeRate) {
        CheckingExpression.checkNonNegativeDouble(
                surchargeRate, "Hệ số phụ thu không được âm");

        this.surchargeRate = surchargeRate;
    }

    public void setAmenities(String amenities) {
        this.amenities = FileUtil.clean(amenities);
    }

    public void setViewType(String viewType) {
        CheckingExpression.checkNotNullOrEmpty(
                viewType, "Hướng nhìn không được để trống");

        this.viewType = FileUtil.clean(viewType);
    }

    // Phòng VIP chỉ được tối đa 2 người
    @Override
    public void setCapacity(int capacity) {
        CheckingExpression.checkPositiveInt(
                capacity, "Sức chứa phòng phải > 0");

        if (capacity > 2) {
            throw new IllegalArgumentException(
                    "Phòng VIP chỉ được tối đa 2 người");
        }

        super.setCapacity(capacity);
    }
}
