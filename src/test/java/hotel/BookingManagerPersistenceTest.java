package hotel;

import hotel.model.Booking;
import hotel.model.BookingRequest;
import hotel.model.BookingDetail;
import hotel.model.Deposit;
import hotel.model.GroupBooking;
import hotel.model.enums.PaymentMethod;
import hotel.service.BookingManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BookingManagerPersistenceTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadsExistingBookingFilesAndRoundTripsThem() throws IOException {
        Path dataDirectory = temporaryFolder.newFolder().toPath();
        copyBookingFiles(dataDirectory);

        BookingManager manager = new BookingManager(dataDirectory.toString());
        manager.readFile();

        assertEquals(10, manager.getBookingCount());
        assertEquals(9, manager.getRequestCount());
        assertEquals(14, manager.getAllDeposits().length);
        assertNotNull(manager.findBookingById("BK001"));
        assertEquals("RQ001", manager.findRequestById("RQ001").getRequestId());
        assertEquals(2, manager.getBookingDetails("BK003").size());
        assertTrue(manager.findBookingById("BK003") instanceof GroupBooking);

        manager.writeFile();
        assertTrue(Files.readAllLines(dataDirectory.resolve("bookings.txt")).get(0).startsWith("#"));

        BookingManager reloaded = new BookingManager(dataDirectory.toString());
        reloaded.readFile();
        assertEquals(10, reloaded.getBookingCount());
        assertEquals(9, reloaded.getRequestCount());
        assertEquals(14, reloaded.getAllDeposits().length);

        Booking group = reloaded.findBookingById("BK003");
        assertTrue(group instanceof GroupBooking);
        assertEquals(3, ((GroupBooking) group).getMemberIds().length);
        BookingRequest convertedRequest = reloaded.findRequestById("RQ001");
        assertEquals("BK001", convertedRequest.getBookingId());
        Deposit directBookingDeposit = reloaded.getAllDeposits()[6];
        assertEquals("BK004", directBookingDeposit.getBookingId());
        assertEquals("RO301", reloaded.getBookingDetails("BK005").get(0).getRoomId());
        assertEquals("RO103", reloaded.getBookingDetails("BK005").get(0).getPreviousRoomId());
        assertEquals("BK011", reloaded.generateBookingId());
        assertEquals("RQ010", reloaded.generateRequestId());
        assertEquals("BD017", reloaded.generateDetailId());
        assertEquals("DP015", reloaded.generateDepositId());
    }

    @Test
    public void persistsNewRequestDetailsDepositsAndConversion() throws IOException {
        Path dataDirectory = temporaryFolder.newFolder().toPath();
        BookingManager manager = new BookingManager(dataDirectory.toString());
        manager.readFile();

        LocalDate checkIn = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest("RQ001", "CU001", "INDIVIDUAL",
                null, checkIn, checkIn.plusDays(1), "EM001", 1);
        request.addDetail(new BookingDetail("BD001", request.getRequestId(), "RO101",
                1, 100000, 0, checkIn, checkIn.plusDays(1), null));

        assertTrue(manager.createBookingRequest(request));
        Deposit deposit = manager.processDeposit("RQ001", 30000, PaymentMethod.CASH, "EM001", null);
        assertNotNull(deposit);
        assertNotNull(manager.convertRequestToBooking("RQ001", "EM001", null));
        manager.writeFile();

        BookingManager reloaded = new BookingManager(dataDirectory.toString());
        reloaded.readFile();
        assertEquals(1, reloaded.getBookingCount());
        assertEquals(1, reloaded.getRequestCount());
        assertEquals(1, reloaded.getAllDeposits().length);
        assertEquals(1, reloaded.getBookingDetails("BK001").size());
        assertEquals("BK001", reloaded.findRequestById("RQ001").getBookingId());
    }

    private static void copyBookingFiles(Path destination) throws IOException {
        String[] names = {
                "bookings.txt",
                "booking_requests.txt",
                "booking_details.txt",
                "deposits.txt"
        };
        Path sourceDirectory = Path.of(BookingManager.DATA_DIR);
        for (String name : names) {
            Files.copy(sourceDirectory.resolve(name), destination.resolve(name));
        }
    }
}
