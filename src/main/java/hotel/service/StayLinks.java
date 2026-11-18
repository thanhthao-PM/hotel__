package hotel.service;

import hotel.model.Booking;
import hotel.model.BookingDetail;
import hotel.model.BookingRequest;
import hotel.model.CheckOutReceipt;
import hotel.model.ExtraCharge;
import hotel.model.GuestMember;
import hotel.model.HotelService;
import hotel.model.Invoice;
import hotel.model.InvoiceDetail;
import hotel.model.Person;
import hotel.model.Room;
import hotel.model.ServiceUsage;
import hotel.model.StayRecord;
import hotel.model.enums.BookingStatus;
import hotel.model.enums.RequestStatus;
import hotel.model.enums.RoomStatus;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * ĐIỂM NỐI giữa module P4 và các module khác. P4 chỉ gọi các hàm dưới đây, không đọc thẳng
 * dữ liệu nội bộ của module khác (đúng yêu cầu mục 5.5 và 6.6 của kế hoạch).
 *
 * Phần 1: 5 interface (giữ nguyên). Phần 2: 5 adapter nối interface với Manager thật.
 * Phần test vẫn dùng FakeHotel để cài đặt giả cả năm interface.
 *
 * Cách ghép trong MainApp:
 *   StayLinks.BookingAdapter bookingAdapter = new StayLinks.BookingAdapter(bookingManager);
 *   StayManager stay = new StayManager(
 *       new StayLinks.RoomAdapter(roomManager),
 *       new StayLinks.ServiceAdapter(serviceManager),
 *       new StayLinks.PersonAdapter(customerManager),
 *       bookingAdapter,
 *       new StayLinks.InvoiceAdapter(invoiceManager, bookingAdapter),
 *       "data");
 */
public final class StayLinks {
    private StayLinks() { }

    // =====================================================================
    //  PHẦN 1: INTERFACE
    // =====================================================================

    /** Một dòng BookingDetail nhìn từ phía P4 (P3 điền từ BookingDetail thật). */
    public static class BookingLine {
        public final String bookingDetailId;
        public final String roomId;
        public LocalDate checkInDate;
        public LocalDate checkOutDate;
        public final int guestCount;
        /** Giá một đêm đã chốt (BookingDetail.bookingUnitPrice), P5 dùng lập dòng tiền phòng. */
        public final double unitPrice;
        /** Phần cọc của phòng (BookingDetail.depositShare), P5 dùng làm depositApplied. */
        public final double depositShare;

        public BookingLine(String bookingDetailId, String roomId, LocalDate checkInDate,
                           LocalDate checkOutDate, int guestCount) {
            this(bookingDetailId, roomId, checkInDate, checkOutDate, guestCount, 0, 0);
        }

