package hotel.model;
import java.time.LocalDate;

public class GroupBooking extends Booking {
    private String groupName;        // Tên đoàn khách (ví dụ: "Đoàn công ty X")
    private String[] roomIds;        // Danh sách các mã phòng đoàn đặt
    private Room[] roomObjs;         // Mảng đối tượng Room tương ứng
    private GuestMember[] members;   // Mảng thành viên đi cùng trong đoàn (từ P1)
    private int memberCount;         // Số lượng thành viên thực tế hiện tại trong mảng

    // Constructor khởi tạo
    public GroupBooking(String bookingId, String customerId, LocalDate checkInDate, 
                        LocalDate expectedCheckOutDate, double deposit, String createdBy, 
                        String note, String groupName, String[] roomIds, int maxMembers) {
        super(bookingId, customerId, checkInDate, expectedCheckOutDate, deposit, createdBy, note);
        this.groupName = groupName;
        this.roomIds = roomIds;
        this.members = new GuestMember[maxMembers]; // Cấp phát mảng thuần lưu GuestMember
        this.memberCount = 0;
    }

    /**
     * Ghi đè phương thức getRooms() từ Booking.
     * Trả về danh sách tất cả các phòng thuộc về đơn đặt của đoàn.
     */
    @Override
    public Room[] getRooms() {
        if (roomObjs == null) {
            return new Room[0];
        }
        return roomObjs;
    }

    /**
     * Thêm một thành viên mới vào danh sách thành viên của đoàn
     */
    public boolean addMember(GuestMember member) {
        if (member == null || memberCount >= members.length) {
            return false; // Mảng đã đầy hoặc đối tượng null
        }
        members[memberCount++] = member; // Gán vào vị trí hiện tại rồi tăng memberCount lên 1
        return true;
    }

    /**
     * Xóa một thành viên khỏi đoàn bằng mã định danh (id/identityNumber)
     */
    public boolean removeMember(String memberId) {
        if (memberId == null) return false;
        
        for (int i = 0; i < memberCount; i++) {
            if (members[i] != null && members[i].getId().equals(memberId)) {
                // Dồn các phần tử phía sau lên 1 vị trí để đè lên phần tử bị xóa
                for (int j = i; j < memberCount - 1; j++) {
                    members[j] = members[j + 1];
                }
                members[--memberCount] = null; // Gán phần tử cuối về null và giảm số lượng
                return true;
            }
        }
        return false; // Không tìm thấy mã thành viên
    }

    // --- GETTER & SETTER ---
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String[] getRoomIds() { return roomIds; }
    public void setRoomIds(String[] roomIds) { this.roomIds = roomIds; }

    public void setRoomObjs(Room[] roomObjs) { this.roomObjs = roomObjs; }

    public GuestMember[] getMembers() { return members; }
    public int getMemberCount() { return memberCount; }
}