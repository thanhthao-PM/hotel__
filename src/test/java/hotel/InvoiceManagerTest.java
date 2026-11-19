package hotel;

import hotel.model.BookingDetail;
import hotel.model.Invoice;
import hotel.model.StayGuest;
import hotel.model.StayRecord;
import hotel.service.InvoiceManager;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class InvoiceManagerTest {
    @Test
    public void createsRoomChargeFromBookedNightlyPrice() {
        LocalDate checkIn = LocalDate.of(2026, 1, 10);
        StayRecord stay = new StayRecord("BK001", "BD001", "R101");
        stay.addGuest(new StayGuest(stay.getStayId(), "CU001", true));
        stay.checkIn(LocalDateTime.of(2026, 1, 10, 14, 0));
        stay.checkOut(LocalDateTime.of(2026, 1, 12, 11, 0));
        BookingDetail bookingDetail = new BookingDetail("BD001", "RQ001", "BK001",
                "R101", null, 1, 150000, 30000,
                checkIn, checkIn.plusDays(2), null);
        InvoiceManager manager = new InvoiceManager(1);

        Invoice invoice = manager.createInvoiceForStay(
                stay, bookingDetail.getBookingUnitPrice(), bookingDetail.getDepositShare(),
                null, "EM001");

        assertEquals(300000, invoice.calculateTotal(), 0.001);
        assertEquals(30000, invoice.getDepositApplied(), 0.001);
        assertSame(invoice, manager.createInvoiceForStay(
                stay, bookingDetail.getBookingUnitPrice(), bookingDetail.getDepositShare(),
                null, "EM001"));
        assertEquals(1, manager.getCount());
    }
}
