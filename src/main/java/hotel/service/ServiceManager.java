package hotel.service;

import hotel.model.FoodService;
import hotel.model.HotelService;
import hotel.model.LaundryService;
import hotel.util.CheckingExpression;
import hotel.model.SpaService;
import hotel.storage.IFileHandler;
import hotel.util.FileUtil;
import hotel.util.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

// Quản lý danh sách dịch vụ khách sạn (CRUD, tìm kiếm, bảng giá, đọc/ghi file)
public class ServiceManager implements IManager<HotelService>, IFileHandler {

    // Tự động nhận diện đường dẫn file trong thư mục file\ (ưu tiên file/ hoặc src/main/java/hotel/file/)
    public static String resolvePath(String fileName) {
        if (Files.exists(Paths.get("file", fileName))) {
            return "file/" + fileName;
        }
        if (Files.exists(Paths.get("src/main/java/hotel/file", fileName))) {
            return "src/main/java/hotel/file/" + fileName;
        }
        return "src/main/java/hotel/file/" + fileName;
    }

    // Đường dẫn file dữ liệu
    public static final String FILE_PATH ="src/main/java/hotel/file/services.txt";

    // Mảng đối tượng (ArrayList) lưu dịch vụ
    private ArrayList<HotelService> services = new ArrayList<>();

    // ====================== CRUD DỊCH VỤ ======================

    // Thêm dịch vụ mới: không trùng mã, tên không rỗng, đơn giá phải dương
    @Override
    public boolean add(HotelService service) {
        if (service == null) {
            return false;
        }
        if (findById(service.getServiceId()) != null) {
            System.out.println("Lỗi: mã dịch vụ đã tồn tại.");
            return false;
        }
        if (!Validator.isNotBlank(service.getName()) || service.getUnitPrice() <= 0) {
            System.out.println("Lỗi: tên không được rỗng và đơn giá phải lớn hơn 0.");
            return false;
        }
        services.add(service);
        return true;
    }

    @Override
    public boolean remove(String id) {
        return delete(id);
    }

    // Xóa mềm dịch vụ = ngưng cung cấp (không xóa hẳn để giữ lịch sử hóa đơn)
    public boolean delete(String id) {
        HotelService s = findById(id);
        if (s == null) {
            return false;
        }
        s.deactivate();
        return true;
    }

    // Sửa dịch vụ: giữ nguyên loại dịch vụ
    @Override
    public boolean update(HotelService updated) {
        if (updated == null) {
            return false;
        }
        int index = indexOfService(updated.getServiceId());
        if (index < 0) {
            return false;
        }
        if (services.get(index).getClass() != updated.getClass()) {
            System.out.println("Lỗi: không được đổi loại dịch vụ.");
            return false;
        }
        if (updated.getUnitPrice() <= 0) {
            System.out.println("Lỗi: đơn giá phải lớn hơn 0.");
            return false;
        }
        services.set(index, updated);
        return true;
    }

    // Tìm dịch vụ theo mã (không phân biệt hoa thường), không thấy trả về null
    @Override
    public HotelService findById(String id) {
        int index = indexOfService(id);
        return index < 0 ? null : services.get(index);
    }

    // In toàn bộ dịch vụ (đa hình: mỗi loại tự in thông tin của mình)
    public void displayAll() {
        displayList(services);
    }

    // Lấy toàn bộ danh sách dịch vụ (cho ListMenu)
    @Override
    public ArrayList<HotelService> getAll() {
        return services;
    }

