package hotel.model;

public class IndividualBooking extends Booking {
    // public IndividualBooking(bookingId, customerId, checkInDate, expectedCheckOutDate, deposit, createdBy, note){
    //     super(bookingId, customerId, checkInDate, expectedCheckOutDate, deposit, createdBy, note);
    // }

    @Override 
    public List<Room> getRooms(){
        return null;
    }
}
