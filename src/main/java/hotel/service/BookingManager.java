package hotel.service;

import hotel.model.Booking;
import hotel.model.BookingList;
import hotel.model.BookingRequest;
import hotel.model.BookingRequestList;
import hotel.model.IndividualBooking;
import hotel.model.GroupBooking;
import hotel.model.enums.BookingStatus;
import hotel.model.enums.RequestType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Quản lý các đơn Đặt phòng và Yêu cầu thay đổi đặt phòng (Module P3).
 * Sử dụng BookingList và BookingRequestList làm thành phần lưu trữ và thao tác dữ liệu.
 */
public class BookingManager {
    // Sử dụng 2 class BookingList và BookingRequestList chuyên biệt để quản lý danh sách
    private final BookingList bookingList;
    private final BookingRequestList requestList;

    // Bộ đếm sinh mã tự động (Static counter)
    private static int nextBookingId = 1;
    private static int nextRequestId = 1;

    /**
     * Constructor khởi tạo với kích thước mặc định cho BookingList
     */
    public BookingManager() {
        this.bookingList = new BookingList();
        this.requestList = new BookingRequestList();
    }

    /**
     * Constructor cho phép tùy chỉnh sức chứa ban đầu của BookingList
     */
    public BookingManager(int initialCapacity) {
        this.bookingList = new BookingList(initialCapacity);
        this.requestList = new BookingRequestList();
    }

    // =========================================================================
    // 1. THUẬT TOÁN KIỂM TRA CHỐNG ĐẶT TRÙNG PHÒNG (OVERLAP CHECK)
    // =========================================================================