        public BookingLine(String bookingDetailId, String roomId, LocalDate checkInDate,
                           LocalDate checkOutDate, int guestCount, double unitPrice, double depositShare) {
            this.bookingDetailId = bookingDetailId;
            this.roomId = roomId;
            this.checkInDate = checkInDate;
            this.checkOutDate = checkOutDate;
            this.guestCount = guestCount;
            this.unitPrice = unitPrice;
            this.depositShare = depositShare;
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

    // =====================================================================
    //  PHẦN 2: ADAPTER NỐI VỚI MANAGER THẬT
    // =====================================================================

    /** P2: nối RoomGateway với RoomManager (mọi đổi trạng thái đi qua changeRoomStatus). */
    public static class RoomAdapter implements RoomGateway {
        private final RoomManager rooms;

        public RoomAdapter(RoomManager rooms) {
            this.rooms = Objects.requireNonNull(rooms);
        }

        @Override
        public boolean isReadyForCheckIn(String roomId) {
            Room r = rooms.findById(roomId);
            if (r == null || !r.isActive()) return false;
            return r.getStatus() == RoomStatus.AVAILABLE || r.getStatus() == RoomStatus.BOOKED;
        }

        @Override
        public boolean markOccupied(String roomId) {
            return rooms.changeRoomStatus(roomId, RoomStatus.OCCUPIED);
        }

        @Override
        public boolean markCleaning(String roomId) {
            return rooms.changeRoomStatus(roomId, RoomStatus.CLEANING);
        }

        /** Đặt trước nhiều ngày thì phòng vẫn AVAILABLE, nên không phải BOOKED thì coi như đã trống. */
        @Override
        public boolean releaseBookedRoom(String roomId) {
            Room r = rooms.findById(roomId);
            if (r == null) return false;
            if (r.getStatus() != RoomStatus.BOOKED) return true;
            return rooms.changeRoomStatus(roomId, RoomStatus.AVAILABLE);
        }
    }

    /** P2: nối ServiceGateway với ServiceManager. */
    public static class ServiceAdapter implements ServiceGateway {
        private final ServiceManager services;

        public ServiceAdapter(ServiceManager services) {
            this.services = Objects.requireNonNull(services);
        }

        @Override
        public boolean isAvailable(String serviceId) {
            HotelService s = services.findById(serviceId);
            return s != null && s.isAvailable();
        }

        @Override
        public double getCurrentUnitPrice(String serviceId) {
            return services.getCurrentPrice(serviceId);
        }
    }

    /** P1: nối PersonGateway với CustomerManager. */
    public static class PersonAdapter implements PersonGateway {
        private final CustomerManager customers;

        public PersonAdapter(CustomerManager customers) {
            this.customers = Objects.requireNonNull(customers);
        }

        @Override
        public boolean isCustomer(String id) {
            return customers.findCustomerById(id) != null;
        }

        @Override
        public boolean isGuestMember(String id) {
            return customers.findById(id) instanceof GuestMember;
        }

        /** Người lớn (từ 18 tuổi, hoặc chưa có ngày sinh) phải có số giấy tờ; trẻ em thì không cần. */
        @Override
        public boolean isGuestInfoComplete(String id) {
            Person p = customers.findById(id);
            if (p == null || !p.isActive()) return false;
            LocalDate dob = p.getDateOfBirth();
            boolean child = dob != null && Period.between(dob, LocalDate.now()).getYears() < 18;
            if (child) return true;
            String idNumber = p.getIdentityNumber();
            return idNumber != null && !idNumber.trim().isEmpty();
        }

        @Override
        public String getGuestName(String id) {
            Person p = customers.findById(id);
            return p == null ? null : p.getFullName();
        }
    }

    /**
     * P3: nối BookingGateway với BookingManager.
     * Cần BookingManager có thêm hàm: public List<BookingRequest> getAllRequests() { return requestList.getAll(); }
     */
    public static class BookingAdapter implements BookingGateway {
        private final BookingManager bookings;

        public BookingAdapter(BookingManager bookings) {
            this.bookings = Objects.requireNonNull(bookings);
        }

        /** code là mã phiếu yêu cầu, hoặc mã Booking (lần ngược về phiếu yêu cầu qua requestId). */
        private BookingRequest requestOf(String code) {
            BookingRequest r = bookings.findRequestById(code);
            if (r != null) return r;
            Booking b = bookings.findBookingById(code);
            return b == null ? null : bookings.findRequestById(b.getRequestId());
        }

        private BookingDetail findDetail(String bookingDetailId) {
            if (bookingDetailId == null) return null;
            for (BookingRequest req : bookings.getAllRequests()) {
                BookingDetail[] details = req.getDetails();
                for (int i = 0; i < req.getDetailCount(); i++) {
                    if (details[i] != null && bookingDetailId.equals(details[i].getBookingDetailId())) {
                        return details[i];
                    }
                }
            }
            return null;
        }

        private static BookingLine toLine(BookingDetail d) {
            // BookingDetail hiện chỉ có originalRoomId (BookingManager cũng dùng nó làm mã phòng)
            return new BookingLine(d.getBookingDetailId(), d.getOriginalRoomId(),
                    d.getCheckInDate(), d.getCheckOutDate(), d.getGuestCount(),
                    d.getBookingUnitPrice(), d.getDepositShare());
        }

        @Override
        public BookingLine[] getLines(String code) {
            BookingRequest req = requestOf(code);
            if (req == null) return new BookingLine[0];   // TODO: Booking đặt trực tiếp không có phiếu yêu cầu
            BookingDetail[] details = req.getDetails();
            BookingLine[] lines = new BookingLine[req.getDetailCount()];
            for (int i = 0; i < lines.length; i++) lines[i] = toLine(details[i]);
            return lines;
        }

        @Override
        public BookingLine getLine(String bookingDetailId) {
            BookingDetail d = findDetail(bookingDetailId);
            return d == null ? null : toLine(d);
        }

        /** Gọi nhiều lần cùng một code vẫn ra cùng bookingId (đoàn đến lẻ tẻ, mỗi dòng một lần). */
        @Override
        public String resolveBookingForCheckIn(String code, String employeeId) {
            BookingRequest req = bookings.findRequestById(code);
            if (req != null) {
                if (req.getRequestStatus() == RequestStatus.CONFIRMED) {
                    Booking b = bookings.convertRequestToBooking(code, employeeId, "Lập khi nhận phòng");
                    if (b == null) throw new IllegalStateException("Không lập được Booking từ phiếu " + code);
                    return b.getBookingId();
                }
                // Đã chuyển ở lần gọi trước: các BookingDetail đã được gán bookingId
                if (req.getDetailCount() > 0) {
                    String existing = req.getDetails()[0].getBookingId();
                    if (existing != null && !existing.isEmpty()) return existing;
                }
                throw new IllegalStateException("Phiếu " + code + " đang ở trạng thái "
                        + req.getRequestStatus() + ", chưa đủ điều kiện nhận phòng");
            }
            Booking b = bookings.findBookingById(code);
            if (b == null) throw new IllegalStateException("Không tìm thấy phiếu hoặc đặt phòng " + code);
            BookingStatus st = b.getStatus();
            if (st == BookingStatus.CANCELLED || st == BookingStatus.NO_SHOW || st == BookingStatus.COMPLETED) {
                throw new IllegalStateException("Booking " + code + " đang ở trạng thái " + st + ", không nhận phòng được");
            }
            return b.getBookingId();
        }

        @Override
        public void markCheckedIn(String bookingId) {
            Booking b = bookings.findBookingById(bookingId);
            if (b != null) b.changeStatus(BookingStatus.CHECKED_IN);
        }

        @Override
        public void markCompleted(String bookingId) {
            Booking b = bookings.findBookingById(bookingId);
            if (b != null) b.changeStatus(BookingStatus.COMPLETED);
        }

        /** Kiểm tra khoảng [ngày trả cũ, ngày trả mới) không bị phiếu khác giữ rồi mới đổi ngày. */
        @Override
        public boolean extendLine(String bookingDetailId, LocalDate newCheckOutDate) {
            BookingDetail d = findDetail(bookingDetailId);
            if (d == null || newCheckOutDate == null || !newCheckOutDate.isAfter(d.getCheckOutDate())) return false;
            if (!bookings.isRoomAvailable(d.getOriginalRoomId(), d.getCheckOutDate(), newCheckOutDate)) return false;
            d.setCheckOutDate(newCheckOutDate);
            // TODO(P3): cập nhật Booking.expectedCheckOutDate nếu ngày mới vượt ngày trả cũ của Booking
            return true;
        }

        @Override
        public void markNoShow(String code, String employeeId) {
            BookingRequest req = bookings.findRequestById(code);
            if (req != null) {
                // TODO(P3): đặt requestStatus = NO_SHOW và xử lý Deposit (FORFEITED hay hoàn)
                req.cancel("No-show");
                return;
            }
            Booking b = bookings.findBookingById(code);
            if (b == null) throw new IllegalArgumentException("Không tìm thấy phiếu " + code);
            b.changeStatus(BookingStatus.NO_SHOW);
        }
    }

    /** P5: nối InvoiceGateway với InvoiceManager. */
    public static class InvoiceAdapter implements InvoiceGateway {
        /** Phụ thu trả muộn mỗi giờ (lấy theo ca chạy thử: 2 giờ = 100.000). Nhóm chốt lại con số. */
        public static final double LATE_FEE_PER_HOUR = 50_000;

        private final InvoiceManager invoices;
        private final BookingGateway bookings;

        public InvoiceAdapter(InvoiceManager invoices, BookingGateway bookings) {
            this.invoices = Objects.requireNonNull(invoices);
            this.bookings = Objects.requireNonNull(bookings);
        }

        @Override
        public boolean hasInvoice(String stayId) {
            if (stayId == null) return false;
            for (int i = 0; i < invoices.getCount(); i++) {
                if (stayId.equals(invoices.getAllInvoices()[i].getStayId())) return true;
            }
            return false;
        }

        /**
         * Lập Invoice cho một lượt ở đã trả: dòng tiền phòng (giá chốt x số đêm), các dòng dịch vụ chưa hủy,
         * phụ thu trả muộn, depositApplied = depositShare của BookingDetail. Lỗi nào cũng ném RuntimeException,
         * StayManager.checkOut sẽ bắt và chuyển thành cảnh báo.
         */
        @Override
        public String createInvoiceForStay(StayRecord stay, CheckOutReceipt receipt, String employeeId) {
            BookingLine line = bookings.getLine(stay.getBookingDetailId());
            if (line == null) {
                throw new IllegalStateException("Không tìm thấy BookingDetail " + stay.getBookingDetailId());
            }
            Invoice inv = new Invoice(stay.getStayId(), employeeId, line.depositShare);
            String id = inv.getInvoiceId();
            int n = 0;

            long nights = stay.calculateNights();
            inv.addDetail(new InvoiceDetail(id + "-D" + (++n), id, Invoice.LineType.ROOM,
                    "Phòng " + stay.getRoomId() + ", " + nights + " đêm",
                    stay.getRoomId(), null, null, nights, line.unitPrice));

            for (ServiceUsage u : stay.getUsages()) {
                // InvoiceDetail bắt đơn giá > 0, nên dòng đã hủy hoặc miễn phí không đưa vào hóa đơn
                if (u.isCancelled() || u.getUnitPriceAtUse() <= 0) continue;
                inv.addDetail(new InvoiceDetail(id + "-D" + (++n), id, Invoice.LineType.SERVICE,
                        "Dịch vụ " + u.getServiceId(), null, u.getServiceId(), u.getUsageId(),
                        u.getQuantity(), u.getUnitPriceAtUse()));
            }

            if (receipt != null && receipt.getLateHours() > 0) {
                int h = receipt.getLateHours();
                inv.addExtraCharge(new ExtraCharge(id + "-EC1", id, "Trả phòng muộn " + h + " giờ",
                        h * LATE_FEE_PER_HOUR, employeeId));
            }

            invoices.addInvoice(inv);
            return id;
        }
    }
}