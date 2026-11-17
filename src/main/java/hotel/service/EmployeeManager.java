package hotel.service;

import hotel.model.Employee;
import hotel.storage.IFileHandler;
import hotel.util.CheckingExpression;
import hotel.util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EmployeeManager implements IManager<Employee>, IFileHandler{

    private static final String FILE_PATH = "data/employee.txt";
    private static final int INITIAL_CAPACITY = 10;

    private Employee[] items;
    private int count;

    public EmployeeManager() {
        this.items = new Employee[INITIAL_CAPACITY];
        this.count = 0;
    }

    private void grow() {
        Employee[] bigger = new Employee[this.items.length * 2];
        for (int i = 0; i < count; i++)
            bigger[i] = this.items[i];
        this.items = bigger;
    }

    @Override public boolean add(Employee e) {
        if (e == null) return false;
        if (findById(e.getId()) != null
                || findByIdentityNumber(e.getIdentityNumber()) != null
                || findByPhone(e.getPhone()).length > 0) return false;   // thêm kiểm tra trùng SĐT
        if (count == items.length) grow();
        items[count] = e;
        count++;
        return true;
    }

        @Override public boolean update(Employee e) {
        // Kiểm tra xem nhân viên có rỗng không
        if(e == null) return false;
        // Tìm nhân viên và update
        for (int i = 0; i < this.count; i++) {
            if (items[i].getId().equals(e.getId())) {
                // duyệt mọi nhân viên khác (bỏ qua vị trí i) để kiểm tra trùng CCCD và SĐT
                for (int j = 0; j < count; j++) {
                    if (j == i) continue;
                    String idNum = e.getIdentityNumber();
                    if (idNum != null && !idNum.isEmpty() && idNum.equals(items[j].getIdentityNumber())) {
                        return false;
                    }
                    if (e.getPhone() != null && e.getPhone().equals(items[j].getPhone())) {
                        return false;
                    }
                }
                items[i] = e;
                return true;
            }
        }
        // Không tìm được thì return false
        return false;
    }

    @Override public boolean remove(String id) {
        Employee e = findById(id);
        if (e == null) return false;
        e.deactivate(); // Xoá mềm
        return true;
    }

    @Override
    public ArrayList<Employee> getAll() {
        ArrayList<Employee> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(items[i]);
        }
        return list;
    }

    @Override public Employee findById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (int i = 0; i < count; i++) {
            if (items[i].getId().equals(id)) return items[i];
        }
        return null;
    }

    public Employee findByIdentityNumber(String idNumber) {
        if (idNumber == null || idNumber.isEmpty()) return null;
        for (int i = 0; i < this.count; i++) {
            if (items[i].getIdentityNumber().equals(idNumber)) return items[i];
        }
        return null;
    }

    public Employee[] findByName (String name) {
        if(name == null || name.isEmpty()) return null;
        String keyword = name.trim().toLowerCase();
        Employee[] eList = new Employee[count];
        int c = 0;
        for(int i = 0; i < this.count; i++) {
            if(items[i].getFullName().toLowerCase().contains(keyword)) eList[c++] = items[i];
        }
        return shrink(eList, c);
    }

    public Employee[] findByPhone (String phone) {
        if(phone == null || phone == "") return null;
        Employee[] eList = new Employee[count];
        int c = 0;
        for(int i = 0; i < count; i++) {
            if(items[i].getPhone().equals(phone)) eList[c++] = items[i];
        }
        return shrink(eList, c);
    }

    // Cắt mảng về đúng số phần tử có dữ liệu, để nơi gọi dùng .length là đúng
    private static Employee[] shrink(Employee[] arr, int size) {
        Employee[] exact = new Employee[size];
        for (int i = 0; i < size; i++) exact[i] = arr[i];
        return exact;
    }


    public int getCount() {return count;}

    public Employee getAt(int index) {
        CheckingExpression.checkValidArrayIndex(index, count);;
        return items[index];
    } 

    public void sortByName() {
        for (int i = 0; i < count - 1; i++) {
            int minIndex = i;                                   // giả sử ô i đang nhỏ nhất
            for (int j = i + 1; j < count; j++) {
                if (items[j].getFullName().compareToIgnoreCase(items[minIndex].getFullName()) < 0) {
                    minIndex = j;                               // tìm được ô nhỏ hơn thì ghi nhớ
                }
            }
            if (minIndex != i) {                                // đổi chỗ hai ô
                Employee temp = items[i];
                items[i] = items[minIndex];
                items[minIndex] = temp;
            }
        }
    }

    public void displayAll() {
        if (count == 0) {
            System.out.println("Danh sách nhân viên trống");
            return;
        }
        System.out.printf("%-8s %-25s %-12s %-18s %-10s%n",
                "Mã", "Họ tên", "SĐT", "Chức danh", "Trạng thái");
        for (int i = 0; i < count; i++) {
            Employee e = items[i];
            System.out.printf("%-8s %-25s %-12s %-18s %-10s%n",
                    e.getId(), e.getFullName(), e.getPhone(), e.getPosition(),
                    e.isActive() ? "Hoạt động" : "Ngưng");
        }
    }

    @Override public void readFile() {
        Path path = Paths.get(FILE_PATH);
        if (!Files.exists(path)) {
            return;     // lần chạy đầu chưa có file, đây là chuyện bình thường, không phải lỗi
        }

        items = new Employee[INITIAL_CAPACITY];      // bắt đầu từ mảng trống, tránh nạp trùng
        count = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {   // hết file thì readLine trả null
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;                              // bỏ qua dòng trống
                }
                try {
                    Employee e = Employee.fromFileString(line);
                    if (!add(e)) {
                        System.out.println("Dòng " + lineNumber + ": trùng mã hoặc CCCD, bỏ qua");
                    }
                } catch (RuntimeException ex) {
                    // Dòng hỏng: báo lỗi rồi đi tiếp dòng sau
                    System.out.println("Dòng " + lineNumber + " bị lỗi, bỏ qua: " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.out.println("Không đọc được file " + FILE_PATH + ": " + ex.getMessage());
        }
    }

    @Override public void writeFile() {
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
