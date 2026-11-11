package hotel.model.enums;

public enum DepositStatus {
    HELD,      // Tiền cọc đang được giữ
    APPLIED,   // Tiền cọc đã được áp dụng trừ vào hóa đơn khi checkout
    REFUNDED,  // Tiền cọc đã được hoàn trả lại cho khách
    FORFEITED  // Tiền cọc bị thu hồi/mất cọc do khách hủy muộn hoặc no-show
}