package BackendCode;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the actual {@code .ser} files the original program wrote in 2018.
 * <p>
 * {@link SerImporterTest} builds its fixtures with the current classes, so both sides
 * of the format always agree and it cannot notice when the format breaks. It did not
 * notice twice: once when {@code serialVersionUID} was pinned to a tidy 1L, and again
 * when {@code RentTime} changed from a primitive {@code long} to a {@code Long} --
 * Java serialization matches fields on name and type and rejects the whole object on a
 * mismatch, so every legacy booking silently vanished.
 * <p>
 * These are the real files, copied in from test resources. Nothing about them can be
 * regenerated, which is the point: they are the only check that the program can still
 * read data it did not write.
 */
class LegacyFormatTest {

    private static final String[] FILES = {"CarOwner.ser", "Customer.ser", "Car.ser", "Booking.ser"};

    @BeforeEach
    void placeTheOriginalFiles() throws Exception {
        DataFiles.reset();
        for (String name : FILES) {
            try (InputStream in = getClass().getResourceAsStream("/legacy/" + name)) {
                assertNotNull(in, name + " should be on the test classpath");
                Files.copy(in, new File(name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Test
    @DisplayName("every record in the original files is imported")
    void allRecordsSurvive() {
        assertEquals(3, CarOwner.View().size(), "owners");
        assertEquals(3, Customer.View().size(), "customers");
        assertEquals(4, Car.View().size(), "cars");
        assertEquals(3, Booking.View().size(), "bookings");
    }

    @Test
    @DisplayName("the original ids are kept, gaps and all")
    void idsArePreserved() {
        StringBuilder customerIds = new StringBuilder();
        for (Customer customer : Customer.View()) {
            customerIds.append(customer.getID()).append(' ');
        }
        // The original data has holes where records were deleted. Re-numbering would
        // detach every booking from the customer and car it was actually for.
        assertEquals("1 6 8 ", customerIds.toString());
    }

    @Test
    @DisplayName("a booking still names the customer and car it was for")
    void bookingsResolveTheirReferences() {
        for (Booking booking : Booking.View()) {
            assertNotNull(booking.getCustomer(), "booking " + booking.getID() + " lost its customer");
            assertNotNull(booking.getCar(), "booking " + booking.getID() + " lost its car");
        }
        assertEquals("Muhammad Ali", Booking.View().get(0).getCustomer().getName());
        assertEquals("Corvette", Booking.View().get(0).getCar().getName());
    }

    @Test
    @DisplayName("a legacy booking gets a reservation window it never recorded")
    void legacyBookingsGainAWindow() {
        for (Booking booking : Booking.View()) {
            assertTrue(booking.getStartsAt() > 0,
                    "booking " + booking.getID() + " should have been given a start");
            assertTrue(booking.getEndsAt() > booking.getStartsAt(),
                    "and an end after it");
        }
    }

    @Test
    @DisplayName("the booking still on loan is still on loan, and the finished ones are finished")
    void rentalStateSurvives() {
        int out = 0;
        int finished = 0;
        for (Booking booking : Booking.View()) {
            if (booking.isOut()) {
                out++;
            } else if (booking.getReturnTime() != 0) {
                finished++;
            }
        }
        assertEquals(1, out, "one car was still out when the original program was last used");
        assertEquals(2, finished);
    }

    @Test
    @DisplayName("money recorded in the original files survives")
    void balancesSurvive() {
        // The customer with a bill in the original data still has it.
        int totalBilled = 0;
        for (Customer customer : Customer.View()) {
            totalBilled += customer.getBill();
        }
        assertTrue(totalBilled > 0, "the original data has outstanding bills; they should not vanish");
    }
}
