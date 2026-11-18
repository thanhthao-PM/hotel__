package hotel.ui;

import hotel.model.Account;
import hotel.model.Customer;
import hotel.model.Employee;
import hotel.model.GuestMember;
import hotel.model.Person;
import hotel.model.enums.Gender;
import hotel.model.enums.Role;
import hotel.service.AccountManager;
import hotel.service.CustomerManager;
import hotel.service.EmployeeManager;
import hotel.service.InvoiceManager;
import hotel.service.ReportService;
import hotel.util.CheckingExpression;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// P4 thêm
import hotel.model.CheckInReceipt;
import hotel.model.CheckOutReceipt;
import hotel.model.ServiceUsage;
import hotel.model.StayRecord;
import hotel.service.StayLinks;
import hotel.service.StayManager;
// P4 hết ---

/**
 * TOÀN BỘ MENU nằm trong file này. Mỗi menu là một hàm showXxxMenu().
 *
 * Cách đọc file:
 *   run()            -> vòng lặp chính: đăng nhập -> menu theo Role -> đăng xuất
 *   showAdminMenu / showManagerMenu / showEmployeeMenu   -> menu chính của từng Role (có switch-case)
 *   Phần "P1"        -> menu con của module Người, Tài khoản (đã làm)
 *   Phần "P2"        -> menu Phòng, Dịch vụ, Bảo trì phòng (đã làm)
 *   Phần "KHUNG P3..P5" -> menu con của các bạn khác: chỉ cần thay notReady(...) bằng lệnh gọi Manager của mình
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
    
    private final InvoiceManager invoiceManager; // THÊM 2 DÒNG NÀY: để gọi nghiệp vụ và báo cáo
    private final ReportService reportService; // THÊM 2 DÒNG NÀY: để gọi nghiệp vụ và báo cáo
    // P4 thêm --
    private StayManager stayManager;
    // P4 hết ---
    // TODO(P2..P5): thêm RoomManager, BookingManager... vào đây và vào constructor

    private Account currentUser;                         // người đang đăng nhập (null = chưa đăng nhập)
    private int depth = 0;

    // P4 thêm --
    public void setStayManager(StayManager stayManager) { this.stayManager = stayManager; }
    // P4 hết ---

    public ConsoleMenu(AccountManager accountManager,
                       EmployeeManager employeeManager,
                       CustomerManager customerManager,
                       RoomManager roomManager,
                       ServiceManager serviceManager,
                       InvoiceManager invoiceManager,
                       ReportService reportService) {
        
        private Account currentUser;                         // người đang đăng nhập (null = chưa đăng nhập)
        private int depth = 0;
        this.accountManager = accountManager;
        this.employeeManager = employeeManager;
        this.customerManager = customerManager;
        this.roomManager = roomManager;
        this.serviceManager = serviceManager;
        // THÊM 2 DÒNG NÀY:
        this.invoiceManager = invoiceManager; // để gọi nghiệp vụ và báo cáo
        this.reportService = reportService;
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
        // P4 thêm --
        if (stayManager != null) stayManager.writeFile();
        // P4 hết --
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
                case 2: notReady("Duyệt giảm giá (P5)");      break;
                case 3: notReady("Hủy hóa đơn (P5)");         break; 
                case 4: notReady("Hoàn tiền (P5)");           break;  
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
                    // [P2] Mục 1: mở menu Phòng (EMPLOYEE chỉ thấy các mục xem, tìm, dọn phòng, báo hỏng)
                    case 1:
                        openSub(this::showRoomMenu);
                        break;

                    case 2: openSub(() -> showPostListMenu(false));    break;
                    case 3:
                        if (isEmployeeRole()) noPermission();        
                        else openSub(() -> showPostListMenu(true));
                        break;
                    case 4: notReady("Danh sách đặt phòng (P3)");    break;   // TODO(P3)
                    // P4 thêm --
                    case 5:
                        if (stayManager == null) notReady("Khách đang lưu trú");
                        else stayManager.displayActiveGuests();
                        break;
                    // P4 hết --
                    case 6: displayInvoices();                       break;    // TODO(P5)

                    // [P2] Mục 7: mở menu Dịch vụ (EMPLOYEE chỉ xem)
                    case 7:
                        openSub(this::showServiceMenu);
                        break;

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
    // ############################################################

    // ---------- MODULE P2: PHÒNG ----------
    private void showRoomMenu() {
        while (true) {
            printTitle("PHÒNG");

            // Dựng danh sách mục menu: mục nào cần quyền thì chỉ thêm khi canEdit() = true
            List<String> menuItems = new ArrayList<>();
            menuItems.add("1. Danh sách phòng");
            menuItems.add("2. Tìm phòng trống");
            menuItems.add("3. Tìm kiếm / lọc");
            menuItems.add("4. Sắp xếp theo giá");
            menuItems.add("5. Đổi trạng thái (dọn phòng)");
            if (canEdit()) {
                menuItems.add("6. Thêm phòng");
                menuItems.add("7. Sửa phòng");
                menuItems.add("8. Xóa phòng");
                menuItems.add("9. Đổi giá phòng");
            }
            menuItems.add("10. Bảo trì phòng");

            printMenu(menuItems.toArray(new String[0]));
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;

            try {
                switch (choice) {
                    case 1: roomManager.displayAll();   break;
                    case 2: findFreeRooms();    break;
                    case 3: searchRooms();  break;
                    case 4: sortRooms();    break;
                    case 5: changeRoomStatus(); break;
                    // Kiểm tra quyền ngay trong xử lý, không chỉ dựa vào việc ẩn menu
                    case 6:
                        if (canEdit())    addRoom();  
                        else   noPermission(); 
                        break;
                    case 7:
                        if (canEdit())     editRoom(); 
                        else   noPermission(); 
                        break;
                    case 8:
                        if (canEdit())     removeRoom();   
                        else   noPermission(); 
                        break;
                    case 9:
                        if (canEdit())  changeRoomPrice(); 
                        else   noPermission(); 
                        break;
                    case 10:    openSub(this::showMaintenanceMenu);     break;
                    default:
                        System.out.println("Lựa chọn không hợp lệ.");
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

            List<String> menuItems = new ArrayList<>();
            menuItems.add("1. Danh sách dịch vụ");
            menuItems.add("2. Tìm kiếm");
            menuItems.add("3. Sắp xếp theo giá");
            if (canEdit()) {
                menuItems.add("4. Thêm dịch vụ");
                menuItems.add("5. Sửa dịch vụ");
                menuItems.add("6. Đổi đơn giá");
                menuItems.add("7. Ngưng cung cấp");
                menuItems.add("8. Cung cấp lại");
            }
            printMenu(menuItems.toArray(new String[0]));
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;

            try {
                switch (choice) {
                    case 1:     serviceManager.displayAll();    break;
                    case 2:     searchServices();   break;
                    case 3:     sortServices();     break;
                    // Kiểm tra quyền ngay trong xử lý: EMPLOYEE chỉ được xem
                    case 4:
                        if (canEdit())     addService();   
                        else       noPermission();  
                        break;
                    case 5:
                        if (canEdit())     editService();  
                        else    noPermission();
                        break;
                    case 6:
                        if (canEdit()) {
                            changeServicePrice();
                        } else {
                            noPermission();
                        }
                        break;

                    case 7:
                        if (canEdit()) {
                            deactivateService();
                        } else {
                            noPermission();
                        }
                        break;

                    case 8:
                        if (canEdit()) {
                            activateService();
                        } else {
                            noPermission();
                        }
                        break;

                    default:
                        System.out.println("Lựa chọn không hợp lệ.");
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

            List<String> menuItems = new ArrayList<>();
            menuItems.add("1. Danh sách bảo trì");
            menuItems.add("2. Tạo bản ghi bảo trì (báo hỏng)");
            if (canEdit()) {
                menuItems.add("3. Đóng bản ghi bảo trì");
            }
            printMenu(menuItems.toArray(new String[0]));
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;

            try {
                switch (choice) {
                    case 1:
                        roomManager.displayMaintenances();
                        break;

                    case 2:
                        reportMaintenance();
                        break;

                    case 3:
                        // EMPLOYEE chỉ được báo hỏng, không được đóng bản ghi
                        if (canEdit()) {
                            closeMaintenance();
                        } else {
                            noPermission();
                        }
                        break;

                    default:
                        System.out.println("Lựa chọn không hợp lệ.");
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
                    "3. Đặt phòng trực tiếp - đoàn", "4. Sửa đặt phòng", "5. Hủy đặt phòng",
                    "6. Thu cọc", "7. Gửi đề nghị hủy (chờ quản lý duyệt)");
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;

            try {
                switch (choice) {
                    case 1: notReady("Nhận yêu cầu đặt phòng qua điện thoại"); break;   
                    case 2: notReady("Đặt phòng trực tiếp - cá nhân"); break;          
                    case 3: notReady("Đặt phòng trực tiếp - đoàn"); break;             
                    case 4: notReady("Sửa đặt phòng"); break;                          
                    case 5: notReady("Hủy đặt phòng"); break;                          
                    case 6: notReady("Thu cọc"); break;                                
                    case 7: notReady("Gửi đề nghị hủy (chờ quản lý duyệt)"); break;    
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
            printMenu("1. Danh sách đề nghị hủy chờ duyệt", "2. Duyệt hủy phiếu",
                    "3. Quyết định hoàn hoặc mất cọc", "4. Từ chối đề nghị hủy");
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;

            try {
                switch (choice) {
                    case 1: notReady("Danh sách đề nghị hủy chờ duyệt"); break;   
                    case 2: notReady("Duyệt hủy phiếu"); break;                   
                    case 3: notReady("Quyết định hoàn hoặc mất cọc"); break;      
                    case 4: notReady("Từ chối đề nghị hủy"); break;               
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }
    // P4 thêm --
    // ---------- MODULE P4: LƯU TRÚ ----------
    private void showStayMenu() {
        if (stayManager == null) { notReady("Lưu trú (P4)"); return; }
        while (true) {
            printTitle("LƯU TRÚ");
            printMenu("1. Nhận phòng", "2. Ghi dịch vụ", "3. Sửa hoặc hủy dịch vụ", "4. Đổi phòng",
                    "5. Gia hạn", "6. Trả phòng", "7. Khách không đến (no-show)", "8. Tra cứu và thống kê",
                    canEdit() ? "9. Xóa lượt ở" : null);
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;

            try {
                switch (choice) {
                    case 1: stayCheckIn();      break;
                    case 2: stayAddUsage();     break;
                    case 3: stayEditUsage();    break;
                    case 4: stayChangeRoom();   break;
                    case 5: stayExtend();       break;
                    case 6: stayCheckOut();     break;
                    case 7: stayNoShow();       break;
                    case 8: stayLookup();       break;
                    case 9:
                        if (!canEdit()) noPermission();
                        else stayRemove();
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void stayCheckIn() {
        String code = readNonEmpty("Mã phiếu yêu cầu hoặc mã đặt phòng: ").toUpperCase();
        StayLinks.BookingLine[] lines = stayManager.getLines(code);
        if (lines.length == 0) { System.out.println("Không tìm thấy phiếu " + code); return; }
        System.out.println("Các phòng trong phiếu:");
        for (StayLinks.BookingLine l : lines) {
            System.out.printf("  %-6s phòng %-5s %s -> %s, %d khách %s%n", l.bookingDetailId, l.roomId,
                    l.checkInDate.format(DATE_FORMAT), l.checkOutDate.format(DATE_FORMAT), l.guestCount,
                    stayManager.hasStayForLine(l.bookingDetailId) ? "[ĐÃ NHẬN]" : "");
        }
        String detailId = readNonEmpty("Mã dòng cần nhận phòng: ").toUpperCase();
        String[] guests = readNonEmpty("Mã khách ở phòng này (cách nhau dấu phẩy): ")
                .toUpperCase().split("\\s*,\\s*");
        System.out.print("Ghi chú (có thể bỏ trống): ");
        String note = sc.nextLine().trim();

        StayRecord s = stayManager.checkIn(code, detailId, guests, currentUser.getEmployeeId(), note);
        System.out.println("Nhận phòng thành công, lượt ở " + s.getStayId());
        stayManager.findCheckInReceipt(s.getStayId()).printReceipt();
    }

    private void stayAddUsage() {
        String stayId = readNonEmpty("Mã lượt ở: ").toUpperCase();
        String serviceId = readNonEmpty("Mã dịch vụ: ").toUpperCase();
        double qty = readNonNegativeDouble("Số lượng: ");
        ServiceUsage u = stayManager.addServiceUsage(stayId, serviceId, qty, currentUser.getEmployeeId());
        System.out.println("Đã ghi: " + u);
    }

    private void stayEditUsage() {
        String stayId = readNonEmpty("Mã lượt ở: ").toUpperCase();
        stayManager.displayUsages(stayId);
        String usageId = readNonEmpty("Mã dòng dịch vụ: ").toUpperCase();
        item("1. Sửa số lượng   2. Hủy dòng");
        int c = readChoice();
        if (c == 1) {
            stayManager.updateServiceUsage(stayId, usageId, readNonNegativeDouble("Số lượng mới: "));
            System.out.println("Đã sửa.");
        } else if (c == 2) {
            if (!confirm("Hủy dòng " + usageId + "?")) return;
            boolean needP5 = stayManager.cancelServiceUsage(stayId, usageId, currentUser.getRole());
            System.out.println("Đã hủy." + (needP5 ? " Hóa đơn đã lập: cần P5 điều chỉnh hoặc hoàn tiền." : ""));
        } else {
            System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private void stayChangeRoom() {
        String stayId = readNonEmpty("Mã lượt ở: ").toUpperCase();
        String room = readNonEmpty("Mã phòng mới: ").toUpperCase();
        String reason = readNonEmpty("Lý do: ");
        if (!confirm("Đổi phòng?")) return;
        StayRecord s = stayManager.changeRoom(stayId, room, currentUser.getEmployeeId(), reason);
        System.out.println("Đã đổi phòng, lượt ở mới " + s.getStayId());
    }

    private void stayExtend() {
        String stayId = readNonEmpty("Mã lượt ở: ").toUpperCase();
        LocalDate d = readDate("Ngày trả mới (dd/MM/yyyy): ");
        System.out.println(stayManager.extendStay(stayId, d, currentUser.getEmployeeId())
                ? "Đã gia hạn." : "Không gia hạn được (phòng đã có người đặt kế tiếp).");
    }

    private void stayCheckOut() {
        String stayId = readNonEmpty("Mã lượt ở: ").toUpperCase();
        String cond = readNonEmpty("Tình trạng phòng: ");
        System.out.print("Ghi chú (có thể bỏ trống): ");
        String note = sc.nextLine().trim();
        if (!confirm("Trả phòng của lượt ở " + stayId + "?")) return;

        StayManager.CheckOutResult r = stayManager.checkOut(stayId, currentUser.getEmployeeId(), cond, note);
        r.receipt.printReceipt();
        System.out.println(r.invoiceId == null ? "Chưa có hóa đơn." : "Mã hóa đơn: " + r.invoiceId);
        if (r.bookingCompleted) System.out.println("Tất cả phòng của Booking đã trả: Booking COMPLETED.");
        if (r.warning != null) System.out.println("[CẢNH BÁO] " + r.warning);
    }

    private void stayNoShow() {
        String code = readNonEmpty("Mã phiếu yêu cầu hoặc mã đặt phòng: ").toUpperCase();
        if (!confirm("Đánh dấu NO_SHOW cho " + code + "? (tiền cọc sẽ do quản lý xử lý)")) return;
        stayManager.processNoShow(code, currentUser.getEmployeeId());
        System.out.println("Đã chuyển NO_SHOW.");
    }

    private void stayLookup() {
        printMenu("1. Khách đang ở", "2. Tìm kiếm", "3. Lịch sử lưu trú theo khách",
                "4. Dịch vụ của một lượt ở", "5. Tất cả (mới nhất trước)", "6. Thống kê",
                "7. Xem phiếu của một lượt ở");
        switch (readChoice()) {
            case 1: stayManager.displayActiveGuests(); break;
            case 2: stayManager.displayList(stayManager.search(readNonEmpty("Từ khóa: "))); break;
            case 3: stayManager.displayList(stayManager.findByGuest(readNonEmpty("Mã khách: ").toUpperCase())); break;
            case 4: stayManager.displayUsages(readNonEmpty("Mã lượt ở: ").toUpperCase()); break;
            case 5: stayManager.displayList(stayManager.sortedByCheckIn(true)); break;
            case 6: stayManager.printStatistics(); break;
            case 7:
                String id = readNonEmpty("Mã lượt ở: ").toUpperCase();
                CheckInReceipt ci = stayManager.findCheckInReceipt(id);
                CheckOutReceipt co = stayManager.findCheckOutReceipt(id);
                if (ci != null) ci.printReceipt(); else System.out.println("(Chưa có phiếu nhận)");
                if (co != null) co.printReceipt(); else System.out.println("(Chưa có phiếu trả)");
                break;
            default: System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private void stayRemove() {
        String id = readNonEmpty("Mã lượt ở cần xóa: ").toUpperCase();
        if (!confirm("Xóa lượt ở " + id + " cùng phiếu và dịch vụ kèm theo?")) return;
        System.out.println(stayManager.remove(id) ? "Đã xóa."
                : "Không xóa được (không tồn tại, đang ở, hoặc đã có hóa đơn).");
    }
    // P4 hết --

    // ---------- MODULE P5: THANH TOÁN ----------
    private void showPaymentMenu() {
        while (true) {
            printTitle("THANH TOÁN");
            printMenu("1. Lập hóa đơn", "2. Thu tiền", "3. Ghi phụ thu", "4. Xem chi tiết hóa đơn",
                    canEdit() ? "5. Áp giảm giá" : null, canEdit() ? "6. Hủy hóa đơn" : null,
                    canEdit() ? "7. Hoàn tiền" : null);
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;

            try {
                switch (choice) {
                    case 1: createInvoice(); break;
                    case 2: collectPayment(); break;
                    case 3: addExtraCharge(); break;
                    case 4: viewInvoice(); break;
                    case 5:
                        if (!canEdit()) noPermission();
                        else applyDiscount();
                        break;
                    case 6:
                        if (!canEdit()) noPermission();
                        else cancelInvoice();
                        break;
                    case 7:
                        if (!canEdit()) noPermission();
                        else refundMoney();
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
            printMenu("1. Doanh thu tổng", "2. Công suất phòng", "3. Dịch vụ bán chạy");
            item("0. Quay lại");

            int choice = readChoice();
            if (choice == 0) return;

            try {
                switch (choice) {
                    case 1: showRevenueReport(); break;
                    case 2: notReady("Công suất phòng"); break;   
                    case 3: notReady("Dịch vụ bán chạy"); break;   
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

    // ============================================================
    // MODULE P5 - INVOICE & PAYMENT (TRÂM)
    // ============================================================

    private void displayInvoices() {
        System.out.println("--- DANH SÁCH HÓA ĐƠN ---");
        hotel.model.Invoice[] list = invoiceManager.getAllInvoices();
        int count = invoiceManager.getCount();
        if (count == 0) {
            System.out.println("=> Chưa có hóa đơn nào.");
            return;
        }
        for (int i = 0; i < count; i++) {
            hotel.model.Invoice inv = list[i];
            System.out.println("- " + inv.getInvoiceId() + " | Lưu trú: " + inv.getStayId() + " | Trạng thái: " + inv.getStatus() + " | Nợ: " + inv.calculateRemaining());
        }
    }

    private void createInvoice() {
        System.out.println("--- LẬP HÓA ĐƠN MỚI ---");
        String stayId = readNonEmpty("Nhập mã lưu trú (Stay ID): ");
        double deposit = readNonNegativeDouble("Nhập tiền cọc đã áp dụng: ");
        
        try {
            String nv = (currentUser != null) ? currentUser.toString() : "NhanVien";
            hotel.model.Invoice inv = new hotel.model.Invoice(stayId, nv, deposit);
            invoiceManager.addInvoice(inv);
            System.out.println("=> Lập hóa đơn thành công! Mã hóa đơn: " + inv.getInvoiceId());
        } catch (Exception e) {
            System.out.println("=> Lỗi: " + e.getMessage());
        }
    }

    private void viewInvoice() {
        String invId = readNonEmpty("Nhập mã hóa đơn cần xem: ");
        hotel.model.Invoice inv = invoiceManager.findByInvoiceId(invId);
        if (inv == null) {
            System.out.println("=> Không tìm thấy hóa đơn!");
            return;
        }
        System.out.println("\n--- CHI TIẾT HÓA ĐƠN [" + invId + "] ---");
        System.out.println("Trạng thái: " + inv.getStatus());
        System.out.println("Mã lưu trú: " + inv.getStayId());
        System.out.println("Tiền cọc: " + inv.getDepositApplied());
        System.out.println("Tổng tiền: " + inv.calculateTotal());
        System.out.println("Khách đã thanh toán: " + inv.calculatePaid());
        System.out.println("-> KHÁCH CÒN NỢ: " + inv.calculateRemaining());
    }

    private void collectPayment() {
        String invId = readNonEmpty("Nhập mã hóa đơn cần thu tiền: ");
        hotel.model.Invoice inv = invoiceManager.findByInvoiceId(invId);
        if (inv == null) {
            System.out.println("=> Không tìm thấy hóa đơn!");
            return;
        }
        System.out.println("Số tiền khách còn nợ: " + inv.calculateRemaining());
        double amount = readNonNegativeDouble("Nhập số tiền khách đưa: ");

        try {
            String nv = (currentUser != null) ? currentUser.toString() : "NhanVien";
            hotel.model.Payment payment = new hotel.model.Payment("PAY" + System.currentTimeMillis(), invId, amount, hotel.model.Invoice.PaymentMethod.CASH, nv, "");
            inv.addPayment(payment);
            System.out.println("=> Thu tiền thành công! Trạng thái hóa đơn: " + inv.getStatus());
        } catch (Exception e) {
            System.out.println("=> Lỗi: " + e.getMessage());
        }
    }

    private void addExtraCharge() {
        String invId = readNonEmpty("Nhập mã hóa đơn cần phụ thu: ");
        hotel.model.Invoice inv = invoiceManager.findByInvoiceId(invId);
        if (inv == null) {
            System.out.println("=> Không tìm thấy hóa đơn!");
            return;
        }
        
        String reason = readNonEmpty("Lý do phụ thu: ");
        double amount = readNonNegativeDouble("Số tiền phụ thu: ");

        try {
            String nv = (currentUser != null) ? currentUser.toString() : "NhanVien";
            hotel.model.ExtraCharge charge = new hotel.model.ExtraCharge("EC" + System.currentTimeMillis(), invId, reason, amount, nv);
            inv.addExtraCharge(charge);
            System.out.println("=> Ghi phụ thu thành công!");
        } catch (Exception e) {
            System.out.println("=> Lỗi: " + e.getMessage());
        }
    }

    private void applyDiscount() {
        String invId = readNonEmpty("Nhập mã hóa đơn cần giảm giá: ");
        hotel.model.Invoice inv = invoiceManager.findByInvoiceId(invId);
        if (inv == null) {
            System.out.println("=> Không tìm thấy hóa đơn!");
            return;
        }
        
        String reason = readNonEmpty("Lý do giảm giá: ");
        double value = readNonNegativeDouble("Số tiền giảm: ");

        try {
            String nv = (currentUser != null) ? currentUser.toString() : "QuanLy";
            hotel.model.Discount discount = new hotel.model.Discount("DC" + System.currentTimeMillis(), invId, hotel.model.Invoice.DiscountType.FIXED, value, reason, nv);
            inv.addDiscount(discount);
            System.out.println("=> Áp dụng giảm giá thành công!");
        } catch (Exception e) {
            System.out.println("=> Lỗi: " + e.getMessage());
        }
    }

    private void cancelInvoice() {
        String invId = readNonEmpty("Nhập mã hóa đơn cần hủy: ");
        hotel.model.Invoice inv = invoiceManager.findByInvoiceId(invId);
        if (inv == null) {
            System.out.println("=> Không tìm thấy hóa đơn!");
            return;
        }
        String reason = readNonEmpty("Nhập lý do hủy hóa đơn: ");
        
        try {
            inv.cancel(reason);
            System.out.println("=> Hủy hóa đơn thành công! Trạng thái hiện tại: " + inv.getStatus());
        } catch (Exception e) {
            System.out.println("=> Lỗi: " + e.getMessage());
        }
    }

    private void refundMoney() {
        String invId = readNonEmpty("Nhập mã hóa đơn cần hoàn tiền: ");
        hotel.model.Invoice inv = invoiceManager.findByInvoiceId(invId);
        if (inv == null) {
            System.out.println("=> Không tìm thấy hóa đơn!");
            return;
        }
        double amount = readNonNegativeDouble("Nhập số tiền hoàn: ");
        String reason = readNonEmpty("Nhập lý do hoàn tiền: ");

        try {
            String nv = (currentUser != null) ? currentUser.toString() : "QuanLy";
            hotel.model.RefundRecord refund = new hotel.model.RefundRecord("REF" + System.currentTimeMillis(), invId, amount, reason, nv);
            inv.addRefund(refund);
            System.out.println("=> Đã ghi nhận hoàn tiền thành công!");
        } catch (Exception e) {
            System.out.println("=> Lỗi: " + e.getMessage());
        }
    }

    private void showRevenueReport() {
        System.out.println("--- BÁO CÁO DOANH THU ---");
        System.out.println("=> Tổng doanh thu thực tế thu được: " + reportService.calculateTotalRevenue() + " VND");
    }
}