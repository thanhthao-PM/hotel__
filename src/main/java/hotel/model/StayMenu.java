package hotel.model;

import hotel.model.enums.Role;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Menu con "Lưu trú" của P4. Không tự đăng nhập: P1 tạo StayMenu và truyền vào employeeId + Role
 * của người đang đăng nhập, rồi gắn vào AdminMenu, ManagerMenu, EmployeeMenu.
 * Mọi lỗi nghiệp vụ được bắt tại đây và in thông báo, chương trình không bị sập.
 * Đọc cả dòng rồi mới đổi kiểu để tránh lỗi trôi dòng của Scanner (mục 6.3).
 */
public class StayMenu {
    private final Scanner in;
    private final StayManager manager;
    private final String employeeId;
    private final Role role;

    public StayMenu(Scanner in, StayManager manager, String employeeId, Role role) {
        this.in = in;
        this.manager = manager;
        this.employeeId = employeeId;
        this.role = role;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("========== LƯU TRÚ (" + employeeId + " - " + role + ") ==========");
            System.out.println("1. Nhận phòng");
            System.out.println("2. Ghi dịch vụ");
            System.out.println("3. Sửa hoặc hủy dịch vụ");
            System.out.println("4. Đổi phòng");
            System.out.println("5. Gia hạn");
            System.out.println("6. Trả phòng");
            System.out.println("7. Khách không đến (no-show)");
            System.out.println("8. Tra cứu và thống kê");
            if (role != Role.EMPLOYEE) System.out.println("9. Xóa lượt ở");
            System.out.println("0. Quay lại");
            int choice = readInt("Chọn: ");
            try {
                switch (choice) {
                    case 0: return;
                    case 1: checkIn(); break;
                    case 2: addUsage(); break;
                    case 3: editUsage(); break;
                    case 4: changeRoom(); break;
                    case 5: extend(); break;
                    case 6: checkOut(); break;
                    case 7: noShow(); break;
                    case 8: lookup(); break;
                    case 9:
                        if (role == Role.EMPLOYEE) { System.out.println("Bạn không có quyền xóa lượt ở."); break; }
                        remove();
                        break;
                    default: System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println("[LỖI] " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("[LỖI HỆ THỐNG] " + e);
            }
        }
    }

    private void checkIn() {
        String code = readLine("Mã phiếu yêu cầu hoặc mã đặt phòng (để trống = hủy): ");
        if (code.isEmpty()) return;
        StayLinks.BookingLine[] lines = manager.getLines(code);
        if (lines.length == 0) { System.out.println("Không tìm thấy phiếu " + code); return; }
        System.out.println("Các phòng trong phiếu:");
        for (StayLinks.BookingLine l : lines) {
            System.out.printf("  %-6s phòng %-5s %s -> %s, %d khách %s%n", l.bookingDetailId, l.roomId,
                    l.checkInDate, l.checkOutDate, l.guestCount, manager.hasStayForLine(l.bookingDetailId) ? "[ĐÃ NHẬN]" : "");
        }
        String detailId = readLine("Mã dòng cần nhận phòng: ");
        String guests = readLine("Mã khách ở phòng này (cách nhau dấu phẩy): ");
        String note = readLine("Ghi chú (có thể bỏ trống): ");
        StayRecord s = manager.checkIn(code, detailId, splitIds(guests), employeeId, note);
        System.out.println("Nhận phòng thành công, lượt ở " + s.getStayId());
        manager.findCheckInReceipt(s.getStayId()).printReceipt();
    }

    private void addUsage() {
        String stayId = readLine("Mã lượt ở: ");
        String serviceId = readLine("Mã dịch vụ: ");
        double qty = readDouble("Số lượng: ");
        ServiceUsage u = manager.addServiceUsage(stayId, serviceId, qty, employeeId);
        System.out.println("Đã ghi: " + u);
    }

    private void editUsage() {
        String stayId = readLine("Mã lượt ở: ");
        manager.displayUsages(stayId);
        String usageId = readLine("Mã dòng dịch vụ: ");
        System.out.println("1. Sửa số lượng   2. Hủy dòng   0. Bỏ qua");
        int c = readInt("Chọn: ");
        if (c == 1) {
            manager.updateServiceUsage(stayId, usageId, readDouble("Số lượng mới: "));
            System.out.println("Đã sửa.");
        } else if (c == 2) {
            if (!confirm("Hủy dòng " + usageId + "?")) return;
            boolean needP5 = manager.cancelServiceUsage(stayId, usageId, role);
            System.out.println("Đã hủy." + (needP5 ? " Hóa đơn đã lập: cần P5 điều chỉnh hoặc hoàn tiền." : ""));
        }
    }

    private void changeRoom() {
        String stayId = readLine("Mã lượt ở: ");
        String room = readLine("Mã phòng mới: ");
        String reason = readLine("Lý do: ");
        if (!confirm("Đổi phòng?")) return;
        StayRecord s = manager.changeRoom(stayId, room, employeeId, reason);
        System.out.println("Đã đổi phòng, lượt ở mới " + s.getStayId());
    }

    private void extend() {
        String stayId = readLine("Mã lượt ở: ");
        LocalDate d = readDate("Ngày trả mới (yyyy-MM-dd): ");
        System.out.println(manager.extendStay(stayId, d, employeeId)
                ? "Đã gia hạn." : "Không gia hạn được (phòng đã có người đặt kế tiếp).");
    }

    private void checkOut() {
        String stayId = readLine("Mã lượt ở: ");
        String cond = readLine("Tình trạng phòng: ");
        String note = readLine("Ghi chú: ");
        if (!confirm("Trả phòng của lượt ở " + stayId + "?")) return;
        StayManager.CheckOutResult r = manager.checkOut(stayId, employeeId, cond, note);
        r.receipt.printReceipt();
        System.out.println(r.invoiceId == null ? "Chưa có hóa đơn." : "Mã hóa đơn: " + r.invoiceId);
        if (r.bookingCompleted) System.out.println("Tất cả phòng của Booking đã trả: Booking COMPLETED.");
        if (r.warning != null) System.out.println("[CẢNH BÁO] " + r.warning);
    }

    private void noShow() {
        String code = readLine("Mã phiếu yêu cầu hoặc mã đặt phòng: ");
        if (!confirm("Đánh dấu NO_SHOW cho " + code + "? (tiền cọc sẽ do quản lý xử lý)")) return;
        manager.processNoShow(code, employeeId);
        System.out.println("Đã chuyển NO_SHOW.");
    }

    private void lookup() {
        System.out.println("1. Khách đang ở   2. Tìm kiếm   3. Lịch sử lưu trú theo khách   4. Dịch vụ của một lượt ở");
        System.out.println("5. Tất cả (mới nhất trước)   6. Thống kê   7. Xem phiếu của một lượt ở");
        switch (readInt("Chọn: ")) {
            case 1: manager.displayActiveGuests(); break;
            case 2: manager.displayList(manager.search(readLine("Từ khóa (mã lượt ở, booking, phòng, khách, trạng thái): "))); break;
            case 3: manager.displayList(manager.findByGuest(readLine("Mã khách: "))); break;
            case 4: manager.displayUsages(readLine("Mã lượt ở: ")); break;
            case 5: manager.displayList(manager.sortedByCheckIn(true)); break;
            case 6: manager.printStatistics(); break;
            case 7:
                String id = readLine("Mã lượt ở: ");
                CheckInReceipt ci = manager.findCheckInReceipt(id);
                CheckOutReceipt co = manager.findCheckOutReceipt(id);
                if (ci != null) ci.printReceipt(); else System.out.println("(Chưa có phiếu nhận)");
                if (co != null) co.printReceipt(); else System.out.println("(Chưa có phiếu trả)");
                break;
            default: System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private void remove() {
        String id = readLine("Mã lượt ở cần xóa: ");
        if (!confirm("Xóa lượt ở " + id + " cùng phiếu và dịch vụ kèm theo?")) return;
        System.out.println(manager.remove(id) ? "Đã xóa."
                : "Không xóa được (không tồn tại, đang ở, hoặc đã có hóa đơn).");
    }

    // ---------- nhập liệu ----------
    private String readLine(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    private int readInt(String prompt) {
        while (true) {
            if (!in.hasNextLine() && System.in != null) { return 0; } // hết dữ liệu nhập thì thoát
            String s = readLine(prompt);
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { System.out.println("Hãy nhập một số nguyên."); }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            String s = readLine(prompt).replace(',', '.');
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { System.out.println("Hãy nhập một số."); }
            if (!in.hasNextLine()) return 0;
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try { return LocalDate.parse(s); } catch (DateTimeParseException e) { System.out.println("Sai định dạng ngày (yyyy-MM-dd)."); }
            if (!in.hasNextLine()) throw new IllegalArgumentException("Không có ngày hợp lệ");
        }
    }

    private boolean confirm(String question) {
        String s = readLine(question + " (y/n): ");
        return s.equalsIgnoreCase("y");
    }

    private static String[] splitIds(String s) {
        if (s.trim().isEmpty()) return new String[0];
        return s.split("\\s*,\\s*");
    }
}