    // Tìm vị trí dịch vụ trong danh sách theo mã, không thấy trả về -1
    private int indexOfService(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).getServiceId().equalsIgnoreCase(id)) {
                return i;
            }
        }
        return -1;
    }

    // In một danh sách dịch vụ bất kỳ
    public void displayList(ArrayList<HotelService> list) {
        if (list.isEmpty()) {
            System.out.println("(Không có dịch vụ nào)");
            return;
        }
        for (HotelService s : list) {
            System.out.println(s.getServiceInfo());
        }
    }

    // ====================== TÌM KIẾM, SẮP XẾP ======================

    // Tìm gần đúng theo tên (chứa từ khóa, không phân biệt hoa thường)
    public ArrayList<HotelService> findByName(String keyword) {
        // keyword null gây NullPointerException ở keyword.toLowerCase()
        CheckingExpression.checkNotNullOrEmpty(keyword, "Từ khóa tìm kiếm không được để trống");
        ArrayList<HotelService> result = new ArrayList<>();
        String key = keyword.toLowerCase();
        for (HotelService s : services) {
            if (s.getName().toLowerCase().contains(key)) {
                result.add(s);
            }
        }
        return result;
    }

    // Tìm theo loại dịch vụ: "FOOD", "LAUNDRY" hoặc "SPA"
    public ArrayList<HotelService> findByType(String type) {
        // type null gây NullPointerException ở type.equalsIgnoreCase(...)
        CheckingExpression.checkNotNullOrEmpty(type, "Loại dịch vụ không được để trống (FOOD, LAUNDRY hoặc SPA)");
        ArrayList<HotelService> result = new ArrayList<>();
        for (HotelService s : services) {
            if ((type.equalsIgnoreCase("FOOD") && s instanceof FoodService)
                    || (type.equalsIgnoreCase("LAUNDRY") && s instanceof LaundryService)
                    || (type.equalsIgnoreCase("SPA") && s instanceof SpaService)) {
                result.add(s);
            }
        }
        return result;
    }

    // Lấy các dịch vụ đang cung cấp
    public ArrayList<HotelService> findAvailable() {
        ArrayList<HotelService> result = new ArrayList<>();
        for (HotelService s : services) {
            if (s.isAvailable()) {
                result.add(s);
            }
        }
        return result;
    }

    // Sắp xếp dịch vụ theo đơn giá (tự viết, chọn sắp xếp), trả về bản sao đã sắp xếp
    public ArrayList<HotelService> sortByPrice(boolean ascending) {
        ArrayList<HotelService> sorted = new ArrayList<>(services);
        for (int i = 0; i < sorted.size() - 1; i++) {
            int best = i;
            for (int j = i + 1; j < sorted.size(); j++) {
                double a = sorted.get(j).getUnitPrice();
                double b = sorted.get(best).getUnitPrice();
                if (ascending ? a < b : a > b) {
                    best = j;
                }
            }
            HotelService temp = sorted.get(i);
            sorted.set(i, sorted.get(best));
            sorted.set(best, temp);
        }
        return sorted;
    }

    // ====================== BẢNG GIÁ, NGƯNG CUNG CẤP ======================

    // Đổi đơn giá dịch vụ (chỉ áp cho lần dùng sau; ServiceUsage của P4 đã lưu giá lúc dùng)
    public boolean changePrice(String serviceId, double newPrice) {
        HotelService s = findById(serviceId);
        if (s == null || newPrice <= 0) {
            return false;
        }
        s.setUnitPrice(newPrice);
        return true;
    }

    // Ngưng cung cấp dịch vụ theo mã
    public boolean deactivateService(String serviceId) {
        HotelService s = findById(serviceId);
        if (s == null) {
            return false;
        }
        s.deactivate();
        return true;
    }

    // Cung cấp lại dịch vụ theo mã
    public boolean activateService(String serviceId) {
        HotelService s = findById(serviceId);
        if (s == null) {
            return false;
        }
        s.activate();
        return true;
    }

    // Lấy đơn giá hiện hành của dịch vụ (cung cấp cho P4), không có trả -1
    public double getCurrentPrice(String serviceId) {
        HotelService s = findById(serviceId);
        return s == null ? -1 : s.getUnitPrice();
    }

    // Tính tiền một dịch vụ theo số lượng (cung cấp cho P5, đa hình theo loại dịch vụ)
    public double calculateServiceCost(String serviceId, double quantity) {
        HotelService s = findById(serviceId);
        return s == null ? -1 : s.calculateCost(quantity);
    }

    // ====================== ĐỌC / GHI FILE ======================

    @Override
    public void readFile() {
        readFromFile();
    }

    @Override
    public void writeFile() {
        writeToFile();
    }

    // Đọc dịch vụ từ file; dòng lỗi thì báo và bỏ qua, không dừng chương trình
    public boolean readFromFile() {
        services.clear();
        try {
            for (String line : FileUtil.readLines(FILE_PATH)) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue; // Bỏ qua dòng trống và ghi chú
                }
                try {
                    HotelService s = parseService(line);
                    services.add(s);
                    HotelService.syncCounter(s.getServiceId());
                } catch (Exception e) {
                    System.out.println("Bỏ qua dòng dịch vụ lỗi: " + line + " (" + e.getMessage() + ")");
                }
            }
            return true;
        } catch (IOException e) {
            System.out.println("Lỗi đọc file dịch vụ: " + e.getMessage());
            return false;
        }
    }

    // Chuyển một dòng file thành đối tượng đúng loại (dùng HotelService.fromFileString)
    private HotelService parseService(String line) {
        return HotelService.fromFileString(line);
    }

    // Ghi dịch vụ xuống file
    public boolean writeToFile() {
        try {
            ArrayList<String> lines = new ArrayList<>();
            for (HotelService s : services) {
                lines.add(s.toFileString());
            }
            FileUtil.writeLines(FILE_PATH, lines);
            return true;
        } catch (IOException e) {
            System.out.println("Lỗi ghi file dịch vụ: " + e.getMessage());
            return false;
        }
    }
}