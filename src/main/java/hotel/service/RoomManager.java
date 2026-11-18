package hotel.service;

import hotel.model.Room;
import hotel.model.RoomMaintenance;
import hotel.model.StandardRoom;
import hotel.model.VipRoom;
import hotel.model.enums.RoomStatus;
import hotel.util.CheckingExpression;
import hotel.storage.IFileHandler;
import hotel.util.FileUtil;
import hotel.util.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;

// Quản lý danh sách phòng và bản ghi bảo trì (CRUD, tìm kiếm, đọc/ghi file)
public class RoomManager implements IManager<Room>, IFileHandler {

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
    public static final String FILE_PATH = resolvePath("rooms.txt");
    public static final String MAINTENANCE_FILE_PATH = resolvePath("room_maintenance.txt");

    // Mảng đối tượng (ArrayList) lưu phòng và bảo trì
    private ArrayList<Room> rooms = new ArrayList<>();
    private ArrayList<RoomMaintenance> maintenances = new ArrayList<>();

    // ====================== CRUD PHÒNG ======================

    // Thêm phòng mới: không trùng mã, không trùng số phòng, giá và sức chứa phải dương
    @Override
    public boolean add(Room room) {
    if (room == null) {
        return false;
    }

    if (findById(room.getRoomId()) != null) {
        return false;
    }

    if (findByRoomNumber(room.getRoomNumber()) != null) {
        return false;
    }

    if (room.getCapacity() <= 0 || room.getBasePrice() <= 0) {
        return false;
    }

    // Thêm phòng vào danh sách
    rooms.add(room);

    // Ghi lại toàn bộ danh sách xuống file
    writeToFile();

    return true;
}

    @Override
    public boolean remove(String id) {
        return delete(id);
    }

    // Xóa mềm phòng: chỉ đặt active = false; không xóa khi phòng đang có khách hoặc đã giữ chỗ
    public boolean delete(String id) {
        Room room = findById(id);
        if (room == null) {
            return false;
        }
        if (room.getStatus() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.BOOKED) {
            System.out.println("Lỗi: phòng đang có khách hoặc đã giữ chỗ, không thể xóa.");
            return false;
        }
        room.setActive(false);
        return true;
    }

    // Sửa phòng: giữ nguyên loại phòng và trạng thái (đổi trạng thái phải qua changeRoomStatus)
    @Override
    public boolean update(Room updated) {
        if (updated == null) {
            return false;
        }
        int index = indexOfRoom(updated.getRoomId());
        if (index < 0) {
            return false;
        }
        Room old = rooms.get(index);
        if (old.getClass() != updated.getClass()) {
            System.out.println("Lỗi: không được đổi loại phòng.");
            return false;
        }
        if (old.getStatus() != updated.getStatus()) {
            System.out.println("Lỗi: đổi trạng thái phải dùng changeRoomStatus().");
            return false;
        }
        Room sameNumber = findByRoomNumber(updated.getRoomNumber());
        if (sameNumber != null && !sameNumber.getRoomId().equals(updated.getRoomId())) {
            System.out.println("Lỗi: số phòng bị trùng với phòng khác.");
            return false;
        }
        rooms.set(index, updated);
        return true;
    }

    // Tìm phòng theo mã (không phân biệt hoa thường), không thấy trả về null
    @Override
    public Room findById(String id) {
        int index = indexOfRoom(id);
        return index < 0 ? null : rooms.get(index);
    }

    // In toàn bộ danh sách phòng (đa hình: mỗi loại phòng tự in thông tin của mình)
    public void displayAll() {
        displayList(rooms);
    }

    // Lấy toàn bộ danh sách phòng (cho ListMenu)
    @Override
    public ArrayList<Room> getAll() {
        return rooms;
    }

