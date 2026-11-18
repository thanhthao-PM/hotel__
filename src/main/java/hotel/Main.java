package hotel;

import hotel.service.AccountManager;
import hotel.service.CustomerManager;
import hotel.service.EmployeeManager;
import hotel.service.InvoiceManager;
import hotel.service.ReportService;
import hotel.ui.ConsoleMenu;

public class Main {
    public static void main(String[] args) {
        // 1. Tạo mỗi Manager ĐÚNG MỘT object, cả chương trình dùng chung
        EmployeeManager employeeManager = new EmployeeManager();
        AccountManager accountManager = new AccountManager(employeeManager);
        CustomerManager customerManager = new CustomerManager();
        InvoiceManager invoiceManager = new InvoiceManager(100);
        ReportService reportService = new ReportService(invoiceManager);
        // 2. Nạp file đúng thứ tự: Employee -> Account -> Customer
        employeeManager.readFile();
        accountManager.readFile();
        customerManager.readFile();
        invoiceManager.readFile();
 
        // 3. Lần chạy đầu: tạo Admin mặc định (đã có Admin thì không làm gì)
        accountManager.ensureDefaultAdmin();
 
        // 4. Chạy menu; finally đảm bảo luôn ghi file dù thoát kiểu gì
        try {
            new ConsoleMenu(accountManager, employeeManager, customerManager, invoiceManager, reportService).run();
        } finally {
            employeeManager.writeFile();
            accountManager.writeFile();
            customerManager.writeFile();
            invoiceManager.writeFile();
        }
    }
}
