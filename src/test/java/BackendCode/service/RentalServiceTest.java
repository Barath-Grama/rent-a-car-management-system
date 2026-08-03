package BackendCode.service;

import BackendCode.Booking;
import BackendCode.Car;
import BackendCode.CarOwner;
import BackendCode.Customer;
import BackendCode.Database;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rental rules, now that they are out of the Swing listeners and can be run
 * without opening a window.
 */
class RentalServiceTest {

    @BeforeEach
    void freshDatabase() {
        UserService.signOut();
        Database.close();
        new File(Database.fileName()).delete();
        for (String legacy : new String[]{"Customer.ser", "CarOwner.ser", "Car.ser", "Booking.ser"}) {
            new File(legacy).delete();
        }
//        Removing an owner or a customer is an administrator's action, and the service
//        enforces that rather than trusting the screen. In the running program somebody
//        is always signed in, so the fixture signs in too.
        UserService.signIn("admin", "123".toCharArray());
        new CarOwner(0, 0, "1111111111111", "Owner One", "03001111111").Add();
        new Customer(0, 0, "2222222222222", "Cust One", "03002222222").Add();
        CarOwner owner = CarOwner.SearchByID(1);
        new Car(0, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, owner).Add();
        new Car(0, "Honda", "Civic", "Black", "Familycar", 4, "2021", "Good",
                "BBB-222", 200, owner).Add();
    }

    @Test
    @DisplayName("booking a free car succeeds and marks it as out")
    void bookCarSucceeds() {
        ServiceResult result = RentalService.bookCar(1, 1);

        assertTrue(result.isSuccess(), result.getMessage());
        assertTrue(Car.SearchByID(1).isRented());
        assertEquals(1, Booking.View().size());
    }

    @Test
    @DisplayName("a car that is already out cannot be booked again")
    void bookCarRefusesWhenAlreadyOut() {
        RentalService.bookCar(1, 1);

        ServiceResult second = RentalService.bookCar(1, 1);

        assertFalse(second.isSuccess());
        assertTrue(second.getMessage().contains("already booked"));
        assertEquals(1, Booking.View().size(), "the refused booking must not have been written");
    }

    @Test
    @DisplayName("booking refuses an unknown car or customer without writing anything")
    void bookCarRefusesUnknownIds() {
        assertFalse(RentalService.bookCar(999, 1).isSuccess());
        assertFalse(RentalService.bookCar(1, 999).isSuccess());
        assertEquals(0, Booking.View().size());
    }

    @Test
    @DisplayName("returning a car charges the customer and credits the owner the same amount")
    void returnCarMovesTheMoney() {
        RentalService.bookCar(1, 1);
        backdateHours(2);   // just under two hours, at 100/hr

        ServiceResult result = RentalService.returnCar(1);

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals(200, CarOwner.SearchByID(1).getBalance());
        assertEquals(200, Customer.SearchByID(1).getBill());
        assertFalse(Car.SearchByID(1).isRented());
    }

    @Test
    @DisplayName("two rentals of the same owner's cars add up rather than overwrite")
    void returnsAccumulate() {
        RentalService.bookCar(1, 1);
        backdateHours(2);
        RentalService.returnCar(1);

        RentalService.bookCar(2, 1);
        backdateHours(2);
        RentalService.returnCar(2);

        // This is the money bug, restated at the service level: 100/hr for 2h plus
        // 200/hr for 2h. The old code wrote 400 over the 200 and lost the difference.
        assertEquals(600, CarOwner.SearchByID(1).getBalance());
        assertEquals(600, Customer.SearchByID(1).getBill());
    }

    @Test
    @DisplayName("returning a car that is not out changes nothing")
    void returnCarRefusesWhenNotOut() {
        ServiceResult result = RentalService.returnCar(1);

        assertFalse(result.isSuccess());
        assertEquals(0, CarOwner.SearchByID(1).getBalance());
        assertEquals(0, Customer.SearchByID(1).getBill());
    }

