package hotel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages booking requests in a dynamically sized list.
 */
public class BookingRequestList {
    private final ArrayList<BookingRequest> bookingRequests = new ArrayList<>();

    public void add(BookingRequest bookingRequest) {
        if (bookingRequest == null) {
            throw new IllegalArgumentException("Booking request must not be null");
        }
        bookingRequests.add(bookingRequest);
    }

    public BookingRequest get(int index) {
        return bookingRequests.get(index);
    }

    public boolean remove(BookingRequest bookingRequest) {
        return bookingRequests.remove(bookingRequest);
    }

    public BookingRequest remove(int index) {
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
