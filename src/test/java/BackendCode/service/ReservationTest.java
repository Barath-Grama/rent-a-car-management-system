package BackendCode.service;

import BackendCode.Booking;
import BackendCode.Car;
import BackendCode.CarOwner;
import BackendCode.Customer;
import BackendCode.Database;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Holding a car for a window, rather than only renting it out on the spot.
 * <p>
 * A booking now has a plan and a reality: the window it is spoken for, and when it was
 * actually collected and brought back. The two are separate because a reservation
 * exists before either event, and the clash check has to work against the plan.
 */
class ReservationTest {

    private static final long HOUR = 60L * 60 * 1000;
    private static final long DAY = 24 * HOUR;

    private long now;

    @BeforeEach
    void freshDatabase() {
        UserService.signOut();
        Database.close();
        new File(Database.fileName()).delete();
        for (String legacy : new String[]{"Customer.ser", "CarOwner.ser", "Car.ser", "Booking.ser"}) {
            new File(legacy).delete();
        }
        UserService.signIn("admin", "123".toCharArray());
        new CarOwner(0, 0, "1111111111111", "Owner", "03001111111").Add();
        new Customer(0, 0, "2222222222222", "First", "03002222222").Add();
        new Customer(0, 0, "3333333333333", "Second", "03003333333").Add();
        new Car(0, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, CarOwner.SearchByID(1)).Add();
        now = System.currentTimeMillis();
    }

    @Test
    @DisplayName("a car can be held for a window in the future without being collected")
    void reserveHoldsWithoutCollecting() {
        ServiceResult result = RentalService.reserve(1, 1, now + DAY, now + 2 * DAY);

        assertTrue(result.isSuccess(), result.getMessage());
        Booking held = Booking.View().get(0);
        assertTrue(held.isAwaitingCollection(), "nobody has taken the car yet");
        assertNull(held.getRentTime());
        // The car is spoken for but not out, so it is not "rented" in the sense the
        // return and removal rules use.
        assertFalse(Car.SearchByID(1).isRented(), "a held car is not a car that is out");
    }

    @Test
    @DisplayName("two reservations for overlapping windows clash")
    void overlappingWindowsRefused() {
        assertTrue(RentalService.reserve(1, 1, now + DAY, now + 3 * DAY).isSuccess());

        ServiceResult clash = RentalService.reserve(1, 2, now + 2 * DAY, now + 4 * DAY);

        assertFalse(clash.isSuccess());
        assertTrue(clash.getMessage().contains("already spoken for"),
                "the refusal should say when: " + clash.getMessage());
        assertEquals(1, Booking.View().size(), "the clashing hold must not be written");
    }

    @Test
    @DisplayName("windows that only touch at an endpoint do not clash")
    void backToBackWindowsAllowed() {
        assertTrue(RentalService.reserve(1, 1, now + DAY, now + 2 * DAY).isSuccess());

        // A car due back at noon can go straight out again at noon.
        ServiceResult next = RentalService.reserve(1, 2, now + 2 * DAY, now + 3 * DAY);

        assertTrue(next.isSuccess(), next.getMessage());
        assertEquals(2, Booking.View().size());
    }

    @Test
    @DisplayName("a window fully inside another one clashes")
    void containedWindowRefused() {
        RentalService.reserve(1, 1, now + DAY, now + 5 * DAY);

        assertFalse(RentalService.reserve(1, 2, now + 2 * DAY, now + 3 * DAY).isSuccess());
    }

    @Test
    @DisplayName("a window that swallows another one clashes")
    void surroundingWindowRefused() {
        RentalService.reserve(1, 1, now + 2 * DAY, now + 3 * DAY);

        assertFalse(RentalService.reserve(1, 2, now + DAY, now + 5 * DAY).isSuccess());
    }

    @Test
    @DisplayName("a window that ends before it starts is refused")
    void backwardsWindowRefused() {
        ServiceResult result = RentalService.reserve(1, 1, now + 2 * DAY, now + DAY);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("after"));
        assertEquals(0, Booking.View().size());
    }

    @Test
    @DisplayName("collecting a reservation puts the car out")
    void collectingMakesTheCarOut() {
        RentalService.reserve(1, 1, now, now + DAY);
        int bookingId = Booking.View().get(0).getID();

        ServiceResult result = RentalService.collect(bookingId);

        assertTrue(result.isSuccess(), result.getMessage());
        Booking collected = Booking.View().get(0);
        assertTrue(collected.isOut());
        assertNotNull(collected.getRentTime());
        assertTrue(Car.SearchByID(1).isRented(), "now it really is out");
    }

    @Test
    @DisplayName("collecting something that is not waiting to be collected is refused")
    void collectingUnknownRefused() {
        assertFalse(RentalService.collect(999).isSuccess());

        RentalService.bookCar(1, 1);   // already collected on the spot
        assertFalse(RentalService.collect(Booking.View().get(0).getID()).isSuccess(),
                "an immediate rental is already collected");
    }

    @Test
    @DisplayName("an uncollected reservation is billed nothing when it is closed")
    void uncollectedReservationCostsNothing() {
        RentalService.reserve(1, 1, now + DAY, now + 2 * DAY);
        Booking held = Booking.View().get(0);

        assertEquals(0, held.calculateBill(), "a car nobody took cannot be charged for");
    }

    @Test
    @DisplayName("a reserved car cannot be returned until somebody collects it")
    void cannotReturnWhatWasNeverCollected() {
        RentalService.reserve(1, 1, now, now + DAY);

        ServiceResult result = RentalService.returnCar(1);

        assertFalse(result.isSuccess());
        assertEquals(0, CarOwner.SearchByID(1).getBalance());
    }

    @Test
    @DisplayName("collect, use and return still charges for the time actually out")
    void fullCycleChargesForActualUse() {
        RentalService.reserve(1, 1, now, now + DAY);
        int bookingId = Booking.View().get(0).getID();
        RentalService.collect(bookingId);

        // backdate the collection so a known amount of time has passed
        Booking out = Booking.View().get(0);
        out.setRentTime(System.currentTimeMillis() - (2 * HOUR - 60_000));
        out.Update();

        assertTrue(RentalService.returnCar(1).isSuccess());

        assertEquals(200, Customer.SearchByID(1).getBill(), "just under two hours at 100/hr");
        assertEquals(200, CarOwner.SearchByID(1).getBalance());
        assertEquals(200, Booking.View().get(0).getAmountCharged().intValue());
    }

    @Test
    @DisplayName("an immediate booking still works and is collected on the spot")
    void immediateBookingUnchanged() {
        assertTrue(RentalService.bookCar(1, 1).isSuccess());

        Booking booking = Booking.View().get(0);
        assertTrue(booking.isOut(), "a walk-in takes the car straight away");
        assertEquals(booking.getStartsAt(), booking.getRentTime().longValue());
        assertTrue(Car.SearchByID(1).isRented());
    }

    @Test
    @DisplayName("a different car is unaffected by another car's reservation")
    void reservationsArePerCar() {
        new Car(0, "Honda", "Civic", "Black", "Familycar", 4, "2021", "Good",
                "BBB-222", 200, CarOwner.SearchByID(1)).Add();

        RentalService.reserve(1, 1, now + DAY, now + 3 * DAY);

        assertTrue(RentalService.reserve(2, 2, now + DAY, now + 3 * DAY).isSuccess(),
                "the same window on a different car is free");
    }
}