    // Tìm vị trí của phòng trong danh sách theo mã, không thấy trả về -1
    private int indexOfRoom(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getRoomId().equalsIgnoreCase(id)) {
                return i;
            }
        }
        return -1;
    }

    // In một danh sách phòng bất kỳ, kèm giá một đêm tính theo từng loại (đa hình)
    public void displayList(ArrayList<Room> list) {
        if (list.isEmpty()) {
            System.out.println("(Không có phòng nào)");
            return;
        }
        for (Room r : list) {
            System.out.println(r.getRoomInfo()
                    + String.format(" | Giá/đêm: %,.0f", r.calculateRoomPrice(1)));
        }
    }

    // ====================== TÌM KIẾM, SẮP XẾP ======================

    // Tìm phòng theo số phòng (chính xác), không thấy trả về null
    public Room findByRoomNumber(String roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return r;
            }
        }
        return null;
    }

    // Tìm theo loại phòng: "VIP" hoặc "STANDARD"
    public ArrayList<Room> findByType(String type) {
        // type null sẽ gây NullPointerException ở type.equalsIgnoreCase(...)
        CheckingExpression.checkNotNullOrEmpty(type, "Loại phòng không được để trống (VIP hoặc STANDARD)");
        ArrayList<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            boolean isVip = r instanceof VipRoom;
            if ((type.equalsIgnoreCase("VIP") && isVip)
                    || (type.equalsIgnoreCase("STANDARD") && !isVip)) {
                result.add(r);
            }
        }
        return result;
    }

    // Tìm theo trạng thái phòng
    public ArrayList<Room> findByStatus(RoomStatus status) {
        ArrayList<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getStatus() == status) {
                result.add(r);
            }
        }
        return result;
    }

    // Tìm theo khoảng giá gốc một đêm [minPrice, maxPrice]
    public ArrayList<Room> findByPriceRange(double minPrice, double maxPrice) {
        // giá không âm và giá tối thiểu không lớn hơn giá tối đa
        CheckingExpression.checkNonNegativeDouble(minPrice, "Giá tối thiểu không được âm");
        if (maxPrice < minPrice) {
            throw new IllegalArgumentException("Giá tối đa không được nhỏ hơn giá tối thiểu");
        }
        ArrayList<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getBasePrice() >= minPrice && r.getBasePrice() <= maxPrice) {
                result.add(r);
            }
        }
        return result;
    }

    // Sắp xếp phòng theo giá gốc (tự viết, chọn sắp xếp chọn), trả về bản sao đã sắp xếp
    public ArrayList<Room> sortByPrice(boolean ascending) {
        ArrayList<Room> sorted = new ArrayList<>(rooms);
        for (int i = 0; i < sorted.size() - 1; i++) {
            int best = i;
            for (int j = i + 1; j < sorted.size(); j++) {
                double a = sorted.get(j).getBasePrice();
                double b = sorted.get(best).getBasePrice();
                if (ascending ? a < b : a > b) {
                    best = j;
                }
            }
            Room temp = sorted.get(i);
            sorted.set(i, sorted.get(best));
            sorted.set(best, temp);
        }
        return sorted;
    }

    // ====================== PHÒNG TRỐNG, TRẠNG THÁI, GIÁ ======================

    // Kiểm tra một phòng dùng được trong [from, to): còn hoạt động và không có bảo trì giao khoảng ngày
    // Lưu ý: việc trùng đơn đặt phòng do P3 kiểm tra thêm
    public boolean isRoomAvailable(String roomId, LocalDate from, LocalDate to) {
        Room room = findById(roomId);
        if (room == null || !room.isActive() || !Validator.isValidDateRange(from, to)) {
            return false;
        }
        return !hasMaintenanceInRange(roomId, from, to);
    }

    // Tìm danh sách phòng dùng được trong khoảng ngày [from, to) (cung cấp cho P3)
    public ArrayList<Room> findAvailableRooms(LocalDate from, LocalDate to) {
        ArrayList<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (isRoomAvailable(r.getRoomId(), from, to)) {
                result.add(r);
            }
        }
        return result;
    }

    // Đổi trạng thái phòng (cổng duy nhất để module khác đổi trạng thái), sai quy tắc thì báo lỗi
    public boolean changeRoomStatus(String roomId, RoomStatus newStatus) {
        Room room = findById(roomId);
        if (room == null) {
            System.out.println("Lỗi: không tìm thấy phòng " + roomId);
            return false;
        }
        boolean ok = room.changeStatus(newStatus);
        if (!ok) {
            System.out.println("Lỗi: không thể chuyển phòng " + roomId + " từ "
                    + room.getStatus() + " sang " + newStatus);
        }
        return ok;
    }

    // Đổi giá gốc của phòng (giá mới phải dương)
    public boolean changePrice(String roomId, double newPrice) {
        Room room = findById(roomId);
        if (room == null || newPrice <= 0) {
            return false;
        }
        room.setBasePrice(newPrice);
        return true;
    }

    // Lấy tiền phòng theo số đêm (cung cấp cho P5), phòng không tồn tại trả -1
    public double getRoomPrice(String roomId, int nights) {
        Room room = findById(roomId);
        return room == null ? -1 : room.calculateRoomPrice(nights);
    }

    // ====================== BẢO TRÌ PHÒNG ======================

    // Tạo bản ghi bảo trì; nếu hiệu lực từ hôm nay thì chuyển phòng sang MAINTENANCE
    public boolean createMaintenance(String roomId, LocalDate start, LocalDate end,
                                     String reason, String reportedBy) {
        Room room = findById(roomId);
        if (room == null) {
            System.out.println("Lỗi: không tìm thấy phòng " + roomId);
            return false;
        }
        RoomMaintenance m = new RoomMaintenance(RoomMaintenance.generateId(), roomId,
                start, end, reason, reportedBy, false);
        if (!m.validatePeriod()) {
            System.out.println("Lỗi: khoảng ngày bảo trì không hợp lệ.");
            return false;
        }
        if (m.isEffectiveOn(LocalDate.now())) {
            if (room.getStatus() == RoomStatus.OCCUPIED) {
                System.out.println("Lỗi: phòng đang có khách, không thể bảo trì ngay.");
                return false;
            }
            if (room.getStatus() != RoomStatus.MAINTENANCE
                    && !room.changeStatus(RoomStatus.MAINTENANCE)) {
                System.out.println("Lỗi: không thể chuyển phòng sang bảo trì.");
                return false;
            }
        }
        maintenances.add(m);
        return true;
    }

    // Đóng bản ghi bảo trì; nếu phòng không còn bảo trì nào hiệu lực thì trả về AVAILABLE
    public boolean closeMaintenance(String maintenanceId) {
        for (RoomMaintenance m : maintenances) {
            if (m.getMaintenanceId().equalsIgnoreCase(maintenanceId)) {
                if (m.isClosed()) {
                    System.out.println("Lỗi: bản ghi này đã đóng.");
                    return false;
                }
                m.closeMaintenance();
                Room room = findById(m.getRoomId());
                if (room != null && room.getStatus() == RoomStatus.MAINTENANCE
                        && !hasMaintenanceInRange(room.getRoomId(), LocalDate.now(),
                        LocalDate.now().plusDays(1))) {
                    room.changeStatus(RoomStatus.AVAILABLE);
                }
                return true;
            }
        }
        System.out.println("Lỗi: không tìm thấy bản ghi bảo trì " + maintenanceId);
        return false;
    }

    // Kiểm tra phòng có bảo trì (chưa đóng) giao với khoảng ngày [from, to)
    public boolean hasMaintenanceInRange(String roomId, LocalDate from, LocalDate to) {
        // chặn null và khoảng ngày sai (trước đó from/to null gây NullPointerException ở isBefore)
        CheckingExpression.checkValidDateRange(from, to);
        for (RoomMaintenance m : maintenances) {
            if (m.getRoomId().equalsIgnoreCase(roomId) && !m.isClosed()
                    && m.getStartDate().isBefore(to) && !m.getEndDate().isBefore(from)) {
                return true;
            }
        }
        return false;
    }

    // Lấy các bản ghi bảo trì của một phòng
    public ArrayList<RoomMaintenance> findMaintenanceByRoom(String roomId) {
        ArrayList<RoomMaintenance> result = new ArrayList<>();
        for (RoomMaintenance m : maintenances) {
            if (m.getRoomId().equalsIgnoreCase(roomId)) {
                result.add(m);
            }
        }
        return result;
    }

    // In toàn bộ bản ghi bảo trì
    public void displayMaintenances() {
        if (maintenances.isEmpty()) {
            System.out.println("(Chưa có bản ghi bảo trì nào)");
            return;
        }
        for (RoomMaintenance m : maintenances) {
            System.out.println(m.getInfo());
        }
    }

    // Lấy toàn bộ bản ghi bảo trì
    public ArrayList<RoomMaintenance> getAllMaintenances() {
        return maintenances;
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

    // Đọc phòng và bảo trì từ file; dòng lỗi thì báo và bỏ qua, không dừng chương trình
    public boolean readFromFile() {
        rooms.clear();
        maintenances.clear();
        try {
            // Đọc phòng trước
            for (String line : FileUtil.readLines(FILE_PATH)) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue; // Bỏ qua dòng trống và ghi chú
                }
                try {
                    Room r = parseRoom(line);
                    rooms.add(r);
                    Room.syncCounter(r.getRoomId());
                } catch (Exception e) {
                    System.out.println("Bỏ qua dòng phòng lỗi: " + line + " (" + e.getMessage() + ")");
                }
            }
            // Đọc bảo trì sau, vì cần phòng đã tồn tại
            for (String line : FileUtil.readLines(MAINTENANCE_FILE_PATH)) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue; // Bỏ qua dòng trống và ghi chú
                }
                try {
                    RoomMaintenance m = RoomMaintenance.fromFileString(line);
                    if (findById(m.getRoomId()) == null) {
                        System.out.println("Bỏ qua bảo trì tham chiếu phòng không tồn tại: " + line);
                        continue;
                    }
                    maintenances.add(m);
                    RoomMaintenance.syncCounter(m.getMaintenanceId());
                } catch (Exception e) {
                    System.out.println("Bỏ qua dòng bảo trì lỗi: " + line + " (" + e.getMessage() + ")");
                }
            }
            return true;
        } catch (IOException e) {
            System.out.println("Lỗi đọc file phòng: " + e.getMessage());
            return false;
        }
    }

    // Chuyển một dòng file thành đối tượng Room đúng loại (dùng Room.fromFileString)
    private Room parseRoom(String line) {
        return Room.fromFileString(line);
    }

    // Ghi phòng và bảo trì xuống file
    public boolean writeToFile() {
        try {
            ArrayList<String> roomLines = new ArrayList<>();
            for (Room r : rooms) {
                roomLines.add(r.toFileString());
            }
            FileUtil.writeLines(FILE_PATH, roomLines);

            ArrayList<String> maintenanceLines = new ArrayList<>();
            for (RoomMaintenance m : maintenances) {
                maintenanceLines.add(m.toFileString());
            }
            FileUtil.writeLines(MAINTENANCE_FILE_PATH, maintenanceLines);
            return true;
        } catch (IOException e) {
            System.out.println("Lỗi ghi file phòng: " + e.getMessage());
            return false;
        }
    }
}