    @Test
    @DisplayName("a car that is out cannot be removed")
    void removeCarRefusesWhileOut() {
        RentalService.bookCar(1, 1);

        ServiceResult result = RentalService.removeCar(1);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("currently booked"));
        assertNotNull(Car.SearchByID(1));
    }

    @Test
    @DisplayName("removing a free car takes its booking history with it")
    void removeCarCascadesHistory() {
        RentalService.bookCar(1, 1);
        backdateHours(1);
        RentalService.returnCar(1);
        assertEquals(1, Booking.View().size());

        assertTrue(RentalService.removeCar(1).isSuccess());

        assertNull(Car.SearchByID(1));
        assertEquals(0, Booking.View().size(), "the finished booking should have cascaded away");
    }

    @Test
    @DisplayName("an owner with a car still out cannot be removed")
    void removeOwnerRefusesWhileACarIsOut() {
        RentalService.bookCar(1, 1);

        ServiceResult result = RentalService.removeOwner(1);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Corolla"), "should name the car that is out");
        assertNotNull(CarOwner.SearchByID(1));
        assertEquals(2, Car.View().size());
    }

    @Test
    @DisplayName("removing an owner takes their cars with them")
    void removeOwnerCascadesCars() {
        assertTrue(RentalService.removeOwner(1).isSuccess());

        assertNull(CarOwner.SearchByID(1));
        assertEquals(0, Car.View().size());
    }

    @Test
    @DisplayName("removing a customer takes their bookings with them")
    void removeCustomerCascadesBookings() {
        RentalService.bookCar(1, 1);
        RentalService.bookCar(2, 1);

        assertTrue(RentalService.removeCustomer(1).isSuccess());

        assertNull(Customer.SearchByID(1));
        assertEquals(0, Booking.View().size());
        assertEquals(2, Car.View().size(), "the cars themselves stay");
    }

    /**
     * Moves the open booking's rent time back so a return bills a known number of
     * hours without the test having to wait.
     * <p>
     * A minute is shaved off so the elapsed time lands inside the hour rather than
     * exactly on it: any started hour rounds up, so backdating by a flat two hours
     * would bill three by the time the test got round to returning the car.
     */
    private void backdateHours(int hours) {
        long millisAgo = hours * 60L * 60 * 1000 - 60_000;
        Booking open = RentalService.openBookingFor(carOfOpenBooking());
        open.setRentTime(System.currentTimeMillis() - millisAgo);
        open.Update();
    }

    private int carOfOpenBooking() {
        for (Booking booking : Booking.View()) {
            if (booking.getReturnTime() == 0) {
                return booking.getCar().getID();
            }
        }
        throw new IllegalStateException("no open booking");
    }

    @Test
    @DisplayName("a failure part way through a transaction leaves no partial write")
    void transactionRollsBackPartialWork() throws SQLException {
        CarOwner owner = CarOwner.SearchByID(1);
        owner.setBalance(500);
        owner.Update();

        // Credit the owner, then hit a UNIQUE violation on the customer's CNIC. If the
        // two were not one transaction the balance would stick and the books would
        // disagree, which is exactly what the old three-separate-writes return did.
        boolean committed = Database.inTransaction(() -> {
            CarOwner inside = CarOwner.SearchByID(1);
            inside.setBalance(inside.getBalance() + 250);
            inside.Update();

            try (PreparedStatement statement = Database.connection().prepareStatement(
                    "INSERT INTO customer (cnic, name, contact_no, bill) VALUES (?, ?, ?, 0)")) {
                statement.setString(1, "2222222222222");   // already taken
                statement.setString(2, "Clashing Person");
                statement.setString(3, "03003333333");
                statement.executeUpdate();
            }
            return true;
        });

        assertFalse(committed, "the unique violation should have failed the transaction");
        assertEquals(500, CarOwner.SearchByID(1).getBalance(),
                "the balance credited inside the failed transaction must have rolled back");
        assertEquals(1, Customer.View().size(), "no second customer should exist");
    }

    @Test
    @DisplayName("work that decides against itself rolls back without an exception")
    void transactionRollsBackOnFalse() {
        boolean committed = Database.inTransaction(() -> {
            CarOwner owner = CarOwner.SearchByID(1);
            owner.setBalance(9999);
            owner.Update();
            return false;   // changed its mind
        });

        assertFalse(committed);
        assertEquals(0, CarOwner.SearchByID(1).getBalance());
    }

    @Test
    @DisplayName("auto-commit is restored after a transaction so later writes still stick")
    void autoCommitRestoredAfterTransaction() throws SQLException {
        Database.inTransaction(() -> false);

        CarOwner owner = CarOwner.SearchByID(1);
        owner.setBalance(42);
        owner.Update();
        Database.close();   // drop anything uncommitted

        assertEquals(42, CarOwner.SearchByID(1).getBalance(),
                "a plain write after a rolled-back transaction should have committed on its own");
        assertTrue(Database.connection().getAutoCommit(), "auto-commit should have been put back");
    }
}
