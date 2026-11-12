package hotel.ui;

import hotel.service.AccountManager;
import hotel.service.CustomerManager;
import hotel.service.EmployeeManager;

public final class MainApp {
    private MainApp() {
    }

    public static void main(String[] args) {
        EmployeeManager employeeManager = new EmployeeManager();
        AccountManager accountManager = new AccountManager(employeeManager);
        CustomerManager customerManager = new CustomerManager();

        employeeManager.readFile();
        accountManager.readFile();
        customerManager.readFile();

        try {
            accountManager.ensureDefaultAdmin();
            new ConsoleMenu(employeeManager, accountManager, customerManager).run();
        } finally {
            employeeManager.writeFile();
            accountManager.writeFile();
            customerManager.writeFile();
        }
    }
}
