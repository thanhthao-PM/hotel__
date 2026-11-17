package hotel.service;

import hotel.model.Customer;
import hotel.model.GuestMember;
import hotel.model.Person;
import hotel.storage.IFileHandler;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CustomerManager implements IManager<Person>, IFileHandler {

    private static final String FILE_PATH = "data/customer.txt";
    private static final int INITIAL_CAPACITY = 10;

    private Person[] items;
    private int count;

    public CustomerManager() {
        this.items = new Person[INITIAL_CAPACITY];
        this.count = 0;
    }

    private void grow() {
        Person[] bigger = new Person[items.length * 2];
        for (int i = 0; i < count; i++) bigger[i] = items[i];
        items = bigger;
    }

    // ================= THÊM / SỬA / XÓA =================

    @Override
    public boolean add(Person p) {
        return insert(p, true);
    }

    // checkRepresentative = false chỉ dùng lúc nạp file (xem giải thích bên dưới)
    private boolean insert(Person p, boolean checkRepresentative) {
        if (p == null) return false;
        if (!(p instanceof Customer) && !(p instanceof GuestMember)) return false;   // chặn Employee lọt vào
        if (isDuplicate(p)) return false;

        if (checkRepresentative && p instanceof GuestMember) {
            GuestMember g = (GuestMember) p;
            if (findCustomerById(g.getRepresentativeId()) == null) return false;     // người đại diện phải tồn tại
        }

        if (count == items.length) grow();
        items[count++] = p;
        return true;
    }

    private boolean isDuplicate(Person p) {
        if (findById(p.getId()) != null) return true;
        if (findByIdentityNumber(p.getIdentityNumber()) != null) return true;        // rỗng thì hàm này trả null
        // Thành viên đoàn có thể dùng SĐT của người đại diện, nên chỉ kiểm tra SĐT với Customer
        if (p instanceof Customer && findByPhone(p.getPhone()).length > 0) return true;
        return false;
    }

    @Override
    public boolean update(Person p) {
        if (p == null) return false;
        // chặn Employee lọt vào qua update (giống insert)
        if (!(p instanceof Customer) && !(p instanceof GuestMember)) return false;
        for (int i = 0; i < count; i++) {
            if (items[i].getId().equals(p.getId())) {
                // duyệt mọi người khác (bỏ qua vị trí i) để kiểm tra trùng CCCD và SĐT
                for (int j = 0; j < count; j++) {
                    if (j == i) continue;
                    String idNum = p.getIdentityNumber();
                    if (idNum != null && !idNum.isEmpty() && idNum.equals(items[j].getIdentityNumber())) {
                        return false;
                    }
                    // SĐT chỉ cần khác nhau giữa các Customer
                    if (p instanceof Customer && items[j] instanceof Customer
                            && p.getPhone().equals(items[j].getPhone())) {
                        return false;
                    }
                }
                items[i] = p;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(String id) {
        Person p = findById(id);
        if (p == null) return false;
        p.deactivate();           // xóa mềm
        return true;
    }

    // ================= TÌM KIẾM =================

    @Override
    public ArrayList<Person> getAll() {
        ArrayList<Person> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(items[i]);
        }
        return list;
    }

    @Override
    public Person findById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (int i = 0; i < count; i++) {
            if (items[i].getId().equals(id)) return items[i];
        }
        return null;
    }

    // Trả đúng kiểu Customer để P3 khỏi ép kiểu; không phải Customer thì trả null
    public Customer findCustomerById(String id) {
        Person p = findById(id);
        return (p instanceof Customer) ? (Customer) p : null;
    }

    public GuestMember[] findGuestsByRepresentative(String representativeId) {
        if (representativeId == null || representativeId.isEmpty()) return new GuestMember[0];
        GuestMember[] result = new GuestMember[count];
        int c = 0;
        for (int i = 0; i < count; i++) {
            if (items[i] instanceof GuestMember) {
                GuestMember g = (GuestMember) items[i];
                if (representativeId.equals(g.getRepresentativeId())) result[c++] = g;
            }
        }
        GuestMember[] exact = new GuestMember[c];
        for (int i = 0; i < c; i++) exact[i] = result[i];
        return exact;
    }

    // Tìm theo một phần của tên, không phân biệt hoa thường
    public Person[] findByName(String name) {
        if (name == null || name.trim().isEmpty()) return new Person[0];
        String keyword = name.trim().toLowerCase();
        Person[] result = new Person[count];
        int c = 0;
        for (int i = 0; i < count; i++) {
            if (items[i].getFullName().toLowerCase().contains(keyword)) result[c++] = items[i];
        }
        return shrink(result, c);
    }

    // SĐT khớp chính xác
    public Person[] findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return new Person[0];
        Person[] result = new Person[count];
        int c = 0;
        for (int i = 0; i < count; i++) {
            if (items[i].getPhone().equals(phone)) result[c++] = items[i];
        }
        return shrink(result, c);
    }

    public Person findByIdentityNumber(String idNumber) {
        if (idNumber == null || idNumber.isEmpty()) return null;   // hai bé cùng để trống không tính là trùng
        for (int i = 0; i < count; i++) {
            if (idNumber.equals(items[i].getIdentityNumber())) return items[i];
        }
        return null;
    }
    
    private static Person[] shrink(Person[] arr, int size) {
        Person[] exact = new Person[size];
        for (int i = 0; i < size; i++) exact[i] = arr[i];
        return exact;
    }

    // ================= SẮP XẾP, TRUY CẬP, IN =================

    public void sortByName() {
        for (int i = 0; i < count - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < count; j++) {
                if (items[j].getFullName().compareToIgnoreCase(items[minIndex].getFullName()) < 0) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                Person temp = items[i];
                items[i] = items[minIndex];
                items[minIndex] = temp;
            }
        }
    }

    public int getCount() {return count;}

    public Person getAt(int index) {
        CheckingExpression.checkValidArrayIndex(index, count);
        return items[index];
    }

    public void displayAll() {
        if (count == 0) {
            System.out.println("Danh sách khách hàng trống");
            return;
        }
        System.out.printf("%-8s %-25s %-12s %-18s %-10s%n", "Mã", "Họ tên", "SĐT", "Loại", "Trạng thái");
        for (int i = 0; i < count; i++) {
            Person p = items[i];
            System.out.printf("%-8s %-25s %-12s %-18s %-10s%n",
                    p.getId(), p.getFullName(), p.getPhone(), p.getRoleName(),   // đa hình: mỗi lớp tự trả tên loại
                    p.isActive() ? "Hoạt động" : "Ngưng");
        }
    }

    // ================= ĐỌC / GHI FILE =================

    @Override
    public void readFile() {
        Path path = Paths.get(FILE_PATH);
        if (!Files.exists(path)) return;

        items = new Person[INITIAL_CAPACITY];
        count = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                try {
                    Person p = parseLine(line);
                    if (!insert(p, false)) {      // false: không kiểm tra người đại diện lúc nạp
                        System.out.println("Dòng " + lineNumber + ": trùng mã, CCCD hoặc SĐT, bỏ qua");
                    }
                } catch (RuntimeException ex) {
                    System.out.println("Dòng " + lineNumber + " bị lỗi, bỏ qua: " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.out.println("Không đọc được file " + FILE_PATH + ": " + ex.getMessage());
        }

        // Nạp xong mới kiểm tra: thành viên nào mất người đại diện thì cảnh báo
        for (int i = 0; i < count; i++) {
            if (items[i] instanceof GuestMember) {
                GuestMember g = (GuestMember) items[i];
                if (findCustomerById(g.getRepresentativeId()) == null) {
                    System.out.println("Cảnh báo: " + g.getId() + " có người đại diện "
                            + g.getRepresentativeId() + " không tồn tại");
                }
            }
        }
    }

    // Chọn Customer hay GuestMember theo nhãn ở đầu dòng
    private Person parseLine(String line) {
        if (line.startsWith(Customer.TYPE_TAG + "|")) return Customer.fromFileString(line);
        if (line.startsWith(GuestMember.TYPE_TAG + "|")) return GuestMember.fromFileString(line);
        throw new IllegalArgumentException("Không nhận ra loại dữ liệu ở đầu dòng");
    }

    @Override
    public void writeFile() {
        String[] lines = new String[count];
        for (int i = 0; i < count; i++) {
            lines[i] = items[i].toFileString();
        }
        try {
            FileUtil.writeAllLines(FILE_PATH, lines, count);
        } catch (IOException ex) {
            System.out.println("Không ghi được file " + FILE_PATH + ": " + ex.getMessage());
        }
    }
}
