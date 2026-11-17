package hotel.ui;
 
import hotel.model.Account;
import hotel.model.Booking;
import hotel.model.BookingDetail;
import hotel.model.BookingRequest;
import hotel.model.Customer;
import hotel.model.Deposit;
import hotel.model.Employee;
import hotel.model.GuestMember;
import hotel.model.Person;
import hotel.model.Room;
import hotel.model.enums.Gender;
import hotel.model.enums.PaymentMethod;
import hotel.model.enums.Role;
import hotel.model.enums.RequestStatus;
import hotel.service.AccountManager;
import hotel.service.BookingManager;
import hotel.service.CustomerManager;
import hotel.service.EmployeeManager;
import hotel.service.RoomManager;
import hotel.util.CheckingExpression;
 
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Scanner;
 
/**
 * TOÀN BỘ MENU nằm trong file này. Mỗi menu là một hàm showXxxMenu().
 *
 * Cách đọc file:
 *   run()            -> vòng lặp chính: đăng nhập -> menu theo Role -> đăng xuất
 *   showAdminMenu / showManagerMenu / showEmployeeMenu   -> menu chính của từng Role (có switch-case)
 *   Phần "P1"        -> menu con của module Người, Tài khoản (đã làm)
 *   Phần module P2/P4/P5 -> menu con đang được ghép dần; P3 đã nối với BookingManager
 *   Cuối file        -> các hàm nhập liệu dùng chung
 */
public class ConsoleMenu {
 
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int FRAME_WIDTH = 60;
    private static final String INDENT = "    ";
 
    private final Scanner sc = new Scanner(System.in);   // CHỈ MỘT Scanner cho cả chương trình
 
    private final AccountManager accountManager;
    private final EmployeeManager employeeManager;
    private final CustomerManager customerManager;
    private final RoomManager roomManager;
    private final BookingManager bookingManager;
 
    private Account currentUser;                         // người đang đăng nhập (null = chưa đăng nhập)
    private int depth = 0;
 
    public ConsoleMenu(AccountManager accountManager,
                       EmployeeManager employeeManager,
                       CustomerManager customerManager) {
        this(accountManager, employeeManager, customerManager, new RoomManager(), new BookingManager());
    }