    /**
     * Kiểm tra xem một phòng cụ thể có trống trong khoảng thời gian [checkIn, checkOut] hay không.
     * Thuật toán Overlap: Hai khoảng [A, B] và [C, D] giao nhau khi (A < D) và (B > C).
     */
    public boolean isRoomAvailable(String roomId, LocalDate checkIn, LocalDate checkOut) {
        if (roomId == null || checkIn == null || checkOut == null) return false;
        if (!checkOut.isAfter(checkIn)) return false; // Ngày trả phải sau ngày nhận

        // Sử dụng phương thức getAll() của BookingList để lấy toàn bộ danh sách phòng hiện có
        Booking[] allBookings = bookingList.getAll();

        for (Booking b : allBookings) {
            // Bỏ qua các đơn đặt đã bị HỦY hoặc đã HOÀN THÀNH
            if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.COMPLETED) {
                continue;
            }

            // Kiểm tra xem đơn đặt b có chứa roomId cần tìm hay không
            boolean hasRoom = false;
            if (b instanceof IndividualBooking) {
                IndividualBooking ib = (IndividualBooking) b;
                if (roomId.equals(ib.getRoomId())) {
                    hasRoom = true;
                }
            } else if (b instanceof GroupBooking) {
                GroupBooking gb = (GroupBooking) b;
                String[] roomIds = gb.getRoomIds();
                if (roomIds != null) {
                    for (String rId : roomIds) {
                        if (roomId.equals(rId)) {
                            hasRoom = true;
                            break;
                        }
                    }
                }
            }

            // Nếu đơn này có chứa phòng, tiến hành kiểm tra giao thoa khoảng thời gian
            if (hasRoom) {
                // Điều kiện giao nhau: checkIn_mới < checkOut_cũ AND checkOut_mới > checkIn_cũ
                if (checkIn.isBefore(b.getExpectedCheckOutDate()) && checkOut.isAfter(b.getCheckInDate())) {
                    return false; // Phòng đã bị trùng lịch!
                }
            }
        }
        return true; // Phòng hoàn toàn trống
    }

    // =========================================================================
    // 2. QUẢN LÝ ĐẶT PHÒNG (SỬ DỤNG BookingList)
    // =========================================================================

    /**
     * Thêm một Đặt phòng mới thông qua BookingList.add()
     */
    public boolean addBooking(Booking booking) {
        if (booking == null) return false;
        try {
            bookingList.add(booking);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Bắt ngoại lệ từ BookingList nếu trùng mã, null hoặc mảng đầy
            return false;
        }
    }

    /**
     * Tìm kiếm đơn đặt phòng theo mã bookingId thông qua BookingList.findById()
     */
    public Booking findBookingById(String bookingId) {
        return bookingList.findById(bookingId);
    }

    /**
     * Hủy đặt phòng theo mã
     */
    public boolean cancelBooking(String bookingId) {
        Booking b = findBookingById(bookingId);
        if (b != null && b.getStatus() != BookingStatus.COMPLETED) {
            b.cancel();
            return true;
        }
        return false;
    }

    /**
     * Xóa một Đặt phòng ra khỏi danh sách thông qua BookingList.removeById()
     */
    public boolean removeBookingById(String bookingId) {
        return bookingList.removeById(bookingId);
    }

    /**
     * Sinh mã Đặt phòng tự động (BK001, BK002...)
     */
    public String generateBookingId() {
        return String.format("BK%03d", nextBookingId++);
    }

    /**
     * Lấy danh sách tất cả các Booking hiện tại dưới dạng mảng thuần
     */
    public Booking[] getAllBookings() {
        return bookingList.getAll();
    }

    /**
     * Lấy danh sách Booking đã xác nhận (CONFIRMED) phục vụ module Lưu trú (P4) check-in
     */
    public Booking[] getConfirmedBookings() {
        Booking[] allBookings = bookingList.getAll();
        int count = 0;
        for (Booking b : allBookings) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                count++;
            }
        }

        Booking[] result = new Booking[count];
        int idx = 0;
        for (Booking b : allBookings) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                result[idx++] = b;
            }
        }
        return result;
    }

    // =========================================================================
    // 3. QUẢN LÝ YÊU CẦU THAY ĐỔI (SỬ DỤNG BookingRequestList)
    // =========================================================================

    /**
     * Tạo một Yêu cầu thay đổi mới thông qua BookingRequestList.add()
     */
    public boolean createRequest(BookingRequest request) {
        if (request == null) return false;
        try {
            requestList.add(request);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Duyệt Yêu cầu thay đổi (Chỉ MANAGER/ADMIN gọi)
     */
    public boolean approveRequest(String requestId, String managerId) {
        BookingRequest req = findRequestById(requestId);
        if (req == null || !req.isPending()) return false;

        Booking booking = findBookingById(req.getBookingId());
        if (booking == null) return false;

        // Xử lý áp dụng thay đổi theo từng loại yêu cầu (RequestType)
        if (req.getRequestType() == RequestType.CANCEL) {
            booking.cancel();
        } else if (req.getRequestType() == RequestType.CHANGE_DATE || req.getRequestType() == RequestType.EXTEND) {
            if (req.getNewCheckInDate() != null) {
                booking.setCheckInDate(req.getNewCheckInDate());
            }
            if (req.getNewCheckOutDate() != null) {
                booking.setExpectedCheckOutDate(req.getNewCheckOutDate());
            }
        } else if (req.getRequestType() == RequestType.CHANGE_ROOM) {
            if (booking instanceof IndividualBooking && req.getNewRoomId() != null) {
                ((IndividualBooking) booking).setRoomId(req.getNewRoomId());
            }
        }

        // Đánh dấu yêu cầu đã được duyệt
        req.approve(managerId);
        return true;
    }

    /**
     * Từ chối Yêu cầu thay đổi
     */
    public boolean rejectRequest(String requestId, String managerId) {
        BookingRequest req = findRequestById(requestId);
        if (req != null && req.isPending()) {
            req.reject(managerId);
            return true;
        }
        return false;
    }

    /**
     * Tìm Yêu cầu theo mã requestId thông qua BookingRequestList.getAll()
     */
    public BookingRequest findRequestById(String requestId) {
        if (requestId == null) return null;
        List<BookingRequest> allRequests = requestList.getAll();
        for (BookingRequest req : allRequests) {
            if (requestId.equalsIgnoreCase(req.getRequestId())) {
                return req;
            }
        }
        return null;
    }

    /**
     * Sinh mã Yêu cầu tự động (REQ001, REQ002...)
     */
    public String generateRequestId() {
        return String.format("REQ%03d", nextRequestId++);
    }

    public int getBookingCount() {
        return bookingList.size();
    }

    public int getRequestCount() {
        return requestList.size();
    }
}