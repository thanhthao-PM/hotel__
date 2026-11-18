package hotel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quản lý danh sách Chi tiết đặt phòng (BookingDetail) bằng ArrayList.
 */
public class bookingDetailList {
    private final ArrayList<BookingDetail> details = new ArrayList<>();

    public void add(BookingDetail detail) {
        if (detail == null) {
            throw new IllegalArgumentException("BookingDetail không được là null");
        }
        details.add(detail);
    }

    public BookingDetail get(int index) {
        return details.get(index);
    }

    public BookingDetail findById(String bookingDetailId) {
        for (BookingDetail d : details) {
            if (bookingDetailId == null ? d.getBookingDetailId() == null
                    : bookingDetailId.equalsIgnoreCase(d.getBookingDetailId())) {
                return d;
            }
        }
        return null;
    }

    public boolean remove(BookingDetail detail) {
        return details.remove(detail);
    }

    public BookingDetail remove(int index) {
        return details.remove(index);
    }

    public boolean removeById(String bookingDetailId) {
        BookingDetail detail = findById(bookingDetailId);
        if (detail != null) {
            return details.remove(detail);
        }
        return false;
    }

    public List<BookingDetail> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(details));
    }

    public int size() {
        return details.size();
    }

    public boolean isEmpty() {
        return details.isEmpty();
    }

    public void clear() {
        details.clear();
    }
}