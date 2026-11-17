package hotel.model;

import java.time.LocalDate;

/**
 * ĐIỂM NỐI giữa module P4 và các module khác. P4 chỉ gọi các hàm dưới đây, không đọc thẳng
 * dữ liệu nội bộ của module khác (đúng yêu cầu mục 5.5 và 6.6 của kế hoạch).
 * Mỗi người P1, P2, P3, P5 cài đặt (hoặc viết lớp bọc) interface của mình.
 * Phần test dùng FakeHotel để cài đặt giả cả năm interface.
 */
public final class StayLinks {
    private StayLinks() { }

    /** Một dòng BookingDetail nhìn từ phía P4 (P3 điền từ BookingDetail thật). */
    public static class BookingLine {
        public final String bookingDetailId;
        public final String roomId;
        public LocalDate checkInDate;
        public LocalDate checkOutDate;
        public final int guestCount;

        public BookingLine(String bookingDetailId, String roomId, LocalDate checkInDate,
                           LocalDate checkOutDate, int guestCount) {
            this.bookingDetailId = bookingDetailId;
            this.roomId = roomId;
            this.checkInDate = checkInDate;
            this.checkOutDate = checkOutDate;
            this.guestCount = guestCount;
        }
    }

    /** P2 cài đặt (RoomManager). Mọi đổi trạng thái phòng chỉ đi qua đây. */
    public interface RoomGateway {
        /** Phòng sẵn sàng đón khách: AVAILABLE, hoặc BOOKED cho đúng khách đến hôm nay. */
        boolean isReadyForCheckIn(String roomId);
        /** Chuyển sang OCCUPIED. false nếu chuyển không hợp lệ. */
        boolean markOccupied(String roomId);
        /** Chuyển sang CLEANING sau khi trả hoặc đổi phòng. */
        boolean markCleaning(String roomId);
        /** Trả phòng đang giữ (BOOKED) về AVAILABLE khi khách no-show. */
        boolean releaseBookedRoom(String roomId);
    }

    /** P2 cài đặt (ServiceManager). */
    public interface ServiceGateway {
        boolean isAvailable(String serviceId);
        /** Giá hiện hành. P4 sẽ chép giá này vào ServiceUsage.unitPriceAtUse. */
        double getCurrentUnitPrice(String serviceId);
    }

    /** P1 cài đặt (CustomerManager, GuestMember). */
    public interface PersonGateway {
        boolean isCustomer(String id);
        boolean isGuestMember(String id);
        /** Giấy tờ, thông tin bắt buộc của người ở đã đầy đủ (người lớn phải có giấy tờ). */
        boolean isGuestInfoComplete(String id);
        String getGuestName(String id);
    }

    /** P3 cài đặt (BookingManager). code là mã BookingRequest hoặc mã Booking. */
    public interface BookingGateway {
        /** Các dòng phòng của phiếu. Trả mảng rỗng hoặc null nếu mã không tồn tại. */
        BookingLine[] getLines(String code);
        BookingLine getLine(String bookingDetailId);
        /**
         * Đảm bảo đã có Booking và trả về bookingId. Nếu code là BookingRequest đã CONFIRMED thì P3
         * chuyển thành Booking (CONVERTED, gán bookingId cho BookingDetail và Deposit); nếu là Booking
         * thì kiểm tra hợp lệ. Gọi nhiều lần cho cùng một code phải cho cùng kết quả.
         * Ném IllegalStateException kèm lý do nếu chưa đủ điều kiện (chưa cọc đủ, đã hủy, hết hạn...).
         */
        String resolveBookingForCheckIn(String code, String employeeId);
        void markCheckedIn(String bookingId);
        void markCompleted(String bookingId);
        /** P3 kiểm tra phòng không bị đặt kế tiếp rồi mới đổi ngày trả của BookingDetail. false nếu từ chối. */
        boolean extendLine(String bookingDetailId, LocalDate newCheckOutDate);
        /** P3 chuyển phiếu sang NO_SHOW và xử lý Deposit (FORFEITED hay hoàn). */
        void markNoShow(String code, String employeeId);
    }

    /** P5 cài đặt (InvoiceManager). */
    public interface InvoiceGateway {
        /**
         * Lập Invoice cho lượt ở vừa trả. P5 đọc stay.getUsages(), receipt.getLateHours() và (tùy chọn)
         * CheckInReceipt.getEarlyHours() để tạo ExtraCharge. Trả về mã hóa đơn.
         */
        String createInvoiceForStay(StayRecord stay, CheckOutReceipt receipt, String employeeId);
        boolean hasInvoice(String stayId);
    }
}

