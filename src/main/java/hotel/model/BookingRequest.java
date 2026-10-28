package hotel.model;
import java.time.LocalDate;
import java.time.LocalDateTime;
import hotel.model.enums.*;

public class BookingRequest {
    private String requestId;            // Mã yêu cầu (ví dụ: REQ001)
    private String bookingId;            // Mã đơn đặt phòng cần thay đổi (dùng mã để liên kết)
    private RequestType requestType;     // Loại yêu cầu: CANCEL, CHANGE_DATE, CHANGE_ROOM, EXTEND
    private String reason;               // Lý do thay đổi của khách/nhân viên
    
    // Các thuộc tính phục vụ cho từng loại yêu cầu cụ thể
    private LocalDate newCheckInDate;    // Ngày nhận phòng mới (nếu đổi ngày/gia hạn)
    private LocalDate newCheckOutDate;   // Ngày trả phòng mới (nếu đổi ngày/gia hạn)
    private String newRoomId;            // Mã phòng mới (nếu yêu cầu đổi phòng)

    private RequestStatus requestStatus; // Trạng thái: PENDING, APPROVED, REJECTED
    private String requestedBy;          // Mã nhân viên hoặc khách hàng tạo yêu cầu
    private String approvedBy;           // Mã Quản lý (Manager/Admin) duyệt hoặc từ chối
    private LocalDateTime createdAt;     // Thời điểm tạo yêu cầu

    /**
     * Constructor khởi tạo một Yêu cầu mới
     */
    public BookingRequest(String requestId, String bookingId, RequestType requestType, 
                          String reason, String requestedBy) {
        this.requestId = requestId;
        this.bookingId = bookingId;
        this.requestType = requestType;
        this.reason = reason;
        this.requestedBy = requestedBy;
        
        // Mặc định khi vừa gửi lên là PENDING (chờ duyệt)
        this.requestStatus = RequestStatus.PENDING;
        this.createdAt = LocalDateTime.now(); // Tự động lấy thời gian hệ thống
    }

    /**
     * Phương thức cho Quản lý (Manager/Admin) CHẤP NHẬN yêu cầu
     * @param managerId Mã người duyệt
     */
    public void approve(String managerId) {
        this.approvedBy = managerId;
        this.requestStatus = RequestStatus.APPROVED;
    }

    /**
     * Phương thức cho Quản lý (Manager/Admin) TỪ CHỐI yêu cầu
     * @param managerId Mã người từ chối
     */
    public void reject(String managerId) {
        this.approvedBy = managerId;
        this.requestStatus = RequestStatus.REJECTED;
    }

    /**
     * Kiểm tra xem yêu cầu có đang trong trạng thái chờ duyệt không
     */
    public boolean isPending() {
        return this.requestStatus == RequestStatus.PENDING;
    }

    // --- GETTER & SETTER ---
    public String getRequestId() { return requestId; }
    public String getBookingId() { return bookingId; }
    public RequestType getRequestType() { return requestType; }
    public String getReason() { return reason; }
    public RequestStatus getRequestStatus() { return requestStatus; }
    public String getRequestedBy() { return requestedBy; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDate getNewCheckInDate() { return newCheckInDate; }
    public LocalDate getNewCheckOutDate() { return newCheckOutDate; }
    public String getNewRoomId() { return newRoomId; }

    /**
     * Cập nhật khoảng ngày mới nếu yêu cầu liên quan đến CHANGE_DATE hoặc EXTEND
     */
    public void setNewDates(LocalDate checkIn, LocalDate checkOut) {
        this.newCheckInDate = checkIn;
        this.newCheckOutDate = checkOut;
    }

    /**
     * Cập nhật mã phòng mới nếu yêu cầu là CHANGE_ROOM
     */
    public void setNewRoomId(String newRoomId) {
        this.newRoomId = newRoomId;
    }
}
