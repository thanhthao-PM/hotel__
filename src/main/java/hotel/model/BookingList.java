package hotel.model;

import java.util.Arrays;

/**
 * Manages bookings in a fixed-size array.
 */
public class BookingList {
    private static final int DEFAULT_CAPACITY = 100;

    private final Booking[] bookings;
    private int size;

    public BookingList() {
        this(DEFAULT_CAPACITY);
    }

    public BookingList(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        bookings = new Booking[capacity];
    }

    public void add(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking must not be null");
        }
        if (size == bookings.length) {
            throw new IllegalStateException("Booking list is full");
        }
        if (findById(booking.getBookingId()) != null) {
            throw new IllegalArgumentException("Booking ID already exists: " + booking.getBookingId());
        }
        bookings[size++] = booking;
    }

    public Booking get(int index) {
        checkIndex(index);
        return bookings[index];
    }

    public Booking findById(String bookingId) {
        for (int i = 0; i < size; i++) {
            if (bookingId == null ? bookings[i].getBookingId() == null
                    : bookingId.equals(bookings[i].getBookingId())) {
                return bookings[i];
            }
        }
        return null;
    }

    public boolean removeById(String bookingId) {
        for (int i = 0; i < size; i++) {
            if (bookingId == null ? bookings[i].getBookingId() == null
                    : bookingId.equals(bookings[i].getBookingId())) {
                int elementsToMove = size - i - 1;
                if (elementsToMove > 0) {
                    System.arraycopy(bookings, i + 1, bookings, i, elementsToMove);
                }
                bookings[--size] = null;
                return true;
            }
        }
        return false;
    }

    public Booking[] getAll() {
        return Arrays.copyOf(bookings, size);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        Arrays.fill(bookings, 0, size, null);
        size = 0;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
    }
}
