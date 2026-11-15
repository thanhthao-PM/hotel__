package hotel.model;

import hotel.model.enums.Gender;
import java.time.LocalDate;
import hotel.util.CheckingExpression;

public class GuestMember extends Person {

    private static final String PREFIX   = "KH";        // tiền tố mã: KH001, KH002...
    public static final String TYPE_TAG = "GUEST MEMBER";  // nhãn loại ghi ở đầu dòng file
    private static final String SEP      = "|";         // ký tự ngăn cách
    
    private String representativeId;
    private String relationship;

    public void setRepresentativeId(String representativeId) {
        CheckingExpression.checkNotNullOrEmpty(representativeId, "Mã khách hàng đại diện không được để trống");
        this.representativeId = representativeId;
    }

    public void setRelationship(String relationship) {
        CheckingExpression.checkNotNullOrEmpty(relationship, "Mối quan hệ không được để trống");
        this.relationship = relationship;
    }

    public String getRepresentativeId() {return representativeId;}
    public String getRelationship() {return relationship;}

    @Override
    public String getRoleName() {return "Khách thành viên";}
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
                getRepresentativeId(),
                getRelationship()
        );
    }

    public static GuestMember fromFileString(String fileString) {
        // dòng file không được null hoặc rỗng
        CheckingExpression.checkNotNullOrEmpty(fileString, "Dòng dữ liệu GuestMember không được để trống");

        String[] parts = fileString.split("\\|", -1);
        if (parts.length != 11 || !TYPE_TAG.equals(parts[0])) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ để tạo GuestMember từ fileString.");
        }

        //mã phải đúng dạng KHxxx trước khi cắt chuỗi, tránh StringIndexOutOfBoundsException
        CheckingExpression.checkNotNullOrEmpty(parts[1], "Mã thành viên không được để trống");
        if (!parts[1].startsWith(PREFIX) || parts[1].length() <= PREFIX.length()) {
            throw new IllegalArgumentException("Mã thành viên không đúng định dạng " + PREFIX + "xxx: " + parts[1]);
        }

        GuestMember guestMember = new GuestMember(
                parts[1], // id
                parts[2], // name
                parts[3], // phone
                Gender.valueOf(parts[4]), // gender
                parts[5], // identityNumber
                LocalDate.parse(parts[6]), // dateOfBirth
                parts[7], // address
                Boolean.parseBoolean(parts[8]), // active
                parts[9], // representativeId
                parts[10] // relationship
        );
        Person.syncNextNumber(Integer.parseInt(parts[1].substring(PREFIX.length()))); // Đồng bộ nextNumber với ID từ file
        return guestMember;
    }

    // String id, String name, String phone, Gender gender, String identityNumber,
                //   LocalDate dateOfBirth, String address, boolean active, boolean identityRequired)

    // Constructor 1: TẠO MỚI KHÁCH HÀNG (ID TỰ ĐỘNG TĂNG)
    public GuestMember(String fullName, String phone, Gender gender, String identityNumber, LocalDate dateOfBirth, String address, boolean active, String representativeId, String relationship) {
        super(PREFIX + String.format("%03d", generateNextNumber()), fullName, phone, gender, identityNumber, dateOfBirth, address, active, false);
        setRepresentativeId(representativeId);
        setRelationship(relationship);
    }

    // Constructor 2: TẠO KHÁCH HÀNG TỪ FILE (ID ĐƯỢC LẤY TRỰC TIẾP)
    public GuestMember(String id, String fullName, String phone, Gender gender, String identityNumber, LocalDate dateOfBirth, String address, boolean active, String representativeId, String relationship) {
        super(id, fullName, phone, gender, identityNumber, dateOfBirth, address, active, false);
        setRepresentativeId(representativeId);
        setRelationship(relationship);
    }

}
