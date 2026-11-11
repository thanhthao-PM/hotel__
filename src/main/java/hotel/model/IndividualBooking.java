package hotel.model;

import java.time.LocalDate;

public class IndividualBooking extends Booking {
    private String roomId;
    private int numberOfGuests;
    private Room roomObj;

    public IndividualBooking(String bookingId, String requestId, String customerId, 
                             LocalDate checkInDate, LocalDate expectedCheckOutDate, 
                             String createdBy, String note, String roomId, int numberOfGuests) {
        super(bookingId, requestId, customerId, checkInDate, expectedCheckOutDate, createdBy, note);
        this.roomId = roomId;
        this.numberOfGuests = numberOfGuests;
    }

    @Override
    public Room[] getRooms() {
        if (roomObj == null) return new Room[0];
        return new Room[]{ roomObj };
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Room getRoomObj() { return roomObj; }
    public void setRoomObj(Room roomObj) { this.roomObj = roomObj; }
    public int getNumberOfGuests() { return numberOfGuests; }
}