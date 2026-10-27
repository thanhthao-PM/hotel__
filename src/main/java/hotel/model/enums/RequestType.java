package hotel.model.enums;

public enum RequestType {
    CANCEL,       // Yêu cầu hủy đặt phòng
    CHANGE_DATE,  // Yêu cầu thay đổi ngày ở (Check-in/Check-out)
    CHANGE_ROOM,  // Yêu cầu đổi sang phòng khác
    EXTEND        // Yêu cầu gia hạn thêm ngày ở
}
