package BackendCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Carrying the legacy {@code .ser} records into the database.
 * <p>
 * The program these files came from deleted records without checking what referred to
 * them, so real data can hold a booking whose car or customer is long gone. Dropping
 * those individually matters: a single orphan reaching the database fails a foreign
 * key, and a failed batch rolls the whole import back, which cost every other record.
 */
class SerImporterTest {

    @BeforeEach
    void freshEverything() {
        DataFiles.reset();
    }

    private static void write(String file, Object... records) throws Exception {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            for (Object record : records) {
                out.writeObject(record);
            }
        }
    }

    private static int count(String table) throws SQLException {
        try (Statement statement = Database.connection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    @Test
    @DisplayName("a complete legacy set is imported with its ids intact")
    void importsCleanData() throws Exception {
        CarOwner owner = new CarOwner(500, 1, "1111111111111", "Owner", "03001111111");
        Customer customer = new Customer(90, 1, "2222222222222", "Cust", "03002222222");
        Car car = new Car(1, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, owner);
        long now = System.currentTimeMillis();

        write("CarOwner.ser", owner);
        write("Customer.ser", customer);
        write("Car.ser", car);
        write("Booking.ser", new Booking(1, customer, car, now - 7200000L, now));

        assertEquals(1, count("car_owner"));
        assertEquals(1, count("customer"));
        assertEquals(1, count("car"));
        assertEquals(1, count("booking"));
        assertEquals(500, CarOwner.SearchByID(1).getBalance(), "balances should survive");
        assertEquals("AAA-111", Car.SearchByID(1).getRegNo());
    }

    @Test
    @DisplayName("a booking whose car is gone is dropped, and everything else still imports")
    void orphanedBookingDoesNotSinkTheImport() throws Exception {
        CarOwner owner = new CarOwner(0, 1, "1111111111111", "Owner", "03001111111");
        CarOwner deletedOwner = new CarOwner(0, 99, "9999999999999", "Gone", "03009999999");
        Customer customer = new Customer(0, 1, "2222222222222", "Cust", "03002222222");

        Car good = new Car(1, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, owner);
        // its owner is absent from CarOwner.ser, so this car cannot be imported
        Car orphaned = new Car(2, "Honda", "Civic", "Black", "Familycar", 4, "2021", "Good",
                "BBB-222", 200, deletedOwner);

        long now = System.currentTimeMillis();
        write("CarOwner.ser", owner);
        write("Customer.ser", customer);
        write("Car.ser", good, orphaned);
        write("Booking.ser",
                new Booking(1, customer, good, now - 7200000L, now),
                new Booking(2, customer, orphaned, now - 3600000L, now));

        // Before the fix this was 0 across the board: the orphan failed the foreign
        // key, the batch aborted, and the rollback took the good records with it.
        assertEquals(1, count("car_owner"));
        assertEquals(1, count("customer"));
        assertEquals(1, count("car"), "the car with no owner should be dropped");
        assertEquals(1, count("booking"), "only the booking pointing at a dropped car goes");
        assertNotNull(Car.SearchByID(1), "the good car must survive");
        assertNull(Car.SearchByID(2));
    }

    @Test
    @DisplayName("a booking whose customer is gone is dropped the same way")
    void orphanedCustomerReferenceDropped() throws Exception {
        CarOwner owner = new CarOwner(0, 1, "1111111111111", "Owner", "03001111111");
        Customer present = new Customer(0, 1, "2222222222222", "Here", "03002222222");
        Customer deleted = new Customer(0, 77, "7777777777777", "Gone", "03007777777");
        Car car = new Car(1, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, owner);
        long now = System.currentTimeMillis();

        write("CarOwner.ser", owner);
        write("Customer.ser", present);
        write("Car.ser", car);
        write("Booking.ser",
                new Booking(1, present, car, now - 7200000L, now),
                new Booking(2, deleted, car, now - 3600000L, now));

        assertEquals(1, count("booking"));
        assertEquals(1, count("car"));
    }

    @Test
    @DisplayName("importing does not run a second time")
    void importIsOncePerDatabase() throws Exception {
        CarOwner owner = new CarOwner(0, 1, "1111111111111", "Owner", "03001111111");
        write("CarOwner.ser", owner);

        assertEquals(1, count("car_owner"));
        Database.close();
        assertEquals(1, count("car_owner"), "reopening must not import the file again");
    }

    @Test
    @DisplayName("no legacy files is the ordinary case and imports nothing")
    void noLegacyFilesIsFine() throws Exception {
        assertEquals(0, count("car_owner"));
        assertEquals(0, count("booking"));
    }
}
