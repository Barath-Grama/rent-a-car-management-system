package BackendCode;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carries the old {@code .ser} records into the database, once.
 * <p>
 * Records used to be stored as raw serialized objects in four files. Those files are
 * still in the repository, and anyone who has been running the program has their own,
 * so the switch to SQLite must not throw that data away. On the first run against an
 * empty database this reads whatever is there and inserts it.
 * <p>
 * The old ids are preserved rather than re-issued, because the legacy Booking records
 * only make sense against the customer and car ids they were saved with.
 * <p>
 * Nothing else in the program deserializes anything any more. This class is the only
 * reason the model classes still implement {@link java.io.Serializable}, and the only
 * reason their {@code serialVersionUID} values must stay as the originals.
 *
 * @author @Barath-Grama
 */
public final class SerImporter {

    private static final Logger LOG = LoggerFactory.getLogger(SerImporter.class);

    private SerImporter() {
    }

    /**
     * Imports the legacy files if there is anything to import and the database has
     * not been populated yet. Doing nothing is the normal case.
     *
     * @param connection an open connection with the schema already applied
     */
    static void importIfPresent(Connection connection) {
        try {
            if (!isEmpty(connection) || !legacyFilesExist()) {
                return;
            }
            ArrayList<CarOwner> owners = read("CarOwner.ser");
            ArrayList<Customer> customers = read("Customer.ser");
            ArrayList<Car> cars = read("Car.ser");
            ArrayList<Booking> bookings = read("Booking.ser");

            if (owners.isEmpty() && customers.isEmpty() && cars.isEmpty() && bookings.isEmpty()) {
                return;
            }

            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                insertOwners(connection, owners);
                insertCustomers(connection, customers);
                insertCars(connection, cars, owners);
                insertBookings(connection, bookings);
                connection.commit();
                LOG.info("imported legacy records: {} owners, {} customers, {} cars, {} bookings",
                        owners.size(), customers.size(), cars.size(), bookings.size());
            } catch (SQLException ex) {
                connection.rollback();
                LOG.error("legacy import rolled back", ex);
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException ex) {
            LOG.error("legacy import failed", ex);
        }
    }

    private static boolean legacyFilesExist() {
        return new File("CarOwner.ser").exists() || new File("Customer.ser").exists()
                || new File("Car.ser").exists() || new File("Booking.ser").exists();
    }

    /** Only import into a database nobody has put anything into yet. */
    private static boolean isEmpty(Connection connection) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM car_owner) + (SELECT COUNT(*) FROM customer) "
                + "+ (SELECT COUNT(*) FROM car) + (SELECT COUNT(*) FROM booking) AS total";
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            return rows.next() && rows.getInt("total") == 0;
        }
    }

    /**
     * Reads every object out of one legacy file. The files hold a bare sequence of
     * objects with no count in front, so the end is found by running into EOF.
     */
    @SuppressWarnings("unchecked")
    private static <T> ArrayList<T> read(String fileName) {
        ArrayList<T> records = new ArrayList<>();
        File file = new File(fileName);
        if (!file.exists()) {
            return records;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            while (true) {
                try {
                    records.add((T) in.readObject());
                } catch (EOFException end) {
                    break;
                } catch (ClassNotFoundException ex) {
                    LOG.error("could not read a legacy .ser file", ex);
                    break;
                }
            }
        } catch (EOFException empty) {
//            a zero-length file, which is what an interrupted write left behind
        } catch (IOException ex) {
            LOG.warn("could not read " + fileName + ": " + ex);
        }
        return records;
    }

    private static void insertOwners(Connection connection, ArrayList<CarOwner> owners) throws SQLException {
        String sql = "INSERT INTO car_owner (id, cnic, name, contact_no, balance) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (CarOwner owner : owners) {
                statement.setInt(1, owner.getID());
                statement.setString(2, owner.getCNIC());
                statement.setString(3, owner.getName());
                statement.setString(4, owner.getContact_No());
                statement.setInt(5, owner.getBalance());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertCustomers(Connection connection, ArrayList<Customer> customers) throws SQLException {
        String sql = "INSERT INTO customer (id, cnic, name, contact_no, bill) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Customer customer : customers) {
                statement.setInt(1, customer.getID());
                statement.setString(2, customer.getCNIC());
                statement.setString(3, customer.getName());
                statement.setString(4, customer.getContact_No());
                statement.setInt(5, customer.getBill());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertCars(Connection connection, ArrayList<Car> cars,
                                   ArrayList<CarOwner> owners) throws SQLException {
        Map<Integer, Boolean> knownOwners = new HashMap<>();
        for (CarOwner owner : owners) {
            knownOwners.put(owner.getID(), Boolean.TRUE);
        }
        String sql = "INSERT INTO car (id, maker, name, colour, type, seating_capacity, model, "
                + "condition, reg_no, rent_per_hour, owner_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Car car : cars) {
                CarOwner owner = car.getCarOwner();
//                the embedded owner copy could point at an owner that had since been
//                deleted; such a car cannot satisfy the foreign key, so it is skipped
                if (owner == null || !knownOwners.containsKey(owner.getID())) {
                    LOG.warn("skipping car " + car.getID() + ": its owner no longer exists");
                    continue;
                }
                statement.setInt(1, car.getID());
                statement.setString(2, car.getMaker());
                statement.setString(3, car.getName());
                statement.setString(4, car.getColour());
                statement.setString(5, car.getType());
                statement.setInt(6, car.getSeatingCapacity());
                statement.setString(7, car.getModel());
                statement.setString(8, car.getCondition());
                statement.setString(9, car.getRegNo());
                statement.setInt(10, car.getRentPerHour());
                statement.setInt(11, owner.getID());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertBookings(Connection connection, ArrayList<Booking> bookings) throws SQLException {
        String sql = "INSERT INTO booking (id, customer_id, car_id, rent_time, return_time, amount_charged) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Booking booking : bookings) {
                if (booking.getCustomer() == null || booking.getCar() == null) {
                    LOG.warn("skipping booking " + booking.getID() + ": incomplete record");
                    continue;
                }
                statement.setInt(1, booking.getID());
                statement.setInt(2, booking.getCustomer().getID());
                statement.setInt(3, booking.getCar().getID());
                statement.setLong(4, booking.getRentTime());
                if (booking.getReturnTime() == 0) {
                    statement.setNull(5, Types.INTEGER);
                    statement.setNull(6, Types.INTEGER);
                } else {
                    statement.setLong(5, booking.getReturnTime());
//                    The legacy format never stored what was charged, so it has to be
//                    reconstructed from the rate saved alongside the booking. This
//                    runs after the migration that backfills existing rows, so
//                    imported rentals would otherwise report no revenue at all.
                    statement.setInt(6, booking.calculateBill());
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
