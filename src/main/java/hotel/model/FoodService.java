package hotel.model;

import hotel.util.FileUtil;
import hotel.util.CheckingExpression; 

// Dịch vụ ăn uống: tính tiền theo số món
public class FoodService extends HotelService {

    private String category;          // nhóm: đồ ăn sáng, đồ uống...
    private int preparationMinutes;   // thời gian chuẩn bị

    // Constructor đầy đủ
    public FoodService(String serviceId, String name, double unitPrice, String unit,
                       String description, boolean available,
                       String category, int preparationMinutes) {
        super(serviceId, name, unitPrice, unit, description, available);
        // THÊM: kiểm tra phần riêng của dịch vụ ăn uống
        CheckingExpression.checkNotNullOrEmpty(category, "Nhóm món không được để trống");
        CheckingExpression.checkNonNegativeInt(preparationMinutes, "Thời gian chuẩn bị không được âm");

        this.category = category;
        this.preparationMinutes = preparationMinutes;
    }

    // Ghi đè: tiền = đơn giá x số món, làm tròn đến đồng
    @Override
    public double calculateCost(double quantity) {
        // THÊM: số món phải dương
        CheckingExpression.checkPositiveDouble(quantity, "Số lượng món phải > 0");
        return Math.round(getUnitPrice() * quantity);
    }

    // Ghi đè: thêm thông tin riêng của dịch vụ ăn uống
    @Override
    public String getServiceInfo() {
        return "[Ăn uống] " + super.getServiceInfo()
                + String.format(" | Nhóm: %s | Chuẩn bị: %d phút", category, preparationMinutes);
    }

    // Ghi đè: một dòng file của dịch vụ ăn uống
    @Override
    public String toFileString() {
        return FileUtil.join("FOOD", baseFileString(), FileUtil.clean(category), preparationMinutes);
    }

    public String getCategory() { return category; }
    public int getPreparationMinutes() { return preparationMinutes; }
    public void setCategory(String category) { 
        CheckingExpression.checkNotNullOrEmpty(category, "Nhóm món không được để trống");
        this.category = category; }
    public void setPreparationMinutes(int preparationMinutes) { 
        CheckingExpression.checkNonNegativeInt(preparationMinutes, "Thời gian chuẩn bị không được âm");
        this.preparationMinutes = preparationMinutes; }
}