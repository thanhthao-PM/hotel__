package hotel.service;

import hotel.model.Account;
import hotel.model.Employee;
import hotel.model.enums.Gender;
import hotel.model.enums.Role;
import hotel.storage.IFileHandler;
import hotel.util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import hotel.util.CheckingExpression;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AccountManager implements IManager<Account>, IFileHandler {
    private static final String FILE_PATH = "file/accounts.txt";
    private static final int INITIAL_CAPACITY = 10;
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";   // phải đủ độ dài tối thiểu
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Account[] items;
    private int count;

    private final EmployeeManager employeeManager;   // tham chiếu được truyền vào

    public AccountManager(EmployeeManager employeeManager) {
        if (employeeManager == null)
            throw new IllegalArgumentException("EmployeeManager không được null");
        this.employeeManager = employeeManager;
        this.items = new Account[INITIAL_CAPACITY];
        this.count = 0;
    }

    // Hàm nền duy nhất để tra username; các hàm khác dùng lại
    private int indexOf(String username) {
        if (username == null || username.trim().isEmpty()) return -1;
        String key = username.trim();
        for (int i = 0; i < count; i++) {
            if (items[i].getUsername().equalsIgnoreCase(key)) return i;
        }
        return -1;
    }

    @Override public Account findById(String username) {
        int i = indexOf(username);
        return (i < 0) ? null : items[i];
    }

    public Account findByEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) return null;
        for (int i = 0; i < count; i++) {
            if (items[i].getEmployeeId().equals(employeeId)) return items[i];
        }
        return null;
    }

    private void grow() {
        Account[] bigger = new Account[this.items.length * 2];
        for (int i = 0; i < count; i++)
            bigger[i] = this.items[i];
        this.items = bigger;
    }

    @Override public boolean add(Account a) {
        if (a == null) return false;
        if (indexOf(a.getUsername()) >= 0) return false;                  // trùng username
        if (findByEmployeeId(a.getEmployeeId()) != null) return false;    // 1 nhân viên - 1 account
        if (employeeManager.findById(a.getEmployeeId()) == null) return false;
        if (count == items.length) grow();
        items[count++] = a;
        return true;
    }

    @Override public boolean update(Account a) {
        if (a == null) return false;
        int i = indexOf(a.getUsername());
        if (i < 0) return false;
        items[i] = a;
        return true;
    }

    @Override public boolean remove(String username) {
        int i = indexOf(username);
        if (i < 0) return false;
        if (items[i].getRole() == Role.ADMIN && countByRole(Role.ADMIN) <= 1) return false;
        for (int j = i; j < count - 1; j++) items[j] = items[j + 1];
        items[--count] = null;
        return true;
    }

    public Account login(String username, String rawPassword) {
        // tên đăng nhập hoặc mật khẩu rỗng thì báo cùng thông báo chung, không lộ thông tin
        CheckingExpression.checkNotNullOrEmpty(username, "Sai tên đăng nhập hoặc mật khẩu.");
        CheckingExpression.checkNotNullOrEmpty(rawPassword, "Sai tên đăng nhập hoặc mật khẩu.");
        Account acc = findById(username);
        // Cùng một thông báo cho "sai tên" và "sai mật khẩu" để không lộ username nào tồn tại
        if (acc == null) throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        if (acc.isLocked()) throw new IllegalStateException("Tài khoản đã bị khóa.");

        Employee emp = employeeManager.findById(acc.getEmployeeId());
        if (emp == null || !emp.isActive())
            throw new IllegalStateException("Nhân viên không còn hoạt động.");

        if (!acc.verifyPassword(rawPassword)) {
            acc.recordFailedAttempt();            // đủ 3 lần sẽ tự khóa
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }
        acc.resetFailedAttempt();
        acc.updateLastLogin();
        return acc;   // nơi gọi kiểm tra isMustChangePassword()
    }

    public boolean lockAccount(String username) {
        Account acc = findById(username);
        if (acc == null) return false;
        // if (acc.getRole() == Role.ADMIN && countUnlockedAdmins() <= 1 && !acc.isLocked()) return false;
        acc.lock();
        return true;
    }

    public boolean unlockAccount(String username) {
        Account acc = findById(username);
        if (acc == null) return false;
        acc.unlock();
        acc.resetFailedAttempt();   // không reset thì lần sai kế tiếp khóa ngay
        return true;
    }

    // Dùng khi admin muốn reset lại mật khẩu cho user
    public boolean resetPassword(String username, String tempPassword) {
        Account acc = findById(username);
        if(acc == null) return false;
        acc.resetPassword(tempPassword);
        return true;
    }

    public boolean changeRole(String username, Role role) {
        Account acc = findById(username);
        if (acc == null || role == null) return false;
        if (acc.getRole() == Role.ADMIN && role != Role.ADMIN
                && countByRole(Role.ADMIN) <= 1) return false;   // không hạ Admin cuối
        acc.setRole(role);
        return true;
    }

    public int countByRole(Role role) {
        int c = 0;
        for (int i = 0; i < count; i++) {
            if (items[i].getRole() == role) c++;
        }
        return c;
    }

    public void ensureDefaultAdmin() {
        if(countByRole(Role.ADMIN) > 0) return;

        // 1. Tạo employee
        Employee admin = new Employee(
            "Quản trị hệ thống",                   // fullName
            "0900000000",                          // phone: phải qua checkValidPhone
            Gender.OTHER,
            "000000000001",                        // identityNumber: 12 chữ số
            LocalDate.of(1990, 1, 1),
            "Chưa cập nhật",
            true,
            "Quản trị viên",                       // position
            0,                                     // salary: không âm là hợp lệ
            LocalDate.now());                      // hireDate

        if (!employeeManager.add(admin)) {
            throw new IllegalStateException("Không tạo được nhân viên Admin mặc định (trùng SĐT hoặc CCCD?)");
        }

        // 2. Tạo account
        Account account = new Account(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD, Role.ADMIN, admin.getId());
        if(!add(account)) {
            throw new IllegalStateException("Không tạo được tài khoản Admin mặc định (trùng username?)");
        }
    }


    public void displayAll() {
        if (count == 0) {
            System.out.println("Danh sách tài khoản trống");
            return;
        }
        System.out.printf("%-15s %-10s %-8s %-12s %-5s %-18s%n",
                "Tên đăng nhập", "Quyền", "Mã NV", "Trạng thái", "Sai", "Đăng nhập gần nhất");
        for (int i = 0; i < count; i++) {
            Account a = items[i];
            String lastLogin = (a.getLastLogin() == null)
                    ? "Chưa đăng nhập"
                    : a.getLastLogin().format(TIME_FORMAT);
            System.out.printf("%-15s %-10s %-8s %-12s %-5d %-18s%n",
                    a.getUsername(), a.getRole(), a.getEmployeeId(),
                    a.isLocked() ? "Đã khóa" : "Hoạt động",
                    a.getFailedAttempts(), lastLogin);
        }
    }

    @Override
    public ArrayList<Account> getAll() {
        ArrayList<Account> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(items[i]);
        }
        return list;
    }

    @Override public void readFile() {
       Path path = Paths.get(FILE_PATH);
        if (!Files.exists(path)) {
            return;                                  // lần chạy đầu chưa có file, không phải lỗi
        }

        items = new Account[INITIAL_CAPACITY];       // bắt đầu từ mảng trống
        count = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;                        // bỏ qua dòng trống
                }
                try {
                    Account a = Account.fromFileString(line);
                    if (!add(a)) {
                        System.out.println("Dòng " + lineNumber
                                + ": trùng tên đăng nhập, nhân viên đã có tài khoản"
                                + " hoặc mã nhân viên không tồn tại, bỏ qua");
                    }
                } catch (RuntimeException ex) {
                    // Không in cả dòng dữ liệu vì nó chứa passwordHash
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
