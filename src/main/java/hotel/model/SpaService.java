package hotel.model;

import hotel.util.FileUtil;
import hotel.util.CheckingExpression;

// Dịch vụ spa: tính tiền theo gói hoặc theo giờ
public class SpaService extends HotelService {

    private String treatmentType;   // loại liệu trình
    private int durationMinutes;    // thời lượng (phút)

    // Constructor đầy đủ
    public SpaService(String serviceId, String name, double unitPrice, String unit,
                      String description, boolean available,
                      String treatmentType, int durationMinutes) {
        super(serviceId, name, unitPrice, unit, description, available);
        // loại liệu trình không được null hoặc rỗng
        CheckingExpression.checkNotNullOrEmpty(treatmentType, "Loại liệu trình không được để trống");
        // thời lượng phải > 0
        CheckingExpression.checkPositiveInt(durationMinutes, "Thời lượng phải > 0");

        this.treatmentType = treatmentType;
        this.durationMinutes = durationMinutes;
    }

    // Ghi đè: số lượng là số gói hoặc số giờ (cho phép lẻ như 1.5 giờ); tiền = đơn giá x số lượng
    @Override
    public double calculateCost(double quantity) {
        // số lượng phải > 0
        CheckingExpression.checkPositiveDouble(quantity, "Số lượng phải > 0");
        return Math.round(getUnitPrice() * quantity);
    }

    // Ghi đè: thêm thông tin riêng của dịch vụ spa
    @Override
    public String getServiceInfo() {
        return "[Spa] " + super.getServiceInfo()
                + String.format(" | Liệu trình: %s | Thời lượng: %d phút", treatmentType, durationMinutes);
    }

    // Ghi đè: một dòng file của dịch vụ spa
    @Override
    public String toFileString() {
        return FileUtil.join("SPA", baseFileString(), FileUtil.clean(treatmentType), durationMinutes);
    }

    public String getTreatmentType() { return treatmentType; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setTreatmentType(String treatmentType) { 
        CheckingExpression.checkNotNullOrEmpty(treatmentType, "Loại liệu trình không được để trống");
        this.treatmentType = treatmentType; }
    public void setDurationMinutes(int durationMinutes) { 
        CheckingExpression.checkPositiveInt(durationMinutes, "Thời lượng phải > 0");
        this.durationMinutes = durationMinutes; }
}