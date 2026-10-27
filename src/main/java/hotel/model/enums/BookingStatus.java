package hotel.model.enums;

public enum BookingStatus {
    PENDING,    // Mới khởi tạo, chờ xử lý
    CONFIRMED,  // Đã xác nhận (đã cọc hoặc duyệt)
    CHECKED_IN, // Khách đã làm thủ tục nhận phòng (P4 quản lý)
    COMPLETED,  // Khách đã trả phòng & hoàn tất hóa đơn
    CANCELLED,  // Đơn đặt bị hủy
    NO_SHOW     // Quá giờ nhận phòng nhưng khách không đến
} 
