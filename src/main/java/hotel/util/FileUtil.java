package hotel.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

// Lớp tiện ích đọc/ghi file text (toàn bộ là static, cả nhóm dùng chung)
public class FileUtil {

    // Ký tự ngăn cách các trường trong một dòng file
    public static final String SEPARATOR = "|";

    // Không cho tạo đối tượng, vì chỉ dùng hàm static
    private FileUtil() {
    }

    // Thay ký tự ngăn cách trong dữ liệu người nhập bằng dấu cách để không làm hỏng file
    public static String clean(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(SEPARATOR, " ").trim();
    }

    // Ghép nhiều trường thành một dòng, ngăn cách bằng SEPARATOR
    public static String join(Object... fields) {
        // fields null (gọi join((Object[]) null)) sẽ gây NullPointerException ở fields.length
        if (fields == null) {
            throw new IllegalArgumentException("Danh sách trường cần ghép không được null");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(fields[i] == null ? "" : fields[i].toString());
        }
        return sb.toString();
    }

    // Tách một dòng thành các trường (giữ cả trường rỗng ở cuối dòng)
    public static String[] split(String line) {
        // line null sẽ gây NullPointerException ở line.split(...)
        if (line == null) {
            throw new IllegalArgumentException("Dòng dữ liệu không được null");
        }
        return line.split("\\|", -1);
    }

    // Lấy phần số trong mã, ví dụ "R012" -> 12 (dùng để nạp lại bộ đếm mã), lỗi trả 0
    public static int extractNumber(String id) {
        if (id == null) {
            return 0;
        }
        String digits = id.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Đọc toàn bộ dòng của file (UTF-8), bỏ dòng trống; file chưa có thì trả danh sách rỗng
    public static ArrayList<String> readLines(String path) throws IOException {
        // đường dẫn không được rỗng (Paths.get(null) gây NullPointerException)
        CheckingExpression.checkNotNullOrEmpty(path, "Đường dẫn file không được để trống");
        ArrayList<String> result = new ArrayList<>();
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            return result;
        }
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }

    // Ghi danh sách dòng xuống file (UTF-8), tự tạo thư mục và sao lưu file cũ thành .bak
    public static void writeLines(String path, ArrayList<String> lines) throws IOException {
        // kiểm tra trước khi động đến file, tránh ghi đè file cũ bằng dữ liệu hỏng
        CheckingExpression.checkNotNullOrEmpty(path, "Đường dẫn file không được để trống");
        if (lines == null) {
            throw new IllegalArgumentException("Danh sách dòng cần ghi không được null");
        }
        Path p = Paths.get(path);
        if (p.getParent() != null) {
            Files.createDirectories(p.getParent());
        }
        if (Files.exists(p)) {
            Files.copy(p, Paths.get(path + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.write(p, lines, StandardCharsets.UTF_8);
    }

    public static ArrayList<String> readAllLines(String path) throws IOException {
        return readLines(path);
    }

    public static void writeAllLines(String path, String[] lines, int count) throws IOException {
        // kiểm tra đường dẫn, mảng và số phần tử trước khi động đến file
        CheckingExpression.checkNotNullOrEmpty(path, "Đường dẫn file không được để trống");
        if (lines == null) {
            throw new IllegalArgumentException("Mảng dòng cần ghi không được null");
        }
        CheckingExpression.checkNonNegativeInt(count, "Số dòng cần ghi không được âm");
        if (count > lines.length) {
            throw new IllegalArgumentException("Số dòng cần ghi (" + count + ") lớn hơn kích thước mảng (" + lines.length + ")");
        }

        Path p = Paths.get(path);
        if (p.getParent() != null) {
            Files.createDirectories(p.getParent());
        }
        if (Files.exists(p)) {
            Files.copy(p, Paths.get(path + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        }
        ArrayList<String> content = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (lines[i] != null) content.add(lines[i]);
        }
        Files.write(p, content, StandardCharsets.UTF_8);
    }
}