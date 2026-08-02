package BackendCode;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One test per defect that was found and fixed in this codebase.
 * <p>
 * Every case here reproduced a real failure before its fix: money silently
 * disappearing, bookings being deleted that nobody asked to delete, retired IDs coming
 * back and re-binding old records to different people. They are written as regressions
 * rather than as general coverage, so a failure points straight at which fix broke.
 */
class PersistenceRegressionTest {

    private static final long ONE_HOUR = 60L * 60 * 1000;

    @BeforeEach
    void freshFiles() {
        DataFiles.reset();
    }

    /** Adds one owner, one customer and {@code carCount} cars, all owned by that owner. */
    private CarOwner seed(int carCount, int... rentPerHour) {
        new CarOwner(0, 0, "1111111111111", "Owner One", "03001111111").Add();
        new Customer(0, 0, "2222222222222", "Cust One", "03002222222").Add();
        CarOwner owner = CarOwner.SearchByID(1);
        for (int i = 0; i < carCount; i++) {
            new Car(0, "Maker", "Car" + (i + 1), "White", "Familycar", 4, "2020", "Good",
                    "AAA-11" + i, rentPerHour[i], owner).Add();
        }
        return owner;
    }

    @Test
    @DisplayName("unbooking two cars accumulates the owner balance instead of overwriting it")
    void unbookingTwoCarsAccumulatesOwnerBalance() {
        seed(2, 100, 200);
        Customer customer = Customer.SearchByID(1);
        long now = System.currentTimeMillis();
        new Booking(0, customer, Car.SearchByID(1), now, 0).Add();
        new Booking(0, customer, Car.SearchByID(2), now, 0).Add();

        returnCar(1, 2 * ONE_HOUR);   // 100/hr for 2h = 200
        returnCar(2, 2 * ONE_HOUR);   // 200/hr for 2h = 400

        // Before the fix each booking held its own stale copy of the owner, whose
        // balance was still 0, so the second return wrote 400 over the first 200.
        assertEquals(600, CarOwner.SearchByID(1).getBalance(),
                "owner balance should be the sum of both rentals");
        assertEquals(600, Customer.SearchByID(1).getBill(),
                "customer bill should be the sum of both rentals");
    }

    /** Mirrors what Booking_UnBookCar does when the UnBook button is pressed. */
    private void returnCar(int carId, long elapsed) {
        ArrayList<Booking> bookings = Booking.SearchByCarID(carId);
        Booking last = bookings.get(bookings.size() - 1);
        last.setReturnTime(last.getRentTime() + elapsed);
        last.Update();

        int bill = last.calculateBill();
        CarOwner owner = CarOwner.SearchByID(last.getCar().getCarOwner().getID());
        owner.setBalance(owner.getBalance() + bill);
        owner.Update();

        Customer customer = Customer.SearchByID(last.getCustomer().getID());
        customer.setBill(customer.getBill() + bill);
        customer.Update();
    }

    @Test
    @DisplayName("getUnbookedCars() excludes cars that are currently out")
    void getUnbookedCarsExcludesBookedCars() {
        seed(2, 100, 200);
        new Booking(0, Customer.SearchByID(1), Car.SearchByID(1), System.currentTimeMillis(), 0).Add();

        ArrayList<Car> unbooked = Booking.getUnbookedCars();

        // The two lists come from different files, so the same car is two different
        // objects. Car has no equals(), so the old ArrayList.remove(Object) never matched.
        assertEquals(1, unbooked.size(), "only the free car should be listed");
        assertEquals(2, unbooked.get(0).getID());
    }

    @Test
    @DisplayName("Booking.Remove() with an unknown id leaves every record alone")
    void removeWithUnknownIdLeavesFileUntouched() {
        seed(2, 100, 200);
        Customer customer = Customer.SearchByID(1);
        long now = System.currentTimeMillis();
        new Booking(0, customer, Car.SearchByID(1), now, 0).Add();
        new Booking(0, customer, Car.SearchByID(2), now, 0).Add();

        Booking ghost = new Booking();
        ghost.setID(999);
        ghost.Remove();

        // The old loop shifted elements without shrinking the list, then wrote
        // size()-1 records -- so a miss silently dropped the newest booking.
        assertEquals(2, Booking.View().size(), "a no-op removal must not delete anything");
    }

