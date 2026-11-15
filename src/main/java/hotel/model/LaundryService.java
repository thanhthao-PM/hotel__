package hotel.model;

import hotel.util.FileUtil;

// Dịch vụ giặt ủi: tính tiền theo kg hoặc theo món
public class LaundryService extends HotelService {

    private String chargeBy;       // "PER_KG" hoặc "PER_ITEM"
    private int turnaroundHours;   // thời gian giặt xong (giờ)

    // Constructor đầy đủ
    public LaundryService(String serviceId, String name, double unitPrice, String unit,
                          String description, boolean available,
                          String chargeBy, int turnaroundHours) {
        super(serviceId, name, unitPrice, unit, description, available);
        this.chargeBy = chargeBy;
        this.turnaroundHours = turnaroundHours;
    }

    // Ghi đè: số lượng là số kg (PER_KG) hoặc số món (PER_ITEM); tiền = đơn giá x số lượng
    // Nếu tính theo món thì số lượng được làm tròn lên thành số nguyên món
    @Override
    public double calculateCost(double quantity) {
        double q = chargeBy.equalsIgnoreCase("PER_ITEM") ? Math.ceil(quantity) : quantity;
        return Math.round(getUnitPrice() * q);
    }

    // Ghi đè: thêm thông tin riêng của dịch vụ giặt ủi
    @Override
    public String getServiceInfo() {
        return "[Giặt ủi] " + super.getServiceInfo()
                + String.format(" | Tính theo: %s | Trả đồ sau: %d giờ", chargeBy, turnaroundHours);
    }

    // Ghi đè: một dòng file của dịch vụ giặt ủi
    @Override
    public String toFileString() {
        return FileUtil.join("LAUNDRY", baseFileString(), FileUtil.clean(chargeBy), turnaroundHours);
    }

    public String getChargeBy() { return chargeBy; }
    public int getTurnaroundHours() { return turnaroundHours; }
    public void setChargeBy(String chargeBy) { this.chargeBy = chargeBy; }
    public void setTurnaroundHours(int turnaroundHours) { this.turnaroundHours = turnaroundHours; }
}