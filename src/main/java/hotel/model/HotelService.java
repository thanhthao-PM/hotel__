package hotel.model;

import hotel.util.FileUtil;
import hotel.util.CheckingExpression;   

// Lớp trừu tượng cha của các loại dịch vụ khách sạn
public abstract class HotelService {

    // Bộ đếm sinh mã tự động (static), nạp lại từ file khi khởi động
    private static int nextNumber = 1;

    private String serviceId;
    private String name;
    private double unitPrice;     // đơn giá hiện hành
    private String unit;          // đơn vị tính: phần, kg, giờ...
    private String description;
    private boolean available;    // false khi ngưng cung cấp

    // Constructor đầy đủ
    public HotelService(String serviceId, String name, double unitPrice,
                        String unit, String description, boolean available) {
        //kiểm tra dữ liệu đầu vào trước khi gán
        CheckingExpression.checkNotNullOrEmpty(serviceId, "Mã dịch vụ không được để trống");
        CheckingExpression.checkNotNullOrEmpty(name, "Tên dịch vụ không được để trống");
        CheckingExpression.checkNonNegativeDouble(unitPrice, "Đơn giá dịch vụ không được âm");
        CheckingExpression.checkNotNullOrEmpty(unit, "Đơn vị tính không được để trống");

        this.serviceId = serviceId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.unit = unit;
        this.description = description;
        this.available = available;
    }

    // Sinh mã dịch vụ mới dạng SV001, SV002...
    public static String generateId() {
        String id = String.format("SV%03d", nextNumber);
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

    // Tính tiền dịch vụ theo số lượng (mỗi loại dịch vụ tính khác nhau)
    public abstract double calculateCost(double quantity);

    // Chuyển thành một dòng để ghi file (mỗi loại tự ghi thêm thuộc tính riêng)
    public abstract String toFileString();

    // Ngưng cung cấp dịch vụ
    public void deactivate() {
        this.available = false;
    }

    // Cung cấp lại dịch vụ
    public void activate() {
        this.available = true;
    }

    // Thông tin chung để in ra bảng (lớp con ghi đè để thêm phần riêng)
    public String getServiceInfo() {
        return String.format("%-6s | %-22s | %,10.0f / %-6s | %-12s | %s",
                serviceId, name, unitPrice, unit,
                available ? "Đang cung cấp" : "Ngưng", description);
    }

    // Phần ghi file chung của mọi loại dịch vụ
    protected String baseFileString() {
        return FileUtil.join(serviceId, FileUtil.clean(name), unitPrice,
                FileUtil.clean(unit), FileUtil.clean(description), available);
    }

    // Getter
    public String getServiceId() { return serviceId; }
    public String getName() { return name; }
    public double getUnitPrice() { return unitPrice; }
    public String getUnit() { return unit; }
    public String getDescription() { return description; }
    public boolean isAvailable() { return available; }

    // Setter
    public void setName(String name) { 
        CheckingExpression.checkNotNullOrEmpty(name, "Tên dịch vụ không được để trống");
        this.name = name; }
    public void setUnitPrice(double unitPrice) { 
        CheckingExpression.checkNonNegativeDouble(unitPrice, "Đơn giá dịch vụ không được âm");
        this.unitPrice = unitPrice; }
    public void setUnit(String unit) { 
         CheckingExpression.checkNotNullOrEmpty(unit, "Đơn vị tính không được để trống");
        this.unit = unit; }
    public void setDescription(String description) { this.description = description; }
}