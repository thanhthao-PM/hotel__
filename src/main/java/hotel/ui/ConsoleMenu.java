package hotel.ui;
 
import hotel.model.Account;
import hotel.model.Customer;
import hotel.model.Employee;
import hotel.model.Person;
import hotel.model.enums.Gender;
import hotel.model.enums.Role;
import hotel.service.AccountManager;
import hotel.service.CustomerManager;
import hotel.service.EmployeeManager;
import hotel.util.CheckingExpression;
 
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
 
/**
 * TOÀN BỘ MENU nằm trong file này. Mỗi menu là một hàm showXxxMenu().
 *
 * Cách đọc file:
 *   run()            -> vòng lặp chính: đăng nhập -> menu theo Role -> đăng xuất
 *   showAdminMenu / showManagerMenu / showEmployeeMenu   -> menu chính của từng Role (có switch-case)
 *   Phần "P1"        -> menu con của module Người, Tài khoản (đã làm)
 *   Phần "KHUNG P2..P5" -> menu con của các bạn khác: chỉ cần thay notReady(...) bằng lệnh gọi Manager của mình
 *   Cuối file        -> các hàm nhập liệu dùng chung
 */
public class ConsoleMenu {
 
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
 
    private final Scanner sc = new Scanner(System.in);   // CHỈ MỘT Scanner cho cả chương trình
 
    private final AccountManager accountManager;
    private final EmployeeManager employeeManager;
    private final CustomerManager customerManager;
    // TODO(P2..P5): thêm RoomManager, BookingManager... vào đây và vào constructor
 
    private Account currentUser;                         // người đang đăng nhập (null = chưa đăng nhập)
 
    public ConsoleMenu(AccountManager accountManager,
                       EmployeeManager employeeManager,
                       CustomerManager customerManager) {
        this.accountManager = accountManager;
        this.employeeManager = employeeManager;
        this.customerManager = customerManager;
    }
 
    // ============================================================
    // VÒNG CHẠY CHÍNH
    // ============================================================
    public void run() {
        while (true) {
            currentUser = login();
            if (currentUser == null) {
                System.out.println("Tạm biệt!");
                return;
            }
 
            switch (currentUser.getRole()) {
                case ADMIN:    showAdminMenu();    break;
                case MANAGER:  showManagerMenu();  break;
                default:       showEmployeeMenu(); break;
            }
 
            saveAll();            // đăng xuất thì lưu dữ liệu
            currentUser = null;
        }
    }
 
