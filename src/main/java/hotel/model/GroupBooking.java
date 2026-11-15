package hotel.model;

import java.time.LocalDate;

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
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setRoomIds(String[] roomIds) { this.roomIds = roomIds; }
    public Room[] getRoomObjs() { return roomObjs;}
    public void setRoomObjs(Room[] roomObjs) { this.roomObjs = roomObjs; }
    public GuestMember[] getMembers() { return members; }
    public void setMembers(GuestMember[] members) { this.members = members; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}