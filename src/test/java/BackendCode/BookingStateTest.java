package BackendCode;

import BackendCode.service.RentalService;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two places where an absent value had been standing in for a real one.
 */
class BookingStateTest {

    @BeforeEach
    void freshDatabase() {
        DataFiles.reset();
        new CarOwner(0, 0, "1111111111111", "Owner", "03001111111").Add();
        new Customer(0, 0, "2222222222222", "Cust", "03002222222").Add();
        new Car(0, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, CarOwner.SearchByID(1)).Add();
    }

    @Test
    @DisplayName("an open booking has charged nothing, and says so with null rather than 0")
    void openBookingHasNoAmount() {
        RentalService.bookCar(1, 1);

        Booking open = Booking.View().get(0);

        // 0 would be indistinguishable from a genuine zero charge. Nothing bills zero
        // under today's rules, but that is a property of the validation, not of the
        // field, and it is exactly the assumption ReturnTime's 0 sentinel got wrong.
        assertNull(open.getAmountCharged(), "an unreturned booking has no amount, not zero");
    }

    @Test
    @DisplayName("a returned booking records what it charged, and it survives a round trip")
    void returnedBookingRecordsItsAmount() {
        RentalService.bookCar(1, 1);
        Booking open = Booking.View().get(0);
        open.setRentTime(System.currentTimeMillis() - (2 * 60 * 60 * 1000L - 60_000));
        open.Update();

        RentalService.returnCar(1);

        Booking closed = Booking.View().get(0);
        assertNotNull(closed.getAmountCharged());
        assertEquals(200, closed.getAmountCharged().intValue(), "two hours at 100/hr");
        assertEquals(200, Customer.SearchByID(1).getBill(), "and it matches what was billed");
    }

    @Test
    @DisplayName("the amount is read back as null, not defaulted, for an open booking")
    void nullSurvivesTheDatabaseRoundTrip() {
        RentalService.bookCar(1, 1);

        // reload from disk rather than trusting the in-memory object
        Database.close();

        assertNull(Booking.View().get(0).getAmountCharged());
    }

    @Test
    @DisplayName("toString does not throw on a part-built Car or Booking")
    void toStringSurvivesMissingReferences() {
        // The no-arg constructors make these states reachable, and the strings go
        // straight into the confirmation dialogs shown before booking and removal.
        Car bare = new Car();
        assertDoesNotThrow(bare::toString);
        assertTrue(bare.toString().contains("carOwner=null"));

        Booking empty = new Booking();
        assertDoesNotThrow(empty::toString);
        assertTrue(empty.toString().contains("customer=null"));
        assertTrue(empty.toString().contains("car=null"));
    }

    @Test
    @DisplayName("a fully built Car still prints its owner exactly as before")
    void toStringUnchangedWhenEverythingIsPresent() {
        Car car = Car.SearchByID(1);

        String text = car.toString();

        assertTrue(text.contains("carOwner=" + car.getCarOwner().toString().trim().substring(0, 10)),
                "the owner should still be printed in full: " + text);
        assertTrue(text.contains("Corolla"));
    }

    @Test
    @DisplayName("a legacy .ser booking with no recorded amount loads as null")
    void legacyBookingHasNoAmount() throws Exception {
        DataFiles.reset();
        CarOwner owner = new CarOwner(0, 1, "1111111111111", "Owner", "03001111111");
        Customer customer = new Customer(0, 1, "2222222222222", "Cust", "03002222222");
        Car car = new Car(1, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, owner);

        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream("CarOwner.ser"))) {
            out.writeObject(owner);
        }
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream("Customer.ser"))) {
            out.writeObject(customer);
        }
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream("Car.ser"))) {
            out.writeObject(car);
        }
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                new java.io.FileOutputStream("Booking.ser"))) {
            out.writeObject(new Booking(1, customer, car, System.currentTimeMillis(), 0));
        }

        Booking imported = Booking.View().get(0);

        assertEquals(0, imported.getReturnTime(), "the legacy booking was still out");
        assertNull(imported.getAmountCharged(), "so nothing was charged for it");
        assertTrue(new File("rentacar.db").exists());
    }
}
