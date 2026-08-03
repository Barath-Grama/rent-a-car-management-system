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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Giving up on a reservation nobody turned up for.
 * <p>
 * Without this one forgotten hold takes a car off the fleet permanently: the window
 * blocks every overlapping booking, and nothing ever closes it, because closing a
 * booking is what returning the car does and the car was never collected.
 */
class ReservationExpiryTest {

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

    /** Moves a hold's window back so its grace period has already run out. */
    private void makeOverdue(int bookingId, long by) {
        for (Booking booking : Booking.View()) {
            if (booking.getID() == bookingId) {
                long shift = RentalService.COLLECTION_GRACE + by;
                booking.setStartsAt(booking.getStartsAt() - shift);
                booking.setEndsAt(booking.getEndsAt() - shift);
                booking.Update();
                return;
            }
        }
        throw new IllegalStateException("no booking " + bookingId);
    }

    @Test
    @DisplayName("a hold inside its grace period is left alone")
    void withinGraceIsKept() {
        RentalService.reserve(1, 1, now - HOUR, now + DAY);   // due an hour ago

        assertEquals(0, RentalService.expireStaleReservations(),
                "an hour late is inside the two-hour grace period");
        assertTrue(Booking.View().get(0).isAwaitingCollection());
    }

    @Test
    @DisplayName("a hold past its grace period is given up, and says when")
    void pastGraceIsGivenUp() {
        RentalService.reserve(1, 1, now, now + DAY);
        makeOverdue(Booking.View().get(0).getID(), HOUR);

        assertEquals(1, RentalService.expireStaleReservations());

        Booking given = Booking.View().get(0);
        assertTrue(given.isExpired());
        assertFalse(given.isAwaitingCollection(), "it is no longer waiting for anybody");
        assertNotNull(given.getExpiredAt());
    }

    @Test
    @DisplayName("a given-up hold stops blocking the car")
    void givenUpHoldReleasesTheCar() {
        RentalService.reserve(1, 1, now, now + DAY);
        makeOverdue(Booking.View().get(0).getID(), HOUR);

        // The window still overlaps, so before expiry this was refused. The whole point
        // is that a no-show does not cost the car its next booking.
        ServiceResult second = RentalService.reserve(1, 2, now, now + DAY);

        assertTrue(second.isSuccess(), second.getMessage());
        assertEquals(2, Booking.View().size());
    }

    @Test
    @DisplayName("a live hold still blocks the car")
    void liveHoldStillBlocks() {
        RentalService.reserve(1, 1, now + DAY, now + 2 * DAY);

        assertFalse(RentalService.reserve(1, 2, now + DAY, now + 2 * DAY).isSuccess(),
                "a hold that has not even started must still block");
    }

    @Test
    @DisplayName("collecting a hold that was given up is refused, and says so specifically")
    void collectingAGivenUpHoldIsRefused() {
        RentalService.reserve(1, 1, now, now + DAY);
        int bookingId = Booking.View().get(0).getID();
        makeOverdue(bookingId, HOUR);

        ServiceResult result = RentalService.collect(bookingId);

        assertFalse(result.isSuccess());
        // "no such reservation" would be misleading: there was one, and it lapsed.
        assertTrue(result.getMessage().contains("given up"), result.getMessage());
        assertFalse(Car.SearchByID(1).isRented());
    }

    @Test
    @DisplayName("booking sweeps first, so a stale hold never blocks a walk-in")
    void reserveSweepsBeforeChecking() {
        RentalService.reserve(1, 1, now, now + DAY);
        makeOverdue(Booking.View().get(0).getID(), HOUR);

        // No explicit sweep here: reserve does it.
        assertTrue(RentalService.bookCar(1, 2).isSuccess());
        assertTrue(Car.SearchByID(1).isRented());
    }

    @Test
    @DisplayName("sweeping twice gives up nothing the second time")
    void sweepIsIdempotent() {
        RentalService.reserve(1, 1, now, now + DAY);
        makeOverdue(Booking.View().get(0).getID(), HOUR);

        assertEquals(1, RentalService.expireStaleReservations());
        assertEquals(0, RentalService.expireStaleReservations(),
                "a hold already given up must not be touched again");
    }

    @Test
    @DisplayName("a collected booking is never given up, however old")
    void collectedBookingsAreSafe() {
        RentalService.bookCar(1, 1);
        Booking out = Booking.View().get(0);
        out.setStartsAt(now - 30 * DAY);
        out.setEndsAt(now - 29 * DAY);
        out.Update();

        assertEquals(0, RentalService.expireStaleReservations(),
                "the customer has the car; the hold is not stale, the rental is overdue");
        assertTrue(Car.SearchByID(1).isRented());
    }

    @Test
    @DisplayName("a finished booking is never given up")
    void finishedBookingsAreSafe() {
        RentalService.bookCar(1, 1);
        Booking out = Booking.View().get(0);
        out.setRentTime(now - 3 * HOUR);
        out.Update();
        RentalService.returnCar(1);

        assertEquals(0, RentalService.expireStaleReservations());
        assertFalse(Booking.View().get(0).isExpired());
    }

    @Test
    @DisplayName("the lapse rule is judged against the moment it is asked about")
    void lapseRuleIsExplicitAboutTime() {
        Booking hold = new Booking(0, Customer.SearchByID(1), Car.SearchByID(1),
                now, now + DAY, null, 0);

        assertFalse(hold.hasLapsed(RentalService.COLLECTION_GRACE, now + HOUR),
                "an hour in is still inside the grace period");
        assertTrue(hold.hasLapsed(RentalService.COLLECTION_GRACE, now + 3 * HOUR),
                "three hours in is past it");
    }
}