    public ConsoleMenu(AccountManager accountManager,
                       EmployeeManager employeeManager,
                       CustomerManager customerManager,
                       RoomManager roomManager,
                       BookingManager bookingManager) {
        this.accountManager = accountManager;
        this.employeeManager = employeeManager;
        this.customerManager = customerManager;
        this.roomManager = roomManager;
        this.bookingManager = bookingManager;
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
            printMenu("1. Quản lý tài khoản", "2. Quản lý nhân viên", "3. Danh mục khách sạn",
                    "4. Nghiệp vụ", "5. Thống kê và báo cáo", "6. Hệ thống", "7. Tài khoản của tôi");
            item("0. Đăng xuất");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: openSub(this::showAccountMenu);   break;
                    case 2: openSub(this::showStaffMenu);     break;
                    case 3: openSub(this::showCatalogMenu);   break;
                    case 4: openSub(this::showBusinessMenu);  break;
                    case 5: openSub(this::showReportMenu);    break;
                    case 6: openSub(this::showSystemMenu);    break;
                    case 7: openSub(this::showMyAccountMenu); break;
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
            printMenu("1. Danh sách", "2. Đặt phòng và lưu trú", "3. Thanh toán", "4. Duyệt và xử lý",
                    "5. Danh mục khách sạn", "6. Nhân viên", "7. Thống kê", "8. Hệ thống");
            item("0. Đăng xuất");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: openSub(this::showListMenu);            break;
                    case 2: openSub(this::showBookingAndStayMenu);  break;
                    case 3: openSub(this::showPaymentMenu);         break;
                    case 4: openSub(this::showApprovalMenu);        break;
                    case 5: openSub(this::showCatalogMenu);         break;
                    case 6: openSub(this::showStaffMenu);           break;
                    case 7: openSub(this::showReportMenu);          break;
                    case 8: openSub(this::showSystemMenu);          break;
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
            printMenu("1. Đặt phòng", "2. Lưu trú", "3. Thanh toán", "4. Khách hàng", "5. Danh sách", "6. Cá nhân");
            item("0. Đăng xuất");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: openSub(this::showBookingMenu);   break;
                    case 2: openSub(this::showStayMenu);      break;
                    case 3: openSub(this::showPaymentMenu);   break;
                    case 4: openSub(this::showCustomerMenu);  break;
                    case 5: openSub(this::showListMenu);      break;
                    case 6: openSub(this::showSystemMenu);    break;
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
            printMenu("1. Phòng", "2. Dịch vụ", "3. Bảo trì phòng");
            item("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            switch (choice) {
                case 1: openSub(this::showRoomMenu);         break;
                case 2: openSub(this::showServiceMenu);      break;
                case 3: openSub(this::showMaintenanceMenu);  break;
                default: System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }
 
    private void showBusinessMenu() {
        while (true) {
            printTitle("NGHIỆP VỤ");
            printMenu("1. Đặt phòng", "2. Lưu trú", "3. Thanh toán", "4. Duyệt hủy phiếu đặt phòng",
                    "5. Khách hàng", "6. Thành viên đoàn");
            item("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            switch (choice) {
                case 1: openSub(this::showBookingMenu);   break;
                case 2: openSub(this::showStayMenu);      break;
                case 3: openSub(this::showPaymentMenu);   break;
                case 4: openSub(this::showRequestMenu);   break;
                case 5: openSub(this::showCustomerMenu);  break;
                case 6: openSub(this::showGuestMenu);     break;
                default: System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }
 
    private void showBookingAndStayMenu() {
        while (true) {
            printTitle("ĐẶT PHÒNG VÀ LƯU TRÚ");
            printMenu("1. Đặt phòng", "2. Lưu trú (nhận phòng, dịch vụ, đổi phòng, trả phòng)",
                    "3. Thành viên đoàn");
            item("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            switch (choice) {
                case 1: openSub(this::showBookingMenu); break;
                case 2: openSub(this::showStayMenu);    break;
                case 3: openSub(this::showGuestMenu);   break;
                default: System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }
 
    private void showApprovalMenu() {
        while (true) {
            printTitle("DUYỆT VÀ XỬ LÝ");
            printMenu("1. Duyệt hủy phiếu đặt phòng (hoàn / mất cọc)", "2. Duyệt giảm giá",
                    "3. Hủy hóa đơn", "4. Hoàn tiền");
            item("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            switch (choice) {
                case 1: openSub(this::showRequestMenu);                    break;
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
            printMenu("1. Phòng", "2. Khách hàng", isEmployeeRole() ? null : "3. Nhân viên",
                    "4. Đặt phòng", "5. Khách đang lưu trú", "6. Hóa đơn", "7. Dịch vụ");
            item("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: notReady("Danh sách phòng (P2)");        break;   // TODO(P2): roomManager.displayAll()
                    case 2: openSub(() -> showPostListMenu(false));    break;
                    case 3:
                        if (isEmployeeRole()) noPermission();          // kiểm tra quyền ngay trong xử lý, không chỉ ẩn menu
                        else openSub(() -> showPostListMenu(true));
                        break;
                    case 4: openSub(this::showBookingList);          break;
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

    private void showPostListMenu(boolean staff) {
        displayList(staff);
        while (true) {
            printTitle("DANH SÁCH " + (staff ? "NHÂN VIÊN" : "KHÁCH HÀNG"));
            printMenu("1. Xem chi tiết", "2. Tìm kiếm", "3. Lọc theo trạng thái", "4. Sắp xếp theo tên",
                    canEdit() ? "5. Thêm mới" : null, canEdit() ? "6. Sửa" : null,
                    canEdit() ? "7. Xóa (ngưng hoạt động)" : null);
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1:
                        showDetail(staff);
                        break;
                    case 2:
                        if (staff) searchEmployee();
                        else searchCustomer();
                        break;
                    case 3:
                        filterByStatus(staff);
                        break;
                    case 4:
                        if (staff) employeeManager.sortByName();
                        else customerManager.sortByName();
                        displayList(staff);
                        break;
                    case 5:
                    case 6:
                    case 7:
                        if (!canEdit()) {
                            noPermission();
                            break;
                        }
                        if (choice == 5) {
                            if (staff) addEmployee();
                            else addCustomer();
                        } else if (choice == 6) {
                            if (staff) editEmployee();
                            else editCustomer();
                        } else {
                            if (staff) removeEmployee();
                            else removeCustomer();
                        }
                        displayList(staff);
                        break;
                    default:
                        System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void displayList(boolean staff) {
        if (staff) employeeManager.displayAll();
        else customerManager.displayAll();
    }

    private void showDetail(boolean staff) {
        String id = readNonEmpty("Mã cần xem: ").toUpperCase();
        Person person = staff ? employeeManager.findById(id) : customerManager.findById(id);
        if (person == null) {
            System.out.println("Không tìm thấy.");
            return;
        }
        System.out.println("Mã            : " + person.getId());
        System.out.println("Loại          : " + person.getRoleName());
        System.out.println("Họ tên        : " + person.getFullName());
        System.out.println("SĐT           : " + person.getPhone());
        System.out.println("Giới tính     : " + person.getGender());
        System.out.println("CCCD/Hộ chiếu : " + person.getIdentityNumber());
        System.out.println("Ngày sinh     : " + person.getDateOfBirth().format(DATE_FORMAT));
        System.out.println("Địa chỉ       : " + person.getAddress());
        System.out.println("Trạng thái    : " + (person.isActive() ? "Hoạt động" : "Ngưng"));
        if (person instanceof Customer) {
            System.out.println("Email         : " + ((Customer) person).getEmail());
        } else if (person instanceof GuestMember) {
            GuestMember guest = (GuestMember) person;
            System.out.println("Người đại diện: " + guest.getRepresentativeId());
            System.out.println("Quan hệ       : " + guest.getRelationship());
        } else if (person instanceof Employee) {
            Employee employee = (Employee) person;
            System.out.println("Chức danh     : " + employee.getPosition());
            System.out.println("Lương         : " + employee.getSalary());
            System.out.println("Ngày vào làm  : " + employee.getHireDate().format(DATE_FORMAT));
        }
    }

    private void filterByStatus(boolean staff) {
        item("Lọc:");
        printMenu("1. Đang hoạt động", "2. Đã ngưng");
        int choice = readChoice();
        if (choice != 1 && choice != 2) {
            System.out.println("Lựa chọn không hợp lệ.");
            return;
        }
        ArrayList<Person> all = new ArrayList<>();
        if (staff) all.addAll(employeeManager.getAll());
        else all.addAll(customerManager.getAll());

        ArrayList<Person> matched = new ArrayList<>();
        for (Person person : all) {
            if (person.isActive() == (choice == 1)) matched.add(person);
        }
        printPersons(matched.toArray(new Person[0]));
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
            printMenu("1. Danh sách tài khoản", "2. Cấp tài khoản cho nhân viên",
                    "3. Khóa / mở khóa", "4. Đặt lại mật khẩu", "5. Đổi quyền", "6. Xóa tài khoản");
            item("0. Quay lại");
 
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
        printTitle("CẤP TÀI KHOẢN CHO NHÂN VIÊN");
        String employeeId = readNonEmpty("Mã nhân viên (VD NV001): ").toUpperCase();
        Employee emp = employeeManager.findById(employeeId);
        if (emp == null) {
            System.out.println("Không tìm thấy nhân viên " + employeeId + ".");
            return;
        }
        if (!emp.isActive()) {
            System.out.println("Nhân viên " + employeeId + " đã ngưng hoạt động, không thể cấp tài khoản.");
            return;
        }
        if (accountManager.findByEmployeeId(employeeId) != null) {
            System.out.println(emp.getFullName() + " đã có tài khoản.");
            return;
        }

        System.out.println("Cấp tài khoản cho: " + emp.getFullName() + " (" + emp.getPosition() + ")");
        System.out.println("Tên đăng nhập: " + employeeId + " (tự động lấy theo mã nhân viên)");
        Role role = readRole();

        Account acc;
        while (true) {
            System.out.print("Mật khẩu tạm (tối thiểu 8 ký tự): ");
            String password = sc.nextLine();
            try {
                acc = new Account(employeeId, password, role, employeeId);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
 
        if (accountManager.add(acc)) {
            System.out.println("Đã cấp tài khoản. Người dùng phải đổi mật khẩu ở lần đăng nhập đầu.");
        } else {
            System.out.println("Không tạo được tài khoản (trùng tên đăng nhập).");
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
            printMenu("1. Danh sách", "2. Danh sách (sắp xếp theo tên)", "3. Thêm nhân viên",
                    "4. Sửa nhân viên", "5. Xóa (ngưng hoạt động)", "6. Tìm kiếm");
            item("0. Quay lại");
 
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
                    case 4: editEmployee();                break;
                    case 5: removeEmployee();              break;
                    case 6: searchEmployee();              break;
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
        Employee target = employeeManager.findById(id);
        if (target == null) {
            System.out.println("Không tìm thấy nhân viên.");
            return;
        }
        if (id.equals(currentUser.getEmployeeId())) {
            System.out.println("Không thể tự ngưng hoạt động chính mình.");
            return;
        }
        if (!canManageEmployee(target)) {
            System.out.println("Bạn chỉ được ngưng nhân viên có quyền EMPLOYEE.");
            return;
        }
        if (!confirm("Ngưng hoạt động nhân viên " + id + "?")) return;
        employeeManager.remove(id);
        System.out.println("Đã ngưng hoạt động.");
    }

    private boolean canManageEmployee(Employee target) {
        if (isEmployeeRole()) return false;
        if (currentUser.getRole() == Role.ADMIN) return true;
        Account account = accountManager.findByEmployeeId(target.getId());
        return account == null || account.getRole() == Role.EMPLOYEE;
    }

    private void editEmployee() {
        String id = readNonEmpty("Mã nhân viên cần sửa: ").toUpperCase();
        Employee employee = employeeManager.findById(id);
        if (employee == null) {
            System.out.println("Không tìm thấy nhân viên.");
            return;
        }
        if (!canManageEmployee(employee)) {
            System.out.println("Bạn chỉ được sửa nhân viên có quyền EMPLOYEE.");
            return;
        }
        while (true) {
            printTitle("SỬA NHÂN VIÊN " + employee.getId() + " - " + employee.getFullName());
            printCommonFieldMenu();
            printMenu("7. Chức danh", "8. Lương", "9. Ngày vào làm");
            item("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            try {
                if (editCommonField(employee, choice)) continue;
                switch (choice) {
                    case 7: employee.setPosition(readNonEmpty("Chức danh mới: ")); break;
                    case 8: employee.setSalary(readNonNegativeDouble("Lương mới: ")); break;
                    case 9: employee.setHireDate(readDate("Ngày vào làm (dd/MM/yyyy): ")); break;
                    default:
                        System.out.println("Lựa chọn không hợp lệ.");
                        continue;
                }
                System.out.println("Đã cập nhật.");
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    // ============================================================
    // P1: KHÁCH HÀNG (mọi Role; EMPLOYEE không được xóa)
    // ============================================================
    private void showCustomerMenu() {
        while (true) {
            printTitle("KHÁCH HÀNG");
            printMenu("1. Danh sách", "2. Tìm kiếm", "3. Thêm khách hàng", "4. Sửa khách hàng",
                    isEmployeeRole() ? null : "5. Xóa (ngưng hoạt động)", "6. Thành viên đoàn");
            item("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: customerManager.displayAll();   break;
                    case 2: searchCustomer();               break;
                    case 3: addCustomer();                  break;
                    case 4: editCustomer();                  break;
                    case 5:
                        if (isEmployeeRole()) noPermission();
                        else removeCustomer();
                        break;
                    case 6: openSub(this::showGuestMenu);                break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
 
    private void editCustomer() {
        String id = readNonEmpty("Mã khách hàng cần sửa: ").toUpperCase();
        Customer customer = customerManager.findCustomerById(id);
        if (customer == null) {
            System.out.println("Không tìm thấy khách hàng.");
            return;
        }
        while (true) {
            printTitle("SỬA KHÁCH HÀNG " + customer.getId() + " - " + customer.getFullName());
            printCommonFieldMenu();
            printMenu("7. Email");
            item("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            try {
                if (editCommonField(customer, choice)) continue;
                if (choice == 7) {
                    customer.setEmail(readEmail());
                    System.out.println("Đã cập nhật.");
                } else {
                    System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void showGuestMenu() {
        while (true) {
            printTitle("THÀNH VIÊN ĐOÀN");
            printMenu("1. Danh sách thành viên theo người đại diện", "2. Thêm thành viên", "3. Sửa thành viên",
                    canEdit() ? "4. Xóa (ngưng hoạt động)" : null);
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1: listGuests(); break;
                    case 2: addGuest(); break;
                    case 3: editGuest(); break;
                    case 4:
                        if (!canEdit()) noPermission();
                        else removeGuest();
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void listGuests() {
        String representativeId = readNonEmpty("Mã khách đại diện: ").toUpperCase();
        if (customerManager.findCustomerById(representativeId) == null) {
            System.out.println("Không tìm thấy khách đại diện.");
            return;
        }
        printPersons(customerManager.findGuestsByRepresentative(representativeId));
    }

    private void addGuest() {
        String representativeId = readNonEmpty("Mã khách đại diện: ").toUpperCase();
        if (customerManager.findCustomerById(representativeId) == null) {
            System.out.println("Không tìm thấy khách đại diện.");
            return;
        }
        String name = readNonEmpty("Họ tên: ");
        String phone = readPhone();
        Gender gender = readGender();
        String identity = readIdentityOptional();
        LocalDate dateOfBirth = readDate("Ngày sinh (dd/MM/yyyy): ");
        String address = readNonEmpty("Địa chỉ: ");
        String relationship = readNonEmpty("Quan hệ với người đại diện: ");

        GuestMember guest = new GuestMember(name, phone, gender, identity, dateOfBirth,
                address, true, representativeId, relationship);
        if (customerManager.add(guest)) {
            System.out.println("Đã thêm thành viên " + guest.getId() + ".");
        } else {
            System.out.println("Không thêm được: trùng mã, CCCD hoặc người đại diện không hợp lệ.");
        }
    }

    private void editGuest() {
        String id = readNonEmpty("Mã thành viên cần sửa: ").toUpperCase();
        Person person = customerManager.findById(id);
        if (!(person instanceof GuestMember)) {
            System.out.println("Không tìm thấy thành viên đoàn.");
            return;
        }
        GuestMember guest = (GuestMember) person;
        while (true) {
            printTitle("SỬA THÀNH VIÊN " + guest.getId() + " - " + guest.getFullName());
            printCommonFieldMenu();
            printMenu("7. Quan hệ với người đại diện");
            item("0. Quay lại");
            int choice = readChoice();
            if (choice == 0) return;
            try {
                if (editCommonField(guest, choice)) continue;
                if (choice == 7) {
                    guest.setRelationship(readNonEmpty("Quan hệ mới: "));
                    System.out.println("Đã cập nhật.");
                } else {
                    System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void removeGuest() {
        String id = readNonEmpty("Mã thành viên: ").toUpperCase();
        if (!(customerManager.findById(id) instanceof GuestMember)) {
            System.out.println("Không tìm thấy thành viên đoàn.");
            return;
        }
        if (!confirm("Ngưng hoạt động thành viên " + id + "?")) return;
        customerManager.remove(id);
        System.out.println("Đã ngưng hoạt động.");
    }

    private boolean editCommonField(Person person, int choice) {
        switch (choice) {
            case 1:
                person.setName(readNonEmpty("Họ tên mới: "));
                break;
            case 2: {
                String phone = readPhone();
                if (isTakenByOther(person, findSamePhone(person, phone))) {
                    System.out.println("SĐT đã được dùng.");
                    return true;
                }
                person.setPhone(phone);
                break;
            }
            case 3:
                person.setGender(readGender());
                break;
            case 4: {
                String identity = person instanceof GuestMember ? readIdentityOptional() : readIdentity();
                if (isTakenByOther(person, findSameIdentity(person, identity))) {
                    System.out.println("CCCD/Hộ chiếu đã được dùng.");
                    return true;
                }
                person.setIdentityNumber(identity);
                break;
            }
            case 5:
                person.setDateOfBirth(readDate("Ngày sinh (dd/MM/yyyy): "));
                break;
            case 6:
                person.setAddress(readNonEmpty("Địa chỉ mới: "));
                break;
            default:
                return false;
        }
        System.out.println("Đã cập nhật.");
        return true;
    }

    private void printCommonFieldMenu() {
        printMenu("1. Họ tên", "2. SĐT", "3. Giới tính", "4. CCCD/Hộ chiếu", "5. Ngày sinh", "6. Địa chỉ");
    }

    private Person[] findSamePhone(Person person, String phone) {
        if (person instanceof Employee) return employeeManager.findByPhone(phone);
        if (person instanceof Customer) return customerManager.findByPhone(phone);
        return null;
    }

    private Person[] findSameIdentity(Person person, String identity) {
        if (identity.isEmpty()) return null;
        Person found = person instanceof Employee
                ? employeeManager.findByIdentityNumber(identity)
                : customerManager.findByIdentityNumber(identity);
        return found == null ? null : new Person[]{found};
    }

    private boolean isTakenByOther(Person person, Person[] matches) {
        if (matches == null) return false;
        for (Person match : matches) {
            if (!match.getId().equals(person.getId())) return true;
        }
        return false;
    }

    private void searchEmployee() {
        item("Tìm theo:");
        printMenu("1. Tên", "2. Mã", "3. SĐT", "4. CCCD/Hộ chiếu");
        Person[] found;
        switch (readChoice()) {
            case 1: found = employeeManager.findByName(readNonEmpty("Nhập tên: ")); break;
            case 2: found = toArray(employeeManager.findById(readNonEmpty("Nhập mã: ").toUpperCase())); break;
            case 3: found = employeeManager.findByPhone(readNonEmpty("Nhập SĐT: ")); break;
            case 4: found = toArray(employeeManager.findByIdentityNumber(
                    readNonEmpty("Nhập CCCD/Hộ chiếu: "))); break;
            default:
                System.out.println("Lựa chọn không hợp lệ.");
                return;
        }
        printPersons(found);
    }

    private void searchCustomer() {
        item("Tìm theo:");
        printMenu("1. Tên", "2. Mã", "3. SĐT", "4. CCCD/Hộ chiếu");
        Person[] found;
        switch (readChoice()) {
            case 1: found = customerManager.findByName(readNonEmpty("Nhập tên: ")); break;
            case 2: found = toArray(customerManager.findById(readNonEmpty("Nhập mã: ").toUpperCase())); break;
            case 3: found = customerManager.findByPhone(readNonEmpty("Nhập SĐT: ")); break;
            case 4: found = toArray(customerManager.findByIdentityNumber(
                    readNonEmpty("Nhập CCCD/Hộ chiếu: "))); break;
            default:
                System.out.println("Lựa chọn không hợp lệ.");
                return;
        }
        printPersons(found);
    }

    private Person[] toArray(Person person) {
        return person == null ? new Person[0] : new Person[]{person};
    }

    private void printPersons(Person[] people) {
        if (people == null || people.length == 0) {
            System.out.println("Không tìm thấy.");
            return;
        }
        for (Person person : people) {
            System.out.println(person.getId() + " | " + person.getFullName() + " | " + person.getPhone()
                    + " | " + person.getRoleName() + " | "
                    + (person.isActive() ? "Hoạt động" : "Ngưng"));
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
            printMenu("1. Xem thông tin", "2. Đổi mật khẩu");
            item("0. Quay lại");
 
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
            printMenu("1. Lưu dữ liệu", "2. Đổi mật khẩu", isAdmin ? "3. Nhật ký thao tác" : null,
                    isAdmin ? "4. Sao lưu và khôi phục dữ liệu" : null, isAdmin ? "5. Nạp dữ liệu mẫu" : null);
            item("0. Quay lại");
 
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
    // MENU CÁC MODULE P2-P5
    // Giữ tên hàm và cấu trúc vòng lặp để menu chính tiếp tục hoạt động khi ghép module.
    // ############################################################
 
    // ---------- MODULE P2: PHÒNG ----------
    private void showRoomMenu() {
        while (true) {
            printTitle("PHÒNG");
            printMenu("1. Danh sách phòng", "2. Tìm phòng trống", canEdit() ? "3. Thêm phòng" : null,
                    canEdit() ? "4. Sửa phòng" : null, canEdit() ? "5. Xóa phòng" : null,
                    canEdit() ? "6. Đổi giá phòng" : null);
            item("0. Quay lại");
 
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
            printMenu("1. Danh sách dịch vụ", canEdit() ? "2. Thêm dịch vụ" : null,
                    canEdit() ? "3. Sửa dịch vụ" : null, canEdit() ? "4. Ngưng cung cấp dịch vụ" : null);
            item("0. Quay lại");
 
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
            printMenu("1. Danh sách bảo trì", "2. Tạo bản ghi bảo trì (báo hỏng)",
                    canEdit() ? "3. Đóng bản ghi bảo trì" : null);
            item("0. Quay lại");
 
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
            printMenu("1. Nhận yêu cầu đặt phòng qua điện thoại", "2. Đặt phòng trực tiếp - cá nhân",
                    "3. Đặt phòng trực tiếp - đoàn", "4. Sửa yêu cầu đặt phòng",
                    canEdit() ? "5. Hủy Booking trực tiếp" : null, "6. Thu cọc",
                    "7. Gửi đề nghị hủy (chờ quản lý duyệt)", "8. Lập Booking khi khách đến",
                    "9. Danh sách đặt phòng");
            item("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: receiveBookingRequest(); break;
                    case 2: createDirectBooking(false); break;
                    case 3: createDirectBooking(true); break;
                    case 4: editPendingRequest(); break;
                    case 5:
                        if (!canEdit()) noPermission();
                        else cancelDirectBooking();
                        break;
                    case 6: receiveDeposit(); break;
                    case 7: requestBookingCancellation(); break;
                    case 8: convertRequestToBooking(); break;
                    case 9: showBookingList(); break;
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
            printTitle("DUYỆT HỦY PHIẾU ĐẶT PHÒNG");
            printMenu("1. Danh sách đề nghị hủy chờ duyệt",
                    "2. Duyệt hủy và quyết định hoàn/mất cọc", "3. Từ chối đề nghị hủy");
            item("0. Quay lại");
 
            int choice = readChoice();
            if (choice == 0) return;
 
            try {
                switch (choice) {
                    case 1: showCancellationRequests(); break;
                    case 2: approveBookingCancellation(); break;
                    case 3: rejectBookingCancellation(); break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void receiveBookingRequest() {
        Customer customer = promptActiveCustomer();
        if (customer == null) return;
        item("Loại yêu cầu: 1. Cá nhân  2. Đoàn");
        int typeChoice = readChoice();
        if (typeChoice != 1 && typeChoice != 2) {
            System.out.println("Lựa chọn không hợp lệ.");
            return;
        }
        String groupName = typeChoice == 2 ? readNonEmpty("Tên đoàn: ") : null;
        LocalDate[] stayDates = readBookingDates();
        if (stayDates == null) return;
        String requestId = bookingManager.generateRequestId();
        BookingDetail[] details = collectBookingDetails(requestId, stayDates[0], stayDates[1],
                typeChoice == 2);
        if (details == null) return;

        BookingRequest request = new BookingRequest(requestId, customer.getId(),
                typeChoice == 1 ? "INDIVIDUAL" : "GROUP", groupName,
                stayDates[0], stayDates[1], currentUser.getEmployeeId(), details.length);
        for (BookingDetail detail : details) request.addDetail(detail);
        if (bookingManager.createBookingRequest(request)) {
            System.out.printf("Đã tạo phiếu %s. Tiền cọc tối thiểu: %,.0f%n",
                    requestId, request.calculateRequiredDeposit());
        } else {
            System.out.println("Không tạo được phiếu yêu cầu.");
        }
    }

    private void createDirectBooking(boolean group) {
        Customer customer = promptActiveCustomer();
        if (customer == null) return;
        String groupName = group ? readNonEmpty("Tên đoàn: ") : null;
        LocalDate[] stayDates = readBookingDates();
        if (stayDates == null) return;
        BookingDetail[] details = collectBookingDetails(null, stayDates[0], stayDates[1], group);
        if (details == null) return;
        Booking booking = bookingManager.createDirectBooking(customer.getId(),
                group ? "GROUP" : "INDIVIDUAL", groupName, stayDates[0], stayDates[1],
                currentUser.getEmployeeId(), readOptional("Ghi chú (Enter nếu không có): "), details);
        if (booking == null) {
            System.out.println("Không tạo được Booking.");
            return;
        }
        System.out.println("Đã tạo Booking " + booking.getBookingId() + ".");
    }

    private Customer promptActiveCustomer() {
        String customerId = readNonEmpty("Mã khách hàng: ").toUpperCase();
        Customer customer = customerManager.findCustomerById(customerId);
        if (customer == null || !customer.isActive()) {
            System.out.println("Không tìm thấy khách hàng đang hoạt động.");
            return null;
        }
        return customer;
    }

    private LocalDate[] readBookingDates() {
        while (true) {
            LocalDate checkIn = readDate("Ngày nhận phòng (dd/MM/yyyy): ");
            LocalDate checkOut = readDate("Ngày trả phòng (dd/MM/yyyy): ");
            if (checkIn.isBefore(LocalDate.now())) {
                System.out.println("Ngày nhận phòng không được ở quá khứ.");
            } else if (!checkOut.isAfter(checkIn)) {
                System.out.println("Ngày trả phòng phải sau ngày nhận phòng.");
            } else {
                return new LocalDate[]{checkIn, checkOut};
            }
            if (!confirm("Nhập lại ngày")) return null;
        }
    }

    private BookingDetail[] collectBookingDetails(String requestId, LocalDate checkIn,
                                                   LocalDate checkOut, boolean group) {
        ArrayList<Room> availableRooms = new ArrayList<>();
        for (Room room : roomManager.findAvailableRooms(checkIn, checkOut)) {
            if (bookingManager.isRoomAvailable(room.getRoomId(), checkIn, checkOut)) {
                availableRooms.add(room);
            }
        }
        if (availableRooms.isEmpty()) {
            System.out.println("Không có phòng trống trong khoảng ngày đã chọn.");
            return null;
        }
        roomManager.displayList(availableRooms);
        int count = group ? readPositiveInt("Số phòng: ") : 1;
        if (count > availableRooms.size()) {
            System.out.println("Số phòng yêu cầu vượt số phòng đang khả dụng.");
            return null;
        }
        BookingDetail[] details = new BookingDetail[count];
        Set<String> selectedRooms = new HashSet<>();

        for (int i = 0; i < count; i++) {
            while (true) {
                String roomId = readNonEmpty("Mã phòng thứ " + (i + 1) + " (0 để hủy): ")
                        .toUpperCase();
                if ("0".equals(roomId)) return null;
                Room room = roomManager.findById(roomId);
                boolean available = room != null && room.isActive()
                        && room.getCapacity() > 0
                        && roomManager.isRoomAvailable(roomId, checkIn, checkOut)
                        && bookingManager.isRoomAvailable(roomId, checkIn, checkOut);
                if (!available || selectedRooms.contains(roomId)) {
                    System.out.println("Phòng không tồn tại, không khả dụng hoặc đã được chọn.");
                    continue;
                }
                int guests = readPositiveInt("Số khách ở phòng này: ");
                if (guests > room.getCapacity()) {
                    System.out.println("Số khách vượt sức chứa phòng (" + room.getCapacity() + ").");
                    continue;
                }
                double unitPrice = room.calculateRoomPrice(1);
                details[i] = new BookingDetail(bookingManager.generateDetailId(), requestId,
                        roomId, guests, unitPrice, 0, checkIn, checkOut, null);
                selectedRooms.add(roomId);
                break;
            }
        }
        return details;
    }

    private void editPendingRequest() {
        String requestId = readNonEmpty("Mã phiếu yêu cầu cần sửa: ").toUpperCase();
        BookingRequest request = bookingManager.findRequestById(requestId);
        if (request == null || request.getRequestStatus() != RequestStatus.PENDING_DEPOSIT
                || request.isCancellationPending()) {
            System.out.println("Chỉ sửa được yêu cầu chưa thu cọc và chưa gửi đề nghị hủy.");
            return;
        }
        LocalDate[] dates = readBookingDates();
        if (dates == null) return;
        BookingDetail[] updatedDetails = new BookingDetail[request.getDetailCount()];
        Set<String> selectedRooms = new HashSet<>();
        for (int i = 0; i < updatedDetails.length; i++) {
            while (true) {
                ArrayList<Room> availableRooms = new ArrayList<>();
                for (Room candidate : roomManager.findAvailableRooms(dates[0], dates[1])) {
                    if (bookingManager.isRoomAvailable(candidate.getRoomId(), dates[0], dates[1],
                            requestId) && !selectedRooms.contains(candidate.getRoomId())) {
                        availableRooms.add(candidate);
                    }
                }
                if (availableRooms.isEmpty()) {
                    System.out.println("Không còn phòng trống cho phiếu này trong khoảng ngày đã chọn.");
                    return;
                }
                roomManager.displayList(availableRooms);
                String roomId = readNonEmpty("Mã phòng thứ " + (i + 1) + " (0 để hủy): ")
                        .toUpperCase();
                if ("0".equals(roomId)) return;
                Room room = roomManager.findById(roomId);
                if (room == null || !availableRooms.contains(room)) {
                    System.out.println("Phòng không nằm trong danh sách khả dụng.");
                    continue;
                }
                int guests = readPositiveInt("Số khách: ");
                if (guests > room.getCapacity()) {
                    System.out.println("Số khách vượt sức chứa phòng (" + room.getCapacity() + ").");
                    continue;
                }
                BookingDetail current = request.getDetails()[i];
                updatedDetails[i] = new BookingDetail(current.getBookingDetailId(), requestId,
                        roomId, guests, room.calculateRoomPrice(1), 0, dates[0], dates[1], current.getNotes());
                selectedRooms.add(roomId);
                break;
            }
        }
        if (bookingManager.updatePendingRequest(requestId, updatedDetails, dates[0], dates[1])) {
            System.out.println("Đã cập nhật phiếu yêu cầu.");
        } else {
            System.out.println("Không cập nhật được phiếu yêu cầu.");
        }
    }

    private void cancelDirectBooking() {
        String bookingId = readNonEmpty("Mã Booking trực tiếp cần hủy: ").toUpperCase();
        Booking booking = bookingManager.findBookingById(bookingId);
        if (booking == null || booking.getRequestId() != null) {
            System.out.println("Không tìm thấy Booking trực tiếp.");
            return;
        }
        if (!confirm("Hủy Booking " + bookingId + "?")) return;
        if (bookingManager.cancelBookingById(bookingId)) {
            System.out.println("Đã hủy Booking.");
        } else {
            System.out.println("Booking không còn ở trạng thái có thể hủy.");
        }
    }

    private void receiveDeposit() {
        String requestId = readNonEmpty("Mã phiếu yêu cầu: ").toUpperCase();
        BookingRequest request = bookingManager.findRequestById(requestId);
        if (request == null || request.getRequestStatus() != RequestStatus.PENDING_DEPOSIT
                || request.isCancellationPending()) {
            System.out.println("Không tìm thấy phiếu đang chờ cọc.");
            return;
        }
        double amount = readPositiveDouble("Số tiền cọc: ");
        item("Phương thức: 1. Tiền mặt  2. Thẻ  3. Chuyển khoản");
        PaymentMethod method;
        switch (readChoice()) {
            case 1: method = PaymentMethod.CASH; break;
            case 2: method = PaymentMethod.CARD; break;
            case 3: method = PaymentMethod.TRANSFER; break;
            default:
                System.out.println("Phương thức không hợp lệ.");
                return;
        }
        String referenceNo = readOptional("Mã giao dịch (Enter nếu không có): ");
        Deposit deposit = bookingManager.processDeposit(requestId, amount, method,
                currentUser.getEmployeeId(), referenceNo);
        if (deposit == null) {
            System.out.println("Không thu được cọc cho phiếu này.");
            return;
        }
        System.out.printf("Đã thu cọc %s: %,.0f. Phiếu chuyển sang CONFIRMED.%n",
                deposit.getDepositId(), deposit.getAmount());
    }

    private void requestBookingCancellation() {
        String requestId = readNonEmpty("Mã phiếu cần đề nghị hủy: ").toUpperCase();
        String reason = readNonEmpty("Lý do đề nghị hủy: ");
        if (bookingManager.requestCancellation(requestId, reason)) {
            System.out.println("Đã gửi đề nghị hủy, chờ quản lý duyệt.");
        } else {
            System.out.println("Phiếu không tồn tại, đã hết hiệu lực hoặc đang có đề nghị hủy.");
        }
    }

    private void convertRequestToBooking() {
        String requestId = readNonEmpty("Mã phiếu đã cọc: ").toUpperCase();
        BookingRequest request = bookingManager.findRequestById(requestId);
        if (request == null || request.getRequestStatus() != RequestStatus.CONFIRMED) {
            System.out.println("Chỉ lập Booking từ phiếu đã xác nhận cọc.");
            return;
        }
        Booking booking = bookingManager.convertRequestToBooking(requestId,
                currentUser.getEmployeeId(), readOptional("Ghi chú (Enter nếu không có): "));
        if (booking == null) {
            System.out.println("Không lập được Booking từ phiếu yêu cầu.");
            return;
        }
        System.out.println("Đã lập Booking " + booking.getBookingId() + ".");
    }

    private void showBookingList() {
        printTitle("DANH SÁCH ĐẶT PHÒNG");
        Booking[] bookings = bookingManager.getAllBookings();
        if (bookings.length == 0) {
            System.out.println("(Chưa có Booking)");
        } else {
            System.out.printf("%-9s %-9s %-10s %-12s %-12s %-12s %-14s%n",
                    "Mã", "Loại", "Khách", "Nhận", "Trả", "Trạng thái", "Phòng");
            for (Booking booking : bookings) {
                String roomIds;
                if (booking instanceof hotel.model.IndividualBooking) {
                    roomIds = ((hotel.model.IndividualBooking) booking).getRoomId();
                } else {
                    roomIds = String.join(",", ((hotel.model.GroupBooking) booking).getRoomIds());
                }
                System.out.printf("%-9s %-9s %-10s %-12s %-12s %-12s %-14s%n",
                        booking.getBookingId(), booking.getRequestId() == null ? "Trực tiếp" : "Từ phiếu",
                        booking.getCustomerId(), DATE_FORMAT.format(booking.getCheckInDate()),
                        DATE_FORMAT.format(booking.getExpectedCheckOutDate()), booking.getStatus(), roomIds);
            }
        }

        List<BookingRequest> requests = bookingManager.getAllRequests();
        if (!requests.isEmpty()) {
            item("Phiếu yêu cầu");
            for (BookingRequest request : requests) {
                Deposit deposit = bookingManager.findDepositByRequestId(request.getRequestId());
                System.out.printf("%s | Khách %s | %s - %s | %s | Cọc: %s%n",
                        request.getRequestId(), request.getCustomerId(),
                        DATE_FORMAT.format(request.getExpectedCheckInDate()),
                        DATE_FORMAT.format(request.getExpectedCheckOutDate()), request.getRequestStatus(),
                        deposit == null ? "chưa thu" : String.format("%,.0f (%s)",
                                deposit.getAmount(), deposit.getStatus()));
            }
        }
    }

    private void showCancellationRequests() {
        List<BookingRequest> requests = bookingManager.getCancellationRequests();
        if (requests.isEmpty()) {
            System.out.println("(Không có đề nghị hủy đang chờ duyệt)");
            return;
        }
        for (BookingRequest request : requests) {
            System.out.printf("%s | Khách %s | %s - %s | Lý do: %s%n",
                    request.getRequestId(), request.getCustomerId(),
                    DATE_FORMAT.format(request.getExpectedCheckInDate()),
                    DATE_FORMAT.format(request.getExpectedCheckOutDate()), request.getCancelReason());
        }
    }

    private void approveBookingCancellation() {
        String requestId = readNonEmpty("Mã phiếu cần duyệt hủy: ").toUpperCase();
        BookingRequest request = bookingManager.findRequestById(requestId);
        if (request == null || !request.isCancellationPending()) {
            System.out.println("Không có đề nghị hủy đang chờ duyệt cho phiếu này.");
            return;
        }
        Deposit deposit = bookingManager.findDepositByRequestId(requestId);
        boolean refund = false;
        if (deposit != null) {
            item(String.format("Tiền cọc %s: %,.0f (%s)", deposit.getDepositId(),
                    deposit.getAmount(), deposit.getStatus()));
            item("1. Hoàn cọc  2. Mất cọc");
            int decision = readChoice();
            if (decision != 1 && decision != 2) {
                System.out.println("Lựa chọn không hợp lệ.");
                return;
            }
            refund = decision == 1;
        }
        if (!confirm("Xác nhận duyệt hủy phiếu " + requestId + "?")) return;
        if (bookingManager.cancelBookingRequest(requestId, request.getCancelReason(), refund, deposit)) {
            System.out.println("Đã duyệt hủy phiếu."
                    + (deposit == null ? "" : refund ? " Đã ghi nhận hoàn cọc." : " Đã ghi nhận mất cọc."));
        } else {
            System.out.println("Không duyệt được phiếu.");
        }
    }

    private void rejectBookingCancellation() {
        String requestId = readNonEmpty("Mã phiếu cần từ chối đề nghị hủy: ").toUpperCase();
        if (bookingManager.rejectCancellation(requestId)) {
            System.out.println("Đã từ chối đề nghị hủy.");
        } else {
            System.out.println("Không có đề nghị hủy đang chờ duyệt cho phiếu này.");
        }
    }
 
    // ---------- MODULE P4: LƯU TRÚ ----------
    private void showStayMenu() {
        while (true) {
            printTitle("LƯU TRÚ");
            printMenu("1. Nhận phòng", "2. Ghi dịch vụ", "3. Đổi phòng", "4. Trả phòng");
            item("0. Quay lại");
 
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
            printMenu("1. Lập hóa đơn", "2. Thu tiền", "3. Ghi phụ thu", "4. Xem và in hóa đơn",
                    canEdit() ? "5. Áp giảm giá" : null, canEdit() ? "6. Hủy hóa đơn" : null,
                    canEdit() ? "7. Hoàn tiền" : null);
            item("0. Quay lại");
 
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
            printMenu("1. Doanh thu theo kỳ", "2. Công suất phòng", "3. Dịch vụ bán chạy");
            item("0. Quay lại");
 
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
 
    private String pad() {
        return INDENT.repeat(depth);
    }

    private void openSub(Runnable menu) {
        depth++;
        try {
            menu.run();
        } finally {
            depth--;
        }
    }

    private void item(String text) {
        System.out.println(pad() + text);
    }

    private void printMenu(String... items) {
        List<String> list = new ArrayList<>();
        for (String text : items) {
            if (text != null) list.add(text);
        }
        if (list.size() < 4) {
            for (String text : list) item(text);
            return;
        }
        int rows = (list.size() + 1) / 2;
        int width = 0;
        for (int i = 0; i < rows; i++) {
            width = Math.max(width, list.get(i).length());
        }
        width += 4;
        for (int i = 0; i < rows; i++) {
            String left = list.get(i);
            String right = (i + rows < list.size()) ? list.get(i + rows) : "";
            item(String.format("%-" + width + "s%s", left, right).stripTrailing());
        }
    }

    private void printTitle(String title) {
        String text = " " + title + " ";
        int total = FRAME_WIDTH - pad().length();
        int fill = Math.max(0, total - text.length());
        int left = fill / 2;
        int right = fill - left;

        System.out.println();
        System.out.println(pad() + "=".repeat(left) + text + "=".repeat(right));
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
            System.out.print(pad() + "Chọn: ");
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

    private String readOptional(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(s);
                CheckingExpression.checkPositiveInt(value, "Giá trị phải là số nguyên dương");
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập số nguyên.");
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                double value = Double.parseDouble(s);
                CheckingExpression.checkPositiveDouble(value, "Giá trị phải lớn hơn 0");
                if (!Double.isFinite(value)) {
                    throw new IllegalArgumentException("Giá trị phải là số hữu hạn");
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập một số.");
            } catch (IllegalArgumentException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
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

    private String readIdentityOptional() {
        while (true) {
            System.out.print("CCCD/Hộ chiếu (Enter nếu là trẻ em chưa có): ");
            String value = sc.nextLine().trim();
            try {
                CheckingExpression.checkValidIdentityNumber(value, true);
                return value;
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
