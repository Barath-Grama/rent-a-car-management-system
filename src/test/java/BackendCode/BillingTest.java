package BackendCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Rent is charged per hour and any started hour counts as a whole one.
 */
class BillingTest {

    private static final long MINUTE = 60L * 1000;
    private static final long HOUR = 60 * MINUTE;
    private static final long RENT_TIME = 1_000_000L;

    private Car car;

    @BeforeEach
    void seed() {
        DataFiles.reset();
        new CarOwner(0, 0, "1111111111111", "Owner", "03001111111").Add();
        new Customer(0, 0, "2222222222222", "Cust", "03002222222").Add();
        new Car(0, "Maker", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, CarOwner.SearchByID(1)).Add();
        car = Car.SearchByID(1);
    }

    private Booking bookingLasting(long duration) {
        return new Booking(1, Customer.SearchByID(1), car, RENT_TIME,
                duration == 0 ? 0 : RENT_TIME + duration);
    }

    @Test
    @DisplayName("a part hour is charged as a whole hour")
    void partialHourBillsAsFullHour() {
        // Integer division truncated, so 1h59m billed as one hour of rent.
        assertEquals(200, bookingLasting(119 * MINUTE).calculateBill());
        assertEquals(100, bookingLasting(1 * MINUTE).calculateBill());
        assertEquals(100, bookingLasting(59 * MINUTE).calculateBill());
    }

    @Test
    @DisplayName("an exact number of hours is not rounded up past itself")
    void exactHoursBillExactly() {
        assertEquals(100, bookingLasting(HOUR).calculateBill());
        assertEquals(200, bookingLasting(2 * HOUR).calculateBill());
        assertEquals(2400, bookingLasting(24 * HOUR).calculateBill());
    }

    @Test
    @DisplayName("a booking that has not been returned yet bills nothing")
    void openBookingBillsZero() {
        // ReturnTime is 0 while the car is out. Subtracting the rent time from that
        // produced a huge negative number, which would have been added to a balance.
        assertEquals(0, bookingLasting(0).calculateBill());
    }

    @Test
    @DisplayName("a return time before the rent time bills nothing rather than a negative")
    void backwardsClockBillsZero() {
        Booking backwards = new Booking(1, Customer.SearchByID(1), car, RENT_TIME, RENT_TIME - HOUR);
        assertEquals(0, backwards.calculateBill());
    }
}
