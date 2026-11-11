package hotel.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

// Lớp kiểm tra và nhập liệu console (toàn bộ là static)
// Luôn đọc cả dòng rồi mới chuyển kiểu để tránh lỗi trôi dòng của Scanner
public class Validator {

    // Định dạng ngày hiển thị và nhập trên console
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Không cho tạo đối tượng
    private Validator() {
    }

    // Kiểm tra chuỗi không null và không rỗng
    public static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // Kiểm tra khoảng ngày hợp lệ: ngày kết thúc phải sau ngày bắt đầu
    public static boolean isValidDateRange(LocalDate from, LocalDate to) {
        return from != null && to != null && to.isAfter(from);
    }

    // Nhập một chuỗi không rỗng, nhập sai thì hỏi lại
    public static String inputString(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (isNotBlank(s)) {
                return FileUtil.clean(s);
            }
            System.out.println("Lỗi: không được để trống, vui lòng nhập lại.");
        }
    }

    // Nhập số nguyên trong khoảng [min, max], nhập sai thì hỏi lại
    public static int inputInt(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(s);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Lỗi: giá trị phải từ " + min + " đến " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: vui lòng nhập một số nguyên.");
            }
        }
    }

    // Nhập số thực không nhỏ hơn min, nhập sai thì hỏi lại
    public static double inputDouble(Scanner sc, String prompt, double min) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                double value = Double.parseDouble(s);
                if (value >= min) {
                    return value;
                }
                System.out.println("Lỗi: giá trị phải từ " + min + " trở lên.");
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: vui lòng nhập một số.");
            }
        }
    }

    // Nhập ngày theo định dạng dd/MM/yyyy, nhập sai thì hỏi lại
    public static LocalDate inputDate(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + " (dd/MM/yyyy): ");
            String s = sc.nextLine().trim();
            try {
                return LocalDate.parse(s, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Lỗi: ngày không đúng định dạng dd/MM/yyyy.");
            }
        }
    }

    // Hỏi xác nhận có/không (dùng cho thao tác nguy hiểm như xóa)
    public static boolean confirm(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String s = sc.nextLine().trim().toLowerCase();
            if (s.equals("y")) {
                return true;
            }
            if (s.equals("n")) {
                return false;
            }
            System.out.println("Lỗi: chỉ nhập y hoặc n.");
        }
    }
}