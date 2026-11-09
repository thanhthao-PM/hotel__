package hotel.util;

import hotel.model.enums.LineType;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Lớp tiện ích kiểm tra tính hợp lệ dữ liệu và chặn ngoại lệ runtime toàn hệ thống.
 */
public class CheckingExpression {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern CCCD_PATTERN = Pattern.compile("^\\d{12}$");

    // 1. Kiểm tra Chuỗi Null hoặc Rỗng
    public static void checkNotNullOrEmpty(String str, String errorMessage) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    // 2. Kiểm tra Số nguyên Dương (> 0)
    public static void checkPositiveInt(int value, String errorMessage) {
        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    // 3. Kiểm tra Số nguyên Không âm (>= 0)
    public static void checkNonNegativeInt(int value, String errorMessage) {
        if (value < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    // 4. Kiểm tra Số thực Dương (> 0)
    public static void checkPositiveDouble(double value, String errorMessage) {
        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    // 5. Kiểm tra Số thực Không âm (>= 0)
    public static void checkNonNegativeDouble(double value, String errorMessage) {
        if (value < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    // 6. Chặn Phép chia cho 0
    public static void checkDivideByZero(double denominator, String errorMessage) {
        if (denominator == 0) {
            throw new ArithmeticException(errorMessage);
        }
    }

    // 7. Chặn Truy cập Ngoài Biên Mảng
    public static void checkValidArrayIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Chỉ số mảng không hợp lệ: " + index + ", kích thước: " + size);
        }
    }

    // 8. Kiểm tra Số điện thoại Việt Nam
    public static void checkValidPhone(String phone) {
        checkNotNullOrEmpty(phone, "Số điện thoại không được để trống");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (Phải gồm 10 chữ số bắt đầu bằng 0)");
        }
    }

    // 9. Kiểm tra Số CCCD / Hộ chiếu
    public static void checkValidIdentityNumber(String identityNumber, boolean isOptional) {
        if (isOptional && (identityNumber == null || identityNumber.trim().isEmpty())) {
            return; // Cho phép rỗng nếu là trẻ em đi cùng
        }
        checkNotNullOrEmpty(identityNumber, "Số CCCD/Hộ chiếu không được để trống");
        if (!CCCD_PATTERN.matcher(identityNumber).matches() && identityNumber.length() < 8) {
            throw new IllegalArgumentException("Số CCCD/Hộ chiếu không hợp lệ");
        }
    }

    // 10. Kiểm tra Email
    public static void checkValidEmail(String email) {
        checkNotNullOrEmpty(email, "Email không được để trống");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email không đúng định dạng (VD: example@mail.com)");
        }
    }

    // 11. Kiểm tra Khoảng Ngày Logic
    public static void checkValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Ngày tháng không được để null");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải xảy ra sau ngày bắt đầu");
        }
    }

    // 12. Kiểm tra Sức chứa Phòng
    public static void checkRoomCapacity(int guestCount, int capacity) {
        checkPositiveInt(guestCount, "Số lượng khách phải > 0");
        if (guestCount > capacity) {
            throw new IllegalArgumentException("Số lượng khách (" + guestCount + ") vượt quá sức chứa phòng (" + capacity + ")");
        }
    }

    // 13. Kiểm tra Mã Loại Trừ trong InvoiceDetail
    public static void checkMutuallyExclusiveIds(LineType lineType, String roomId, String serviceId) {
        if (lineType == LineType.ROOM) {
            if (roomId == null || roomId.trim().isEmpty() || (serviceId != null && !serviceId.trim().isEmpty())) {
                throw new IllegalArgumentException("Dòng tiền phòng phải có roomId và serviceId phải rỗng");
            }
        } else if (lineType == LineType.SERVICE) {
            if (serviceId == null || serviceId.trim().isEmpty() || (roomId != null && !roomId.trim().isEmpty())) {
                throw new IllegalArgumentException("Dòng dịch vụ phải có serviceId và roomId phải rỗng");
            }
        }
    }

    // 14. Đảm bảo Ít nhất 1 trong 2 Mã có giá trị
    public static void checkAtLeastOneNotNull(String id1, String id2, String errorMessage) {
        boolean hasId1 = (id1 != null && !id1.trim().isEmpty());
        boolean hasId2 = (id2 != null && !id2.trim().isEmpty());
        if (!hasId1 && !hasId2) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}