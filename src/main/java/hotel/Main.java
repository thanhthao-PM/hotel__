package hotel;

import hotel.model.Account;
import hotel.model.BookingDetail;
import hotel.model.BookingRequest;
import hotel.model.Customer;
import hotel.model.Employee;
import hotel.model.GuestMember;
import hotel.model.Person;
import hotel.model.Room;
import hotel.model.StandardRoom;
import hotel.model.VipRoom;
import hotel.model.enums.Gender;
import hotel.model.enums.Role;
import hotel.model.enums.RoomStatus;
import hotel.service.AccountManager;
import hotel.service.BookingManager;
import hotel.service.CustomerManager;
import hotel.service.EmployeeManager;
import hotel.service.InvoiceManager;
import hotel.service.ReportService;
import hotel.service.RoomManager;
import hotel.ui.ConsoleMenu;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        // 1. Tạo mỗi Manager ĐÚNG MỘT object, cả chương trình dùng chung
        EmployeeManager employeeManager = new EmployeeManager();
        AccountManager accountManager = new AccountManager(employeeManager);
        CustomerManager customerManager = new CustomerManager();
        RoomManager roomManager = new RoomManager();
        BookingManager bookingManager = new BookingManager();
        InvoiceManager invoiceManager = new InvoiceManager(100);
        ReportService reportService = new ReportService(invoiceManager);
        // 2. Nạp file đúng thứ tự: Employee -> Account -> Customer
        employeeManager.readFile();
        accountManager.readFile();
        customerManager.readFile();
        roomManager.readFile();
 
        boolean freshInstall = isFreshInstall(Paths.get("data"));

        // 3. Lần chạy đầu: tạo Admin mặc định (đã có Admin thì không làm gì)
        accountManager.ensureDefaultAdmin();
        if (freshInstall) {
            seedDemoData(employeeManager, accountManager, customerManager, roomManager, bookingManager);
            System.out.println("Tài khoản mẫu: demo_manager / DemoQL123, demo_staff / DemoNV123.");
            System.out.println("Lần đăng nhập đầu tiên sẽ yêu cầu đổi mật khẩu.");
        }
 
        // 4. Chạy menu; finally đảm bảo luôn ghi file dù thoát kiểu gì
        try {
            new ConsoleMenu(accountManager, employeeManager, customerManager, invoiceManager, reportService,
                    roomManager, bookingManager).run();
        } finally {
            employeeManager.writeFile();
            accountManager.writeFile();
            customerManager.writeFile();
            invoiceManager.writeFile();
            roomManager.writeFile();
        }
    }

    static boolean isFreshInstall(Path dataDirectory) throws IOException {
        if (Files.notExists(dataDirectory)) {
            return true;
        }
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dataDirectory)) {
            for (Path entry : entries) {
                if (!".gitkeep".equals(entry.getFileName().toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void seedDemoData(EmployeeManager employees, AccountManager accounts,
                                     CustomerManager customers, RoomManager rooms,
                                     BookingManager bookings) {
        Employee manager = employees.findById("NV900");
        if (manager == null) {
            manager = new Employee("NV900", "Nguyễn Minh Anh", "0912345678", Gender.FEMALE,
                    "079123456789", LocalDate.of(1992, 4, 12), "Hà Nội", true,
                    "Quản lý", 18000000, LocalDate.of(2020, 3, 1));
            if (!employees.add(manager)) {
                throw new IllegalStateException("Không thể tạo nhân viên mẫu NV900.");
            }
        }

        Employee staff = employees.findById("NV901");
        if (staff == null) {
            staff = new Employee("NV901", "Trần Quốc Bảo", "0987654321", Gender.MALE,
                    "079123456790", LocalDate.of(1998, 8, 20), "Đà Nẵng", true,
                    "Lễ tân", 10000000, LocalDate.of(2022, 6, 15));
            if (!employees.add(staff)) {
                throw new IllegalStateException("Không thể tạo nhân viên mẫu NV901.");
            }
        }

        addDemoAccount(accounts, "demo_manager", "DemoQL123", Role.MANAGER, manager.getId());
        addDemoAccount(accounts, "demo_staff", "DemoNV123", Role.EMPLOYEE, staff.getId());

        if (customers.findById("KH900") == null
                && !customers.add(new Customer("KH900", "Lê Thu Hà", "0934567890", Gender.FEMALE,
                "079123456791", LocalDate.of(1995, 2, 10), "Hồ Chí Minh", true,
                "ha.le@example.com"))) {
            throw new IllegalStateException("Không thể tạo khách hàng mẫu KH900.");
        }
        if (customers.findById("KH901") == null
                && !customers.add(new Customer("KH901", "Phạm Gia Huy", "0945678901", Gender.MALE,
                "079123456792", LocalDate.of(1988, 11, 5), "Hải Phòng", true,
                "huy.pham@example.com"))) {
            throw new IllegalStateException("Không thể tạo khách hàng mẫu KH901.");
        }
        if (customers.findById("KH902") == null
                && !customers.add(new GuestMember("KH902", "Lê Hoàng Nam", "0934567890",
                Gender.MALE, "", LocalDate.of(2010, 7, 3), "Hồ Chí Minh", true,
                "KH900", "Con"))) {
            throw new IllegalStateException("Không thể tạo thành viên đoàn mẫu KH902.");
        }

        addDemoRoom(rooms, new StandardRoom("R901", "101", 1, 2, 750000,
                RoomStatus.AVAILABLE, true, "Đôi", 1));
        addDemoRoom(rooms, new VipRoom("R902", "201", 2, 2, 1200000,
                RoomStatus.AVAILABLE, true, 0.3, "Wifi, TV", "Thành phố"));
        addDemoRoom(rooms, new StandardRoom("R903", "102", 1, 4, 900000,
                RoomStatus.AVAILABLE, true, "Đơn", 2));
        addDemoRoom(rooms, new VipRoom("R904", "202", 2, 3, 1500000,
                RoomStatus.AVAILABLE, true, 0.25, "Wifi, bồn tắm", "Biển"));
        addDemoRoom(rooms, new StandardRoom("R905", "301", 3, 2, 650000,
                RoomStatus.AVAILABLE, true, "Đôi", 1));
        Room.syncCounter("R905");
        if (rooms.findMaintenanceByRoom("R905").isEmpty()
                && !rooms.createMaintenance("R905", LocalDate.now(), LocalDate.now().plusDays(3),
                "Bảo trì mẫu", "NV900")) {
            throw new IllegalStateException("Không thể tạo lịch bảo trì mẫu cho phòng R905.");
        }

        LocalDate individualCheckIn = LocalDate.now().plusDays(7);
        LocalDate individualCheckOut = individualCheckIn.plusDays(2);
        BookingRequest individual = new BookingRequest("REQ-DEMO-001", "KH900", "INDIVIDUAL",
                null, individualCheckIn, individualCheckOut, "NV900", 1);
        individual.addDetail(new BookingDetail("DT-DEMO-001", individual.getRequestId(), "R901",
                2, rooms.getRoomPrice("R901", 1), 0, individualCheckIn, individualCheckOut,
                "Yêu cầu đặt phòng mẫu"));
        if (!bookings.createBookingRequest(individual)) {
            throw new IllegalStateException("Không thể tạo yêu cầu đặt phòng mẫu REQ-DEMO-001.");
        }

        LocalDate groupCheckIn = LocalDate.now().plusDays(12);
        LocalDate groupCheckOut = groupCheckIn.plusDays(3);
        BookingRequest group = new BookingRequest("REQ-DEMO-002", "KH901", "GROUP",
                "Đoàn công ty mẫu", groupCheckIn, groupCheckOut, "NV901", 2);
        group.addDetail(new BookingDetail("DT-DEMO-002", group.getRequestId(), "R902",
                2, rooms.getRoomPrice("R902", 1), 0, groupCheckIn, groupCheckOut,
                "Phòng VIP"));
        group.addDetail(new BookingDetail("DT-DEMO-003", group.getRequestId(), "R903",
                3, rooms.getRoomPrice("R903", 1), 0, groupCheckIn, groupCheckOut,
                "Phòng tiêu chuẩn"));
        if (!bookings.createBookingRequest(group)) {
            throw new IllegalStateException("Không thể tạo yêu cầu đặt phòng mẫu REQ-DEMO-002.");
        }

        Person.syncNextNumber(902);
    }

    private static void addDemoAccount(AccountManager accounts, String username, String password,
                                       Role role, String employeeId) {
        if (accounts.findById(username) == null
                && !accounts.add(new Account(username, password, role, employeeId))) {
            throw new IllegalStateException("Không thể tạo tài khoản mẫu " + username + ".");
        }
    }

    private static void addDemoRoom(RoomManager rooms, Room room) {
        if (rooms.findById(room.getRoomId()) == null && !rooms.add(room)) {
            throw new IllegalStateException("Không thể tạo phòng mẫu " + room.getRoomId() + ".");
        }
    }
}
