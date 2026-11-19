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
        boolean tagged = parts.length == 10 && TYPE_TAG.equals(parts[0]);
        if (!tagged && parts.length != 9) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ để tạo Customer");
        }

        int offset = tagged ? 1 : 0;
        String id = parts[offset];
        CheckingExpression.checkNotNullOrEmpty(id, "Mã khách hàng không được để trống");
        if (!id.startsWith(PREFIX) || id.length() <= PREFIX.length()) {
            throw new IllegalArgumentException("Mã khách hàng không đúng định dạng " + PREFIX + "xxx: " + id);
        }

        String address = parts[7];
        String active = parts[8];
        String email = tagged ? parts[9] : parts[6];

        Customer customer = new Customer(
                id,
                parts[offset + 1],
                parts[offset + 2],
                Gender.valueOf(parts[offset + 3]),
                parts[offset + 4],
                LocalDate.parse(parts[offset + 5]),
                address,
                Boolean.parseBoolean(active),
                email
        );
        Person.syncNextNumber(Integer.parseInt(id.substring(PREFIX.length())));
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
