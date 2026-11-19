package hotel;

import hotel.model.Customer;
import hotel.model.enums.Gender;
import hotel.service.CustomerManager;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CustomerManagerTest {
    @Test
    public void removesCustomerFromActiveCollection() {
        CustomerManager manager = new CustomerManager();
        Customer customer = new Customer("CU999", "Test Customer", "0912345678",
                Gender.OTHER, "123456789012", LocalDate.of(1990, 1, 1),
                "Test address", true, "test@example.com");

        assertTrue(manager.add(customer));
        assertTrue(manager.remove(customer.getId()));

        assertNull(manager.findById(customer.getId()));
        assertEquals(0, manager.getCount());
        assertFalse(manager.remove(customer.getId()));
    }
}
