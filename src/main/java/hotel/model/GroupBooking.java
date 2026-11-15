package hotel.model;

import java.time.LocalDate;
import hotel.util.CheckingExpression;   

public class GroupBooking extends Booking {
    private String groupName;
    private String[] roomIds;
    private Room[] roomObjs;
    private GuestMember[] members;
    private int memberCount;

    public GroupBooking(String bookingId, String requestId, String customerId, 
                        LocalDate checkInDate, LocalDate expectedCheckOutDate, 
                        String createdBy, String note, String groupName, String[] roomIds, int maxMembers) {
        super(bookingId, requestId, customerId, checkInDate, expectedCheckOutDate, createdBy, note);
        // kiểm tra phần riêng của đặt phòng đoàn
        CheckingExpression.checkNotNullOrEmpty(groupName, "Tên đoàn không được để trống");
        if (roomIds == null || roomIds.length == 0) {
            throw new IllegalArgumentException("Đặt phòng đoàn phải có ít nhất một phòng");
        }
        for (int i = 0; i < roomIds.length; i++) {
            CheckingExpression.checkNotNullOrEmpty(roomIds[i], "Mã phòng thứ " + (i + 1) + " không được để trống");
        }
        CheckingExpression.checkPositiveInt(maxMembers, "Số thành viên tối đa phải > 0");

        this.groupName = groupName;
        this.roomIds = roomIds;
        this.members = new GuestMember[maxMembers];
        this.memberCount = 0;
    }

    @Override
    public Room[] getRooms() {
        if (roomObjs == null) return new Room[0];
        return roomObjs;
    }

    public String getGroupName() { return groupName; }
    public String[] getRoomIds() { return roomIds; }
    public void setRoomObjs(Room[] roomObjs) { this.roomObjs = roomObjs; }
    public GuestMember[] getMembers() { return members; }
    public int getMemberCount() { return memberCount; }
}