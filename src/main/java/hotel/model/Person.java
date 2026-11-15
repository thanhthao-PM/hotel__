package hotel.model;

import hotel.util.CheckingExpression;
import java.time.LocalDate;
import hotel.model.enums.Gender;

public abstract class Person {

    // Các thuộc tính của lớp Person
    private final String id;
    private String fullName;
    private String phone;
    private Gender gender;
    private String identityNumber;
    private LocalDate dateOfBirth;
    private String address;
    private boolean active;
    private final boolean identityRequired;

    private static int nextNumber;

    // Hai phương thức abstract
    public abstract String getRoleName();
    public abstract String toFileString();

    // Các phương thức setter
    public void setName(String name) {
        CheckingExpression.checkNotNullOrEmpty(name, "Họ và tên không được để trống");
        this.fullName = name.trim(); //trim() để loại bỏ khoảng trắng ở đầu và cuối chuỗi
    }

    public void setPhone(String phone) {
        CheckingExpression.checkValidPhone(phone);
        this.phone = phone;
    }

    public void setGender(Gender gender) {
        if (gender == null) {
            throw new IllegalArgumentException("Giới tính không được để trống.");
        }
        this.gender = gender;
    }

    public void setIdentityNumber(String identityNumber) {
        CheckingExpression.checkValidIdentityNumber(identityNumber, !identityRequired);
        this.identityNumber = identityNumber;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Ngày sinh không được để trống.");
        }
        this.dateOfBirth = dateOfBirth;
    }

    public void setAddress(String address) {
        CheckingExpression.checkNotNullOrEmpty(address, "Địa chỉ không được để trống");
        this.address = address.trim();
    }

    public void activate() {this.active = true;}
    public void deactivate() {this.active = false;}

    // Các phương thức getter
    public String getId() {return id;}
    public String getFullName() {return fullName;}
    public String getPhone() {return phone;}
    public Gender getGender() {return gender;}
    public String getIdentityNumber() {return identityNumber;}
    public LocalDate getDateOfBirth() {return dateOfBirth;}
    public String getAddress() {return address;}
    public boolean isActive() {return active;}

    // Constructor
    public Person(String id, String fullName, String phone, Gender gender, String identityNumber,
                  LocalDate dateOfBirth, String address, boolean active, boolean identityRequired) {
        this.id = id;
        this.identityRequired = identityRequired;
        setName(fullName);
        setPhone(phone);
        setGender(gender);
        setIdentityNumber(identityNumber);
        setDateOfBirth(dateOfBirth);
        setAddress(address);
        this.active = active;
    }

    // Dùng chung
    public static void syncNextNumber(int maxSeen) {nextNumber = Math.max(nextNumber, maxSeen + 1);}
    protected static int generateNextNumber() {return nextNumber++;}
}
