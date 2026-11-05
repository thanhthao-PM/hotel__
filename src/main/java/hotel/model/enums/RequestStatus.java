package hotel.model.enums;

public enum RequestStatus {
    PENDING_DEPOSIT, // Đã tạo yêu cầu, chờ khách chuyển cọc
    CONFIRMED,       // Đã nhận cọc thành công, giữ chỗ
    CONVERTED,       // Khách đã đến và chuyển thành Booking thành công
    CANCELLED,       // Khách hoặc nhân viên hủy
    EXPIRED,         // Quá hạn cọc mà khách chưa chuyển tiền
    NO_SHOW          // Đến ngày mà khách không tới nhận phòng
}