    @Test
    @DisplayName("Booking.Remove() removes the right record from any position")
    void removeDeletesOnlyTheRequestedBooking() {
        seed(3, 100, 200, 300);
        Customer customer = Customer.SearchByID(1);
        long now = System.currentTimeMillis();
        for (int carId = 1; carId <= 3; carId++) {
            new Booking(0, customer, Car.SearchByID(carId), now, 0).Add();
        }

        Booking middle = new Booking();
        middle.setID(2);
        middle.Remove();

        ArrayList<Booking> left = Booking.View();
        assertEquals(2, left.size());
        assertEquals(1, left.get(0).getID());
        assertEquals(3, left.get(1).getID());
    }

    @Test
    @DisplayName("removing the newest record does not hand its id to the next one added")
    void removingNewestRecordDoesNotRecycleItsId() {
        new Customer(0, 0, "1111111111111", "A", "03001111111").Add();
        new Customer(0, 0, "2222222222222", "B", "03002222222").Add();
        new Customer(0, 0, "3333333333333", "C", "03003333333").Add();

        Customer.SearchByID(3).Remove();
        new Customer(0, 0, "4444444444444", "D", "03004444444").Add();

        // Deriving the next id from the highest one still on file is not enough:
        // deleting the newest record lowers that maximum. A recycled id would make an
        // old booking re-bind to whichever new record inherited the number.
        assertEquals(4, Customer.View().get(2).getID(), "D must not inherit C's id");
        assertNotNull(Customer.SearchByID(4));
    }

    @Test
    @DisplayName("the id counter reseeds from existing records when its file is missing")
    void idCounterReseedsFromExistingRecords() {
        new Customer(0, 0, "1111111111111", "A", "03001111111").Add();
        new java.io.File("Customer.id").delete();   // an install predating the counter

        new Customer(0, 0, "2222222222222", "B", "03002222222").Add();

        assertEquals(2, Customer.SearchByCNIC("2222222222222").getID());
    }

    @Test
    @DisplayName("renaming a customer shows through in the bookings that reference them")
    void renamingCustomerShowsThroughInBookings() {
        seed(1, 100);
        new Booking(0, Customer.SearchByID(1), Car.SearchByID(1), System.currentTimeMillis(), 0).Add();

        Customer customer = Customer.SearchByID(1);
        customer.setName("Renamed Person");
        customer.Update();

        // Booking.ser holds its own serialized copy of the customer, so without
        // re-resolving on read the Booking Details table showed the old name forever.
        assertEquals("Renamed Person", Booking.View().get(0).getCustomer().getName());
    }

    @Test
    @DisplayName("renaming an owner shows through in the cars they own")
    void renamingOwnerShowsThroughInCars() {
        seed(1, 100);

        CarOwner owner = CarOwner.SearchByID(1);
        owner.setName("Renamed Owner");
        owner.Update();

        assertEquals("Renamed Owner", Car.SearchByID(1).getCarOwner().getName());
    }

    @Test
    @DisplayName("changing a car's registration number does not orphan its bookings")
    void changingRegNoKeepsBookingsFindable() {
        seed(1, 100);
        new Booking(0, Customer.SearchByID(1), Car.SearchByID(1), System.currentTimeMillis(), 0).Add();

        Car car = Car.SearchByID(1);
        car.setRegNo("ZZZ-999");
        car.Update();

        assertEquals(1, Booking.SearchByCarRegNo("ZZZ-999").size(),
                "the booking should be findable under the new registration number");
    }

    @Test
    @DisplayName("isRented() distinguishes a car that is out from one that is free")
    void isRentedReflectsOutstandingBookings() {
        seed(2, 100, 200);
        new Booking(0, Customer.SearchByID(1), Car.SearchByID(1), System.currentTimeMillis(), 0).Add();

        // Car_Remove and CarOwner_Remove gate on this before deleting anything.
        assertTrue(Car.SearchByID(1).isRented(), "car 1 is still out");
        assertFalse(Car.SearchByID(2).isRented(), "car 2 was never booked");
    }

    @Test
    @DisplayName("a save reports whether it actually reached the file")
    void saveMethodsReportSuccess() {
        // Callers show "Record Successfully Added !" off the back of this, so a void
        // return meant the message appeared whether or not anything was written.
        assertTrue(new Customer(0, 0, "1111111111111", "A", "03001111111").Add());
        Customer saved = Customer.SearchByID(1);
        assertTrue(saved.Update());
        assertTrue(saved.Remove());
    }
}
