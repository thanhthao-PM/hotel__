package hotel.model;

import java.time.LocalDate;
import hotel.util.CheckingExpression;   

public class IndividualBooking extends Booking {
    private String roomId;
    private int numberOfGuests;
    private Room roomObj;

    public IndividualBooking(String bookingId, String requestId, String customerId, 
                             LocalDate checkInDate, LocalDate expectedCheckOutDate, 
                             String createdBy, String note, String roomId, int numberOfGuests) {
        super(bookingId, requestId, customerId, checkInDate, expectedCheckOutDate, createdBy, note);
        //kiểm tra phần riêng của đặt phòng cá nhân
        CheckingExpression.checkNotNullOrEmpty(roomId, "Mã phòng không được để trống");
        CheckingExpression.checkPositiveInt(numberOfGuests, "Số khách phải > 0");

        this.roomId = roomId;
        this.numberOfGuests = numberOfGuests;
    }

    @Override
    public Room[] getRooms() {
        if (roomObj == null) return new Room[0];
        return new Room[]{ roomObj };
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { 
        // mã phòng mới không được rỗng
        CheckingExpression.checkNotNullOrEmpty(roomId, "Mã phòng không được để trống");
        this.roomId = roomId; }
    public Room getRoomObj() { return roomObj; }
    public void setRoomObj(Room roomObj) { this.roomObj = roomObj; }
    public int getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(int numberOfGuests) { this.numberOfGuests = numberOfGuests; }
}