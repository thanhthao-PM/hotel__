package hotel.model;
import java.time.LocalDate;

public class IndividualBooking extends Booking {
    private String roomId;       // Mã phòng đăng ký (dùng khi lưu/đọc file)
    private int numberOfGuests; // Số lượng khách ở
    private Room roomObj;       // Đối tượng Room (được liên kết sau khi nạp file)

    // Constructor khởi tạo đầy đủ tham số
    public IndividualBooking(String bookingId, String customerId, LocalDate checkInDate, 
                             LocalDate expectedCheckOutDate, double deposit, String createdBy, 
                             String note, String roomId, int numberOfGuests) {
        // Gọi constructor của lớp cha Booking
        super(bookingId, customerId, checkInDate, expectedCheckOutDate, deposit, createdBy, note);
        this.roomId = roomId;
        this.numberOfGuests = numberOfGuests;
    }

    /**
     * Ghi đè (Override) phương thức getRooms() từ lớp cha Booking.
     * Vì đây là đặt phòng cá nhân, hàm trả về mảng chứa đúng 1 phòng.
     */
    @Override
    public Room[] getRooms() {
        if (roomObj == null) {
            return new Room[0]; // Trả về mảng rỗng nếu chưa liên kết đối tượng Room
        }
        return new Room[]{ roomObj }; // Khởi tạo và trả về mảng 1 phần tử
    }

    // --- GETTER & SETTER ---
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    
    public Room getRoomObj() { return roomObj; }
    public void setRoomObj(Room roomObj) { this.roomObj = roomObj; }

    public int getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(int numberOfGuests) { this.numberOfGuests = numberOfGuests; }
}