    // Trả về Account đăng nhập thành công, hoặc null nếu người dùng chọn thoát
    private Account login() {
        printTitle("ĐĂNG NHẬP");
        while (true) {
            System.out.print("Tên đăng nhập (0 để thoát): ");
            String username = sc.nextLine().trim();
            if (username.equals("0")) return null;
 
            System.out.print("Mật khẩu: ");
            String password = sc.nextLine();
 
            try {
                Account acc = accountManager.login(username, password);
                if (acc.isMustChangePassword()) {
                    System.out.println("Đây là lần đăng nhập đầu tiên, bạn phải đổi mật khẩu.");
                    changePassword(acc, false);   // false = không cho hủy
                }
                return acc;
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private void saveAll() {
        employeeManager.writeFile();
        accountManager.writeFile();
        customerManager.writeFile();
    }
 
    // ============================================================
    // MENU CHÍNH THEO ROLE
    // ============================================================
    private void showAdminMenu() {
        while (true) {
            printTitle("ADMIN: " + currentUser.getUsername());
            System.out.println("1. Quản lý tài khoản");
            System.out.println("2. Quản lý nhân viên");
            System.out.println("3. Danh mục khách sạn (phòng, dịch vụ, bảo trì)");
            System.out.println("4. Nghiệp vụ (đặt phòng, lưu trú, thanh toán, khách hàng)");
            System.out.println("5. Thống kê và báo cáo");
            System.out.println("6. Hệ thống");
            System.out.println("7. Tài khoản của tôi");
            System.out.println("0. Đăng xuất");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: showAccountMenu();   break;
                    case 2: showStaffMenu();     break;
                    case 3: showCatalogMenu();   break;
                    case 4: showBusinessMenu();  break;
                    case 5: showReportMenu();    break;
                    case 6: showSystemMenu();    break;
                    case 7: showMyAccountMenu(); break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {   // lưới an toàn: lỗi một chức năng không làm sập cả chương trình
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private void showManagerMenu() {
        while (true) {
            printTitle("QUẢN LÝ: " + currentUser.getUsername());
            System.out.println("1. Danh sách");
            System.out.println("2. Đặt phòng và lưu trú");
            System.out.println("3. Thanh toán");
            System.out.println("4. Duyệt và xử lý");
            System.out.println("5. Danh mục (phòng, dịch vụ, bảo trì)");
            System.out.println("6. Nhân viên");
            System.out.println("7. Thống kê");
            System.out.println("8. Hệ thống");
            System.out.println("0. Đăng xuất");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: showListMenu();            break;
                    case 2: showBookingAndStayMenu();  break;
                    case 3: showPaymentMenu();         break;
                    case 4: showApprovalMenu();        break;
                    case 5: showCatalogMenu();         break;
                    case 6: showStaffMenu();           break;
                    case 7: showReportMenu();          break;
                    case 8: showSystemMenu();          break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // Sắp theo thứ tự công việc một ca trực: đặt phòng, nhận phòng, dịch vụ, trả phòng, thanh toán
    private void showEmployeeMenu() {
        while (true) {
            printTitle("NHÂN VIÊN: " + currentUser.getUsername());
            System.out.println("1. Đặt phòng");
            System.out.println("2. Lưu trú");
            System.out.println("3. Thanh toán");
            System.out.println("4. Khách hàng");
            System.out.println("5. Danh sách");
            System.out.println("6. Cá nhân");
            System.out.println("0. Đăng xuất");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: showBookingMenu();   break;
                    case 2: showStayMenu();      break;
                    case 3: showPaymentMenu();   break;
                    case 4: showCustomerMenu();  break;
                    case 5: showListMenu();      break;
                    case 6: showSystemMenu();    break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ============================================================
    // MENU GOM NHÓM (chỉ chọn module rồi gọi menu con của module đó)
    // ============================================================
    private void showCatalogMenu() {
        while (true) {
            printTitle("DANH MỤC KHÁCH SẠN");
            System.out.println("1. Phòng");
            System.out.println("2. Dịch vụ");
            System.out.println("3. Bảo trì phòng");
            System.out.println("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            switch (choice) {
                case 1: showRoomMenu();         break;
                case 2: showServiceMenu();      break;
                case 3: showMaintenanceMenu();  break;
                default: System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }
 
    private void showBusinessMenu() {
        while (true) {
            printTitle("NGHIỆP VỤ");
            System.out.println("1. Đặt phòng");
            System.out.println("2. Lưu trú");
            System.out.println("3. Thanh toán");
            System.out.println("4. Duyệt yêu cầu thay đổi đặt phòng");
            System.out.println("5. Khách hàng");
            System.out.println("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            switch (choice) {
                case 1: showBookingMenu();   break;
                case 2: showStayMenu();      break;
                case 3: showPaymentMenu();   break;
                case 4: showRequestMenu();   break;
                case 5: showCustomerMenu();  break;
                default: System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }
 
    private void showBookingAndStayMenu() {
        while (true) {
            printTitle("ĐẶT PHÒNG VÀ LƯU TRÚ");
            System.out.println("1. Đặt phòng");
            System.out.println("2. Lưu trú (nhận phòng, dịch vụ, đổi phòng, trả phòng)");
            System.out.println("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            switch (choice) {
                case 1: showBookingMenu(); break;
                case 2: showStayMenu();    break;
                default: System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }
 
    private void showApprovalMenu() {
        while (true) {
            printTitle("DUYỆT VÀ XỬ LÝ");
            System.out.println("1. Duyệt yêu cầu thay đổi đặt phòng");
            System.out.println("2. Duyệt giảm giá");
            System.out.println("3. Hủy hóa đơn");
            System.out.println("4. Hoàn tiền");
            System.out.println("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            switch (choice) {
                case 1: showRequestMenu();                    break;
                case 2: notReady("Duyệt giảm giá (P5)");      break;   // TODO(P5)
                case 3: notReady("Hủy hóa đơn (P5)");         break;   // TODO(P5)
                case 4: notReady("Hoàn tiền (P5)");           break;   // TODO(P5)
                default: System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }
 
    // ============================================================
    // DANH SÁCH (dùng chung cho mọi Role)
    // ============================================================
    private void showListMenu() {
        while (true) {
            printTitle("DANH SÁCH");
            System.out.println("1. Phòng");
            System.out.println("2. Khách hàng");
            if (!isEmployeeRole()) {
                System.out.println("3. Nhân viên");
            }
            System.out.println("4. Đặt phòng");
            System.out.println("5. Khách đang lưu trú");
            System.out.println("6. Hóa đơn");
            System.out.println("7. Dịch vụ");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Danh sách phòng (P2)");        break;   // TODO(P2): roomManager.displayAll()
                    case 2: showCustomerMenu();                       break;
                    case 3:
                        if (isEmployeeRole()) noPermission();          // kiểm tra quyền ngay trong xử lý, không chỉ ẩn menu
                        else showStaffMenu();
                        break;
                    case 4: notReady("Danh sách đặt phòng (P3)");    break;   // TODO(P3)
                    case 5: notReady("Khách đang lưu trú (P4)");     break;   // TODO(P4)
                    case 6: notReady("Danh sách hóa đơn (P5)");      break;   // TODO(P5)
                    case 7: notReady("Danh sách dịch vụ (P2)");      break;   // TODO(P2)
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ============================================================
    // P1: TÀI KHOẢN (chỉ ADMIN)
    // ============================================================
    private void showAccountMenu() {
        if (currentUser.getRole() != Role.ADMIN) {
            noPermission();
            return;
        }
        while (true) {
            printTitle("QUẢN LÝ TÀI KHOẢN");
            System.out.println("1. Danh sách tài khoản");
            System.out.println("2. Tạo tài khoản");
            System.out.println("3. Khóa / mở khóa");
            System.out.println("4. Đặt lại mật khẩu");
            System.out.println("5. Đổi quyền");
            System.out.println("6. Xóa tài khoản");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: accountManager.displayAll(); break;
                    case 2: createAccount();             break;
                    case 3: toggleLock();                break;
                    case 4: resetPassword();             break;
                    case 5: changeRole();                break;
                    case 6: removeAccount();             break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private void createAccount() {
        String employeeId = readNonEmpty("Mã nhân viên (VD NV001): ").toUpperCase();
        String username = readNonEmpty("Tên đăng nhập: ");
        Role role = readRole();
 
        // Constructor của Account tự kiểm tra mật khẩu; sai thì hỏi lại
        Account acc;
        while (true) {
            System.out.print("Mật khẩu tạm (tối thiểu 8 ký tự): ");
            String password = sc.nextLine();
            try {
                acc = new Account(username, password, role, employeeId);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
 
        if (accountManager.add(acc)) {
            System.out.println("Đã tạo tài khoản. Người dùng phải đổi mật khẩu ở lần đăng nhập đầu.");
        } else {
            System.out.println("Không tạo được: trùng tên đăng nhập, nhân viên đã có tài khoản, hoặc mã nhân viên không tồn tại.");
        }
    }
 
    private void toggleLock() {
        String username = readNonEmpty("Tên đăng nhập: ");
        if (username.equalsIgnoreCase(currentUser.getUsername())) {
            System.out.println("Không thể tự khóa chính mình.");
            return;
        }
        Account acc = accountManager.findById(username);
        if (acc == null) {
            System.out.println("Không tìm thấy tài khoản.");
        } else if (acc.isLocked()) {
            accountManager.unlockAccount(username);
            System.out.println("Đã mở khóa " + username + ".");
        } else {
            accountManager.lockAccount(username);
            System.out.println("Đã khóa " + username + ".");
        }
    }
 
    private void resetPassword() {
        String username = readNonEmpty("Tên đăng nhập: ");
        String temp = readNonEmpty("Mật khẩu tạm (tối thiểu 8 ký tự): ");
        if (accountManager.resetPassword(username, temp)) {
            System.out.println("Đã đặt lại. Người dùng phải đổi mật khẩu khi đăng nhập.");
        } else {
            System.out.println("Không tìm thấy tài khoản.");
        }
    }
 
    private void changeRole() {
        String username = readNonEmpty("Tên đăng nhập: ");
        if (username.equalsIgnoreCase(currentUser.getUsername())) {
            System.out.println("Không thể tự đổi quyền của chính mình.");
            return;
        }
        Role role = readRole();
        if (accountManager.changeRole(username, role)) {
            System.out.println("Đã đổi quyền.");
        } else {
            System.out.println("Không đổi được (không tìm thấy, hoặc là Admin cuối cùng).");
        }
    }
 
    private void removeAccount() {
        String username = readNonEmpty("Tên đăng nhập cần xóa: ");
        if (username.equalsIgnoreCase(currentUser.getUsername())) {
            System.out.println("Không thể tự xóa chính mình.");
            return;
        }
        if (!confirm("Xóa tài khoản " + username + "?")) return;
        if (accountManager.remove(username)) {
            System.out.println("Đã xóa.");
        } else {
            System.out.println("Không xóa được (không tìm thấy, hoặc là Admin cuối cùng).");
        }
    }
 
    // ============================================================
    // P1: NHÂN VIÊN (ADMIN và MANAGER)
    // ============================================================
    private void showStaffMenu() {
        if (isEmployeeRole()) {
            noPermission();
            return;
        }
        while (true) {
            printTitle("QUẢN LÝ NHÂN VIÊN");
            System.out.println("1. Danh sách");
            System.out.println("2. Danh sách (sắp xếp theo tên)");
            System.out.println("3. Thêm nhân viên");
            System.out.println("4. Sửa nhân viên");
            System.out.println("5. Xóa (ngưng hoạt động)");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: employeeManager.displayAll(); break;
                    case 2:
                        employeeManager.sortByName();
                        employeeManager.displayAll();
                        break;
                    case 3: addEmployee();                 break;
                    case 4: notReady("Sửa nhân viên");     break;   // TODO(P1)
                    case 5: removeEmployee();              break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private void addEmployee() {
        System.out.println("--- Thêm nhân viên ---");
        // Đọc và kiểm tra TỪNG trường trước, rồi mới new (tránh tăng bộ đếm mã khi dữ liệu sai)
        String name = readNonEmpty("Họ tên: ");
        String phone = readPhone();
        Gender gender = readGender();
        String identity = readIdentity();
        LocalDate dob = readDate("Ngày sinh (dd/MM/yyyy): ");
        String address = readNonEmpty("Địa chỉ: ");
        String position = readNonEmpty("Chức danh: ");
        double salary = readNonNegativeDouble("Lương: ");
        LocalDate hireDate = readDate("Ngày vào làm (dd/MM/yyyy): ");
 
        Employee e = new Employee(name, phone, gender, identity, dob, address, true, position, salary, hireDate);
        if (employeeManager.add(e)) {
            System.out.println("Đã thêm nhân viên " + e.getId() + ".");
        } else {
            System.out.println("Không thêm được: trùng mã, CCCD hoặc SĐT.");
        }
    }
 
    private void removeEmployee() {
        String id = readNonEmpty("Mã nhân viên: ").toUpperCase();
        if (!confirm("Ngưng hoạt động nhân viên " + id + "?")) return;
        if (employeeManager.remove(id)) {
            System.out.println("Đã ngưng hoạt động.");
        } else {
            System.out.println("Không tìm thấy nhân viên.");
        }
        // TODO(P1): Manager chỉ được xóa nhân viên thường; ngưng nhân viên thì khóa Account của họ
    }
 
    // ============================================================
    // P1: KHÁCH HÀNG (mọi Role; EMPLOYEE không được xóa)
    // ============================================================
    private void showCustomerMenu() {
        while (true) {
            printTitle("KHÁCH HÀNG");
            System.out.println("1. Danh sách");
            System.out.println("2. Tìm theo tên");
            System.out.println("3. Thêm khách hàng");
            System.out.println("4. Sửa khách hàng");
            if (!isEmployeeRole()) {
                System.out.println("5. Xóa (ngưng hoạt động)");
            }
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: customerManager.displayAll();   break;
                    case 2: searchCustomerByName();         break;
                    case 3: addCustomer();                  break;
                    case 4: notReady("Sửa khách hàng");     break;   // TODO(P1)
                    case 5:
                        if (isEmployeeRole()) noPermission();
                        else removeCustomer();
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private void searchCustomerByName() {
        String keyword = readNonEmpty("Nhập tên cần tìm: ");
        Person[] found = customerManager.findByName(keyword);
        if (found.length == 0) {
            System.out.println("Không tìm thấy.");
            return;
        }
        for (int i = 0; i < found.length; i++) {
            System.out.println(found[i].getId() + " | " + found[i].getFullName()
                    + " | " + found[i].getPhone() + " | " + found[i].getRoleName());
        }
    }
 
    private void addCustomer() {
        System.out.println("--- Thêm khách hàng ---");
        String name = readNonEmpty("Họ tên: ");
        String phone = readPhone();
        Gender gender = readGender();
        String identity = readIdentity();
        LocalDate dob = readDate("Ngày sinh (dd/MM/yyyy): ");
        String address = readNonEmpty("Địa chỉ: ");
        String email = readEmail();
 
        Customer c = new Customer(name, phone, gender, identity, dob, address, true, email);
        if (customerManager.add(c)) {
            System.out.println("Đã thêm khách hàng " + c.getId() + ".");
        } else {
            System.out.println("Không thêm được: trùng mã, CCCD hoặc SĐT.");
        }
    }
 
    private void removeCustomer() {
        String id = readNonEmpty("Mã khách hàng: ").toUpperCase();
        if (!confirm("Ngưng hoạt động khách " + id + "?")) return;
        if (customerManager.remove(id)) {
            System.out.println("Đã ngưng hoạt động.");
        } else {
            System.out.println("Không tìm thấy khách hàng.");
        }
        // TODO(P1): không cho xóa Customer đại diện khi còn Booking đang hiệu lực (cần BookingManager của P3)
    }
 
    // ============================================================
    // P1: TÀI KHOẢN CỦA TÔI, HỆ THỐNG
    // ============================================================
    private void showMyAccountMenu() {
        while (true) {
            printTitle("TÀI KHOẢN CỦA TÔI");
            System.out.println("1. Xem thông tin");
            System.out.println("2. Đổi mật khẩu");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            switch (choice) {
                case 1:
                    System.out.println("Tên đăng nhập : " + currentUser.getUsername());
                    System.out.println("Quyền         : " + currentUser.getRole());
                    System.out.println("Mã nhân viên  : " + currentUser.getEmployeeId());
                    System.out.println("Đăng nhập gần nhất: "
                            + (currentUser.getLastLogin() == null ? "Chưa có" : currentUser.getLastLogin()));
                    break;
                case 2: changePassword(currentUser, true); break;
                default: System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }
 
    // Hệ thống / Cá nhân: lưu dữ liệu, đổi mật khẩu; riêng ADMIN có thêm nhật ký, sao lưu
    private void showSystemMenu() {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        while (true) {
            printTitle("HỆ THỐNG");
            System.out.println("1. Lưu dữ liệu");
            System.out.println("2. Đổi mật khẩu");
            if (isAdmin) {
                System.out.println("3. Nhật ký thao tác");
                System.out.println("4. Sao lưu và khôi phục dữ liệu");
                System.out.println("5. Nạp dữ liệu mẫu");
            }
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1:
                        saveAll();
                        System.out.println("Đã lưu dữ liệu.");
                        break;
                    case 2: changePassword(currentUser, true); break;
                    case 3:
                    case 4:
                    case 5:
                        if (!isAdmin) noPermission();
                        else notReady("Chức năng hệ thống nâng cao");
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private void changePassword(Account acc, boolean allowCancel) {
        while (true) {
            System.out.print("Mật khẩu mới (tối thiểu 8 ký tự" + (allowCancel ? ", 0 để hủy" : "") + "): ");
            String newPassword = sc.nextLine();
            if (allowCancel && newPassword.equals("0")) return;
            try {
                acc.changePassword(newPassword);
                System.out.println("Đổi mật khẩu thành công.");
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ############################################################
    // KHUNG MENU CỦA P2, P3, P4, P5
    // Chủ module chỉ cần thay dòng notReady(...) bằng lệnh gọi Manager của mình.
    // Giữ nguyên tên hàm và cấu trúc vòng lặp để menu chính không bị gãy.
    // ############################################################
 
    // ---------- MODULE P2: PHÒNG ----------
    private void showRoomMenu() {
        while (true) {
            printTitle("PHÒNG");
            System.out.println("1. Danh sách phòng");
            System.out.println("2. Tìm phòng trống");
            if (canEdit()) System.out.println("3. Thêm phòng");
            if (canEdit()) System.out.println("4. Sửa phòng");
            if (canEdit()) System.out.println("5. Xóa phòng");
            if (canEdit()) System.out.println("6. Đổi giá phòng");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Danh sách phòng"); break;   // TODO(P2)
                    case 2: notReady("Tìm phòng trống"); break;   // TODO(P2)
                    case 3:
                        if (!canEdit()) noPermission();
                        else notReady("Thêm phòng");   // TODO(P2)
                        break;
                    case 4:
                        if (!canEdit()) noPermission();
                        else notReady("Sửa phòng");   // TODO(P2)
                        break;
                    case 5:
                        if (!canEdit()) noPermission();
                        else notReady("Xóa phòng");   // TODO(P2)
                        break;
                    case 6:
                        if (!canEdit()) noPermission();
                        else notReady("Đổi giá phòng");   // TODO(P2)
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ---------- MODULE P2: DỊCH VỤ ----------
    private void showServiceMenu() {
        while (true) {
            printTitle("DỊCH VỤ");
            System.out.println("1. Danh sách dịch vụ");
            if (canEdit()) System.out.println("2. Thêm dịch vụ");
            if (canEdit()) System.out.println("3. Sửa dịch vụ");
            if (canEdit()) System.out.println("4. Ngưng cung cấp dịch vụ");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Danh sách dịch vụ"); break;   // TODO(P2)
                    case 2:
                        if (!canEdit()) noPermission();
                        else notReady("Thêm dịch vụ");   // TODO(P2)
                        break;
                    case 3:
                        if (!canEdit()) noPermission();
                        else notReady("Sửa dịch vụ");   // TODO(P2)
                        break;
                    case 4:
                        if (!canEdit()) noPermission();
                        else notReady("Ngưng cung cấp dịch vụ");   // TODO(P2)
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ---------- MODULE P2: BẢO TRÌ PHÒNG ----------
    private void showMaintenanceMenu() {
        while (true) {
            printTitle("BẢO TRÌ PHÒNG");
            System.out.println("1. Danh sách bảo trì");
            System.out.println("2. Tạo bản ghi bảo trì (báo hỏng)");
            if (canEdit()) System.out.println("3. Đóng bản ghi bảo trì");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Danh sách bảo trì"); break;   // TODO(P2)
                    case 2: notReady("Tạo bản ghi bảo trì (báo hỏng)"); break;   // TODO(P2)
                    case 3:
                        if (!canEdit()) noPermission();
                        else notReady("Đóng bản ghi bảo trì");   // TODO(P2)
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ---------- MODULE P3: ĐẶT PHÒNG ----------
    private void showBookingMenu() {
        while (true) {
            printTitle("ĐẶT PHÒNG");
            System.out.println("1. Đặt phòng cá nhân");
            System.out.println("2. Đặt phòng đoàn");
            System.out.println("3. Sửa đặt phòng");
            System.out.println("4. Hủy đặt phòng");
            System.out.println("5. Gửi yêu cầu thay đổi (chờ quản lý duyệt)");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Đặt phòng cá nhân"); break;   // TODO(P3)
                    case 2: notReady("Đặt phòng đoàn"); break;   // TODO(P3)
                    case 3: notReady("Sửa đặt phòng"); break;   // TODO(P3)
                    case 4: notReady("Hủy đặt phòng"); break;   // TODO(P3)
                    case 5: notReady("Gửi yêu cầu thay đổi (chờ quản lý duyệt)"); break;   // TODO(P3)
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ---------- MODULE P3: DUYỆT YÊU CẦU ----------
    private void showRequestMenu() {
        if (isEmployeeRole()) {
            noPermission();
            return;
        }
        while (true) {
            printTitle("DUYỆT YÊU CẦU");
            System.out.println("1. Danh sách yêu cầu chờ duyệt");
            System.out.println("2. Duyệt yêu cầu");
            System.out.println("3. Từ chối yêu cầu");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Danh sách yêu cầu chờ duyệt"); break;   // TODO(P3)
                    case 2: notReady("Duyệt yêu cầu"); break;   // TODO(P3)
                    case 3: notReady("Từ chối yêu cầu"); break;   // TODO(P3)
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ---------- MODULE P4: LƯU TRÚ ----------
    private void showStayMenu() {
        while (true) {
            printTitle("LƯU TRÚ");
            System.out.println("1. Nhận phòng");
            System.out.println("2. Ghi dịch vụ");
            System.out.println("3. Đổi phòng");
            System.out.println("4. Trả phòng");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Nhận phòng"); break;   // TODO(P4)
                    case 2: notReady("Ghi dịch vụ"); break;   // TODO(P4)
                    case 3: notReady("Đổi phòng"); break;   // TODO(P4)
                    case 4: notReady("Trả phòng"); break;   // TODO(P4)
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ---------- MODULE P5: THANH TOÁN ----------
    private void showPaymentMenu() {
        while (true) {
            printTitle("THANH TOÁN");
            System.out.println("1. Lập hóa đơn");
            System.out.println("2. Thu tiền");
            System.out.println("3. Ghi phụ thu");
            System.out.println("4. Xem và in hóa đơn");
            if (canEdit()) System.out.println("5. Áp giảm giá");
            if (canEdit()) System.out.println("6. Hủy hóa đơn");
            if (canEdit()) System.out.println("7. Hoàn tiền");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Lập hóa đơn"); break;   // TODO(P5)
                    case 2: notReady("Thu tiền"); break;   // TODO(P5)
                    case 3: notReady("Ghi phụ thu"); break;   // TODO(P5)
                    case 4: notReady("Xem và in hóa đơn"); break;   // TODO(P5)
                    case 5:
                        if (!canEdit()) noPermission();
                        else notReady("Áp giảm giá");   // TODO(P5)
                        break;
                    case 6:
                        if (!canEdit()) noPermission();
                        else notReady("Hủy hóa đơn");   // TODO(P5)
                        break;
                    case 7:
                        if (!canEdit()) noPermission();
                        else notReady("Hoàn tiền");   // TODO(P5)
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ---------- MODULE P5: THỐNG KÊ VÀ BÁO CÁO ----------
    private void showReportMenu() {
        if (isEmployeeRole()) {
            noPermission();
            return;
        }
        while (true) {
            printTitle("THỐNG KÊ VÀ BÁO CÁO");
            System.out.println("1. Doanh thu theo kỳ");
            System.out.println("2. Công suất phòng");
            System.out.println("3. Dịch vụ bán chạy");
            System.out.println("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Doanh thu theo kỳ"); break;   // TODO(P5)
                    case 2: notReady("Công suất phòng"); break;   // TODO(P5)
                    case 3: notReady("Dịch vụ bán chạy"); break;   // TODO(P5)
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ============================================================
    // HÀM NHẬP LIỆU DÙNG CHUNG (sai thì báo lỗi rồi hỏi lại)
    // ============================================================
 
    private void printTitle(String title) {
        System.out.println();
        System.out.println("===== " + title + " =====");
    }
 
    private void notReady(String featureName) {      // dùng cho chức năng chưa có người viết
        System.out.println("[Đang phát triển] " + featureName);
    }
 
    private void noPermission() {
        System.out.println("Bạn không có quyền sử dụng chức năng này.");
    }
 
    private boolean isEmployeeRole() {
        return currentUser.getRole() == Role.EMPLOYEE;
    }
 
    // true nếu được phép thêm/sửa/xóa (EMPLOYEE chỉ xem)
    private boolean canEdit() {
        return currentUser.getRole() != Role.EMPLOYEE;
    }
 
    // Đọc số nguyên cho lựa chọn menu. Gõ chữ thì hỏi lại.
    private int readChoice() {
        while (true) {
            System.out.print("Chọn: ");
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập một số.");
            }
        }
    }
 
    private String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Không được để trống.");
        }
    }
 
    private double readNonNegativeDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                double value = Double.parseDouble(s);
                CheckingExpression.checkNonNegativeDouble(value, "Giá trị không được âm");
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập một số.");
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return LocalDate.parse(s, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Ngày không hợp lệ. Nhập dạng dd/MM/yyyy (VD 25/12/1990).");
            }
        }
    }
 
    private Gender readGender() {
        while (true) {
            System.out.print("Giới tính (1. Nam, 2. Nữ, 3. Khác): ");
            String s = sc.nextLine().trim();
            if (s.equals("1")) return Gender.MALE;
            if (s.equals("2")) return Gender.FEMALE;
            if (s.equals("3")) return Gender.OTHER;
            System.out.println("Chỉ nhập 1, 2 hoặc 3.");
        }
    }
 
    private Role readRole() {
        while (true) {
            System.out.print("Quyền (1. ADMIN, 2. MANAGER, 3. EMPLOYEE): ");
            String s = sc.nextLine().trim();
            if (s.equals("1")) return Role.ADMIN;
            if (s.equals("2")) return Role.MANAGER;
            if (s.equals("3")) return Role.EMPLOYEE;
            System.out.println("Chỉ nhập 1, 2 hoặc 3.");
        }
    }
 
    private String readPhone() {
        while (true) {
            System.out.print("SĐT: ");
            String s = sc.nextLine().trim();
            try {
                CheckingExpression.checkValidPhone(s);
                return s;
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private String readIdentity() {
        while (true) {
            System.out.print("CCCD/Hộ chiếu: ");
            String s = sc.nextLine().trim();
            try {
                CheckingExpression.checkValidIdentityNumber(s, false);
                return s;
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private String readEmail() {
        while (true) {
            System.out.print("Email: ");
            String s = sc.nextLine().trim();
            try {
                CheckingExpression.checkValidEmail(s);
                return s;
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // Hỏi có/không cho thao tác nguy hiểm (xóa, hủy...)
    private boolean confirm(String question) {
        while (true) {
            System.out.print(question + " (y/n): ");
            String s = sc.nextLine().trim().toLowerCase();
            if (s.equals("y")) return true;
            if (s.equals("n")) return false;
            System.out.println("Chỉ nhập y hoặc n.");
        }
    }
}
