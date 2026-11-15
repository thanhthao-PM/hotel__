package hotel.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import hotel.model.enums.Role;
import java.time.LocalDate;

import hotel.util.CheckingExpression;

public class Account {

    private static final String SEP      = "|";         // ký tự ngăn cách

    private String username;
    private String passwordHash;
    private Role role;
    private String employeeId; // Tham chiểu đến Employee
    private boolean locked;
    private int failedAttempts;
    private LocalDate lastLogin;
    private boolean mustChangePassword;
    
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int MIN_PASSWORD_LENGTH = 8;

    // ========================== SETTERS ==========================

    public void updateLastLogin() {this.lastLogin = LocalDate.now();}

    public void lock() {this.locked = true;}
    public void unlock() {this.locked = false;}

    // Gọi khi người dùng nhập sai mật khẩu -> Tăng số lần nhập sai
    public void recordFailedAttempt() {
        this.failedAttempts++;
        if(this.failedAttempts >= MAX_FAILED_ATTEMPTS) lock();
    }

    // Gọi khi người dùng nhập đúng mật khẩu/ Admin mở khoá
    public void resetFailedAttempt() {this.failedAttempts = 0;}

    // Hàm băm mật khẩu (ví dụ sử dụng SHA-256)
    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));  // chữ → mảng byte → băm
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));   // mỗi byte thành 2 ký tự hex
            }
            return sb.toString();                      // chuỗi 64 ký tự
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có trong mọi JDK, nên lỗi này gần như không xảy ra
            throw new IllegalStateException("Không hỗ trợ SHA-256", e);
        }
    }

    // Hàm kiểm tra mật khẩu
    private static void checkPassword(String password) {
        CheckingExpression.checkNotNullOrEmpty(password, "Mật khẩu");
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.");
        }
    }

    // Dành cho người dùng muốn đổi mật khẩu
    public void changePassword(String newPassword) {
        // Kiểm tra mật khẩu mới
        checkPassword(newPassword);
        // Băm mật khẩu mới
        String newHash = hash(newPassword); // Chuỗi 64 ký tự
        if (newHash.equals(this.passwordHash)) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ.");
        }
        // Lưu mật khẩu mới
        this.passwordHash = newHash;
        this.mustChangePassword = false; // Đặt lại cờ yêu cầu đổi mật khẩu
    }

    // Dành cho Admmin
    public void resetPassword(String tempPassword) {
        checkPassword(tempPassword);               // kiểm tra độ dài, ném lỗi nếu sai
        this.passwordHash = hash(tempPassword);    // băm rồi mới lưu
        this.mustChangePassword = true;            // ép người dùng đổi ở lần đăng nhập sau
    }

    public void setRole (Role role) {
        if(role == null) {throw new IllegalArgumentException("Vai trò không được để trống!");}
        this.role = role;
    } 

    // ========================== GETTERS ==========================

    public String getUsername() {return this.username;}
    public String getPasswordHash() {return this.passwordHash;}
    public Role getRole() {return this.role;}
    public String getEmployeeId() {return this.employeeId;}
    public boolean isLocked() {return this.locked;}
    public int getFailedAttempts() {return this.failedAttempts;}
    public LocalDate getLastLogin() {return this.lastLogin;}
    public boolean isMustChangePassword() {return this.mustChangePassword;}
    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null) return false;
        return this.passwordHash.equals(hash(rawPassword));
    }

    // ========================== CONSTRUCTORs ==========================
    // Constructor 1: TẠO MỚI. Nhận mật khẩu thô, kiểm tra rồi băm
    public Account(String username, String rawPassword, Role role, String employeeId) {
        // Kiểm tra tên đăng nhập
        CheckingExpression.checkNotNullOrEmpty(username, "Tên đăng nhập không được để trống!");
        // Kiểm tra mật khẩu
        checkPassword(rawPassword);
        // Kiểm tra role
        if(role == null) {throw new IllegalArgumentException("Quyền không được để trống!");}
        this.role = role;
        // Kiểm tra employeeId
        CheckingExpression.checkNotNullOrEmpty(employeeId, "Mã nhân viên không được để trống!");

        this.username = username;
        this.passwordHash = hash(rawPassword);
        this.role = role;
        this.employeeId = employeeId;
        this.locked = false;
        this.failedAttempts = 0;
        this.lastLogin = null;
        this.mustChangePassword = true;
    }

    // Construtor 2: NẠP TỪ FILE. Nhận hash có sẵn, không băm lại.
    private Account (String username, String passwordHash, Role role, String employeeId, boolean locked, int failedAttempts, LocalDate lastLogin, boolean mustChangePassword) {
        // Kiểm tra tên đăng nhập
        CheckingExpression.checkNotNullOrEmpty(username, "Tên đăng nhập không được để trống!");
        // Kiểm tra hashPassword
        CheckingExpression.checkNotNullOrEmpty(passwordHash, "Mật khẩu không được để trống!");
        // Kiểm tra role
        if(role == null) {throw new IllegalArgumentException("Quyền không được để trống!");}
        // Kiểm tra employeeId
        CheckingExpression.checkNotNullOrEmpty(employeeId, "Mã nhân viên không được để trống!");  
        // Kiểm tra failedAttempts
        CheckingExpression.checkNonNegativeInt(failedAttempts, "Số lần sai phải là số nguyên không âm");     
        
        this.username = username.trim();
        this.passwordHash = passwordHash;
        this.role = role;
        this.employeeId = employeeId;
        this.locked = locked;
        this.failedAttempts = failedAttempts;
        this.lastLogin = lastLogin;
        this.mustChangePassword = mustChangePassword;
    }

    // ========================== ToFileString_FromFileString ==========================

    public String toFileString() {
        return String.join(SEP,
                            username,
                            passwordHash,
                            role.name(),
                            employeeId,
                            String.valueOf(locked),
                            String.valueOf(failedAttempts),
                            (lastLogin == null) ? "" : lastLogin.toString(),
                            String.valueOf(mustChangePassword));
    }

    public static Account fromFileString(String fileString) {
        String[] parts = fileString.split("\\|", -1);
        if (parts.length != 8) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ để tạo Account");
        }

        LocalDate lastLogin = null;
        if (!parts[6].isBlank()) {
            lastLogin = LocalDate.parse(parts[6]);
        }

        return new Account(
                parts[0],
                parts[1],
                Role.valueOf(parts[2]),
                parts[3],
                Boolean.parseBoolean(parts[4]),
                Integer.parseInt(parts[5]),
                lastLogin,
                Boolean.parseBoolean(parts[7])
        );
    }

}