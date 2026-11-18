package hotel.model;

import hotel.util.CheckingExpression;
import java.time.LocalDate;
import hotel.model.enums.Gender;

public class Customer extends Person {

    private static final String PREFIX   = "CU";        // tiền tố mã: CU001, CU002...
    public static final String TYPE_TAG = "CUSTOMER";  // nhãn loại ghi ở đầu dòng file
    private static final String SEP      = "|";         // ký tự ngăn cách

    private String email;

    public void setEmail(String email) {
        CheckingExpression.checkValidEmail(email);
        this.email = email;
    }

    public String getEmail() {return this.email;}

    @Override 
    public String getRoleName() {return "Khách hàng";}
    
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
                email
        );
    }

    public static Customer fromFileString(String fileString) {
        //dòng file không được null hoặc rỗng
        CheckingExpression.checkNotNullOrEmpty(fileString, "Dòng dữ liệu Customer không được để trống");

        String[] parts = fileString.split("\\|", -1);
        if (parts.length != 10 || !TYPE_TAG.equals(parts[0])) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ để tạo Customer");
        }
         // mã phải đúng dạng KHxxx trước khi cắt chuỗi, tránh StringIndexOutOfBoundsException
        CheckingExpression.checkNotNullOrEmpty(parts[1], "Mã khách hàng không được để trống");
        if (!parts[1].startsWith(PREFIX) || parts[1].length() <= PREFIX.length()) {
            throw new IllegalArgumentException("Mã khách hàng không đúng định dạng " + PREFIX + "xxx: " + parts[1]);
        }
        

        Customer customer = new Customer(
                parts[1], // id
                parts[2], // name
                parts[3], // phone
                Gender.valueOf(parts[4]), // gender
                parts[5], // identityNumber
                LocalDate.parse(parts[6]), // dateOfBirth
                parts[7], // address
                Boolean.parseBoolean(parts[8]), // active
                parts[9]  // email
        );
        Person.syncNextNumber(Integer.parseInt(parts[1].substring(PREFIX.length()))); // Đồng bộ nextNumber với ID từ file
        return customer;
    }

    // ========================= CONSTRUCTORS ==========================
    // Constructor 1: TẠO MỚI KHÁCH HÀNG (ID TỰ ĐỘNG TĂNG)
    public Customer(String fullName, String phone, Gender gender, String identityNumber, LocalDate dateOfBirth, String address, boolean active, String email) {
        super(PREFIX + String.format("%03d", generateNextNumber()), fullName, phone, gender, identityNumber, dateOfBirth, address, active, true);
        setEmail(email);
    }

    // Constructor 2: TẠO KHÁCH HÀNG TỪ FILE (ID ĐƯỢC LẤY TRỰC TIẾP)
    public Customer(String id, String fullName, String phone, Gender gender, String identityNumber, LocalDate dateOfBirth, String address, boolean active, String email) {
        super(id, fullName, phone, gender, identityNumber, dateOfBirth, address, active, true);
        setEmail(email);
    }

}
