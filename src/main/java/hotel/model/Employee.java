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
        CheckingExpression.checkNotNullOrEmpty(
                fileString, "Dòng dữ liệu Employee không được để trống");

        String[] parts = fileString.split("\\|", -1);
        int offset = 0;

        if (parts.length == 12) {
            if (!TYPE_TAG.equals(parts[0])) {
                throw new IllegalArgumentException("Thiếu nhãn EMPLOYEE ở đầu dòng dữ liệu");
            }
            offset = 1;
        } else if (parts.length != 11) {
            throw new IllegalArgumentException(
                    "Dữ liệu không hợp lệ để tạo Employee từ fileString.");
        }

        String id;
        String name;
        String phone;
        Gender gender;
        String identityNumber;
        LocalDate dateOfBirth;
        String address;
        boolean active;
        String position;
        double salary;
        LocalDate hireDate;

        if (parts.length - offset == 11) {
            id = parts[offset];
            name = parts[offset + 1];
            phone = parts[offset + 2];
            gender = Gender.valueOf(parts[offset + 3]);
            identityNumber = parts[offset + 4];
            dateOfBirth = LocalDate.parse(parts[offset + 5]);

            // Dữ liệu cũ: id|name|phone|gender|identity|dob|position|salary|hire|address|active
            // Dữ liệu mới của Employee.toFileString:
            // EMPLOYEE|id|name|phone|gender|identity|dob|address|active|position|salary|hire
            if (offset == 1
                    && (parts[8].equalsIgnoreCase("true") || parts[8].equalsIgnoreCase("false"))) {
                address = parts[7];
                active = Boolean.parseBoolean(parts[8]);
                position = parts[9];
                salary = Double.parseDouble(parts[10]);
                hireDate = LocalDate.parse(parts[11]);
            } else if (offset == 0 && (parts[7].equalsIgnoreCase("true") || parts[7].equalsIgnoreCase("false"))) {
                address = parts[6];
                active = Boolean.parseBoolean(parts[7]);
                position = parts[8];
                salary = Double.parseDouble(parts[9]);
                hireDate = LocalDate.parse(parts[10]);
            } else {
                position = parts[offset + 6];
                salary = Double.parseDouble(parts[offset + 7]);
                hireDate = LocalDate.parse(parts[offset + 8]);
                address = parts[offset + 9];
                active = Boolean.parseBoolean(parts[offset + 10]);
            }
        } else {
            throw new IllegalArgumentException(
                    "Dữ liệu không hợp lệ để tạo Employee từ fileString.");
        }

        CheckingExpression.checkNotNullOrEmpty(id, "Mã nhân viên không được để trống");
        if (!id.startsWith(PREFIX) || id.length() <= PREFIX.length()) {
            throw new IllegalArgumentException(
                    "Mã nhân viên không đúng định dạng " + PREFIX);
        }

        Employee employee = new Employee(
                id, name, phone, gender, identityNumber, dateOfBirth,
                address, active, position, salary, hireDate);

        Person.syncNextNumber(
                Integer.parseInt(id.substring(PREFIX.length())));

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
