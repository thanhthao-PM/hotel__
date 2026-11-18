package hotel.model;

import java.time.LocalDate;
import hotel.util.CheckingExpression;
import hotel.model.enums.Gender;

public class Employee extends Person {

    private static final String PREFIX   = "EM";        // tiền tố mã: EM001, EM002...
    private static final String TYPE_TAG = "EMPLOYEE";  // nhãn loại ghi ở đầu dòng file
    private static final String SEP      = "|";         // ký tự ngăn cách

    private String position;
    private double salary;
    private LocalDate hireDate;

    public void setPosition(String position) {
        CheckingExpression.checkNotNullOrEmpty(position, "Chức vụ không được để trống");
        this.position = position.trim();
    }

    public void setSalary(double salary) {
        CheckingExpression.checkNonNegativeDouble(salary, "Lương phải là số thực không âm");
        this.salary = salary;
    }

    public void setHireDate(LocalDate hireDate) {
        if (hireDate == null) {
            throw new IllegalArgumentException("Ngày tuyển dụng không được để trống");
        }
        this.hireDate = hireDate;
    }

    public String getPosition() {return position;}
    public double getSalary() {return salary;}
    public LocalDate getHireDate() {return hireDate;}

    @Override 
    public String getRoleName() {return "Nhân viên";}

    @Override
    public String toFileString() {
        return String.join(SEP,
                TYPE_TAG,
                getId(),
                getFullName(),
                getPhone(),
                getGender().toString(),
                getIdentityNumber(),
                getDateOfBirth().toString(),
                getAddress(),
                String.valueOf(isActive()),
                position,
                String.valueOf(salary),
                hireDate.toString()
        );
    }

    public static Employee fromFileString(String fileString) {
        // dòng file không được null hoặc rỗng
        CheckingExpression.checkNotNullOrEmpty(fileString, "Dòng dữ liệu Employee không được để trống");

        String[] parts = fileString.split("\\|", -1);
        if (parts.length != 12 || !TYPE_TAG.equals(parts[0])) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ để tạo Employee từ fileString.");
        }

        //mã phải đúng dạng NVxxx trước khi cắt chuỗi, tránh StringIndexOutOfBoundsException
        CheckingExpression.checkNotNullOrEmpty(parts[1], "Mã nhân viên không được để trống");
        if (!parts[1].startsWith(PREFIX) || parts[1].length() <= PREFIX.length()) {
            throw new IllegalArgumentException("Mã nhân viên không đúng định dạng " + PREFIX + "xxx: " + parts[1]);
        }


        Employee employee = new Employee(
                parts[1], // id
                parts[2], // name
                parts[3], // phone
                Gender.valueOf(parts[4]), // gender
                parts[5], // identityNumber
                LocalDate.parse(parts[6]), // dateOfBirth
                parts[7], // address
                Boolean.parseBoolean(parts[8]), // active
                parts[9], // position
                Double.parseDouble(parts[10]), // salary
                LocalDate.parse(parts[11])  // hireDate
        );
        Person.syncNextNumber(Integer.parseInt(parts[1].substring(PREFIX.length()))); // Đồng bộ nextNumber với ID từ file
        return employee;
    }

    // ========================= CONSTRUCTORS ==========================
    // Constructor 1: Dùng khi tạo mới nhân viên (tự động sinh ID)
    public Employee(String name, String phone, Gender gender, String identityNumber, LocalDate dateOfBirth, String address, boolean active, String position, double salary, LocalDate hireDate) {
        super(PREFIX + String.format("%03d", generateNextNumber()), name, phone, gender, identityNumber, dateOfBirth, address, active, true);
        this.position = position;
        this.salary = salary;
        this.hireDate = hireDate;
    }

    // Constructor 2: Dùng khi đọc từ file (ID được cung cấp)
    public Employee(String id, String name, String phone, Gender gender, String identityNumber, LocalDate dateOfBirth, String address, boolean active, String position, double salary, LocalDate hireDate) {
        super(id, name, phone, gender, identityNumber, dateOfBirth, address, active, true);
        this.position = position;
        this.salary = salary;
        this.hireDate = hireDate;
    }
}
