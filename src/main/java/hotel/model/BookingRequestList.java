package hotel.model;

import java.util.ArrayList;
import hotel.util.CheckingExpression;          
import java.util.Collections;
import java.util.List;

/**
 * Quản lý danh sách các yêu cầu đặt phòng (BookingRequest)
 */
public class BookingRequestList {
    private final ArrayList<BookingRequest> bookingRequests = new ArrayList<>();

    public void add(BookingRequest bookingRequest) {
        if (bookingRequest == null) {
            throw new IllegalArgumentException("Booking request must not be null");
        }
        // mã phiếu yêu cầu không được rỗng
        CheckingExpression.checkNotNullOrEmpty(bookingRequest.getRequestId(), "Mã phiếu yêu cầu không được để trống");
        bookingRequests.add(bookingRequest);
    }

    public BookingRequest get(int index) {
        //chặn truy cập ngoài biên
        CheckingExpression.checkValidArrayIndex(index, bookingRequests.size());
        return bookingRequests.get(index);
    }

    public boolean remove(BookingRequest bookingRequest) {
        return bookingRequests.remove(bookingRequest);
    }

    public BookingRequest remove(int index) {
        // chặn truy cập ngoài biên
        CheckingExpression.checkValidArrayIndex(index, bookingRequests.size());

        return bookingRequests.remove(index);
    }

    public List<BookingRequest> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(bookingRequests));
    }

    public int size() {
        return bookingRequests.size();
    }

    public boolean isEmpty() {
        return bookingRequests.isEmpty();
    }

    public void clear() {
        bookingRequests.clear();
    }
}