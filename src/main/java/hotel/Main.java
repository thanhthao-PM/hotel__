package hotel;

import hotel.service.AccountManager;
import hotel.service.CustomerManager;
import hotel.service.EmployeeManager;
import hotel.ui.ConsoleMenu;

public class Main {
    public static void main(String[] args) {
        // 1. Tạo mỗi Manager ĐÚNG MỘT object, cả chương trình dùng chung
        EmployeeManager employeeManager = new EmployeeManager();
        AccountManager accountManager = new AccountManager(employeeManager);
        CustomerManager customerManager = new CustomerManager();
 
        // 2. Nạp file đúng thứ tự: Employee -> Account -> Customer
        employeeManager.readFile();
        accountManager.readFile();
        customerManager.readFile();
 
        // 3. Lần chạy đầu: tạo Admin mặc định (đã có Admin thì không làm gì)
        accountManager.ensureDefaultAdmin();
 
        // 4. Chạy menu; finally đảm bảo luôn ghi file dù thoát kiểu gì
        try {
            new ConsoleMenu(accountManager, employeeManager, customerManager).run();
        } finally {
            employeeManager.writeFile();
            accountManager.writeFile();
            customerManager.writeFile();
        }
    }
}
