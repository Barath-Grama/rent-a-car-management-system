package BackendCode.dao;

import BackendCode.Booking;
import BackendCode.Car;
import BackendCode.Customer;
import BackendCode.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes {@code booking} rows, resolving the customer and car each one
 * points at.
 * <p>
 * A booking stores two ids. It used to store serialized copies of the whole customer
 * and car, which is what made it possible to add a bill to a balance that had been
 * frozen at booking time and write the result back over the real one. There is
 * nothing to keep in sync now.
 * <p>
 * {@code return_time} is NULL in the database while a car is out. The domain object
 * still uses 0 for that, so the mapping happens here and nothing above this layer
 * had to change.
 *
 * @author @AbdullahShahid01
 */
public final class BookingDao {

    private static final Logger LOG = LoggerFactory.getLogger(BookingDao.class);

    private static final String SELECT =
            "SELECT b.id, b.rent_time, b.return_time, b.amount_charged, b.customer_id, b.car_id FROM booking b ";

    private BookingDao() {
    }

    private static Booking read(ResultSet rows) throws SQLException {
        long returnTime = rows.getLong("return_time");
        if (rows.wasNull()) {
            returnTime = 0;
        }
        Customer customer = CustomerDao.findById(rows.getInt("customer_id"));
        Car car = CarDao.findById(rows.getInt("car_id"));
        Booking booking = new Booking(rows.getInt("id"), customer, car, rows.getLong("rent_time"), returnTime);
        booking.setAmountCharged(rows.getInt("amount_charged"));
        return booking;
    }

    private static ArrayList<Booking> query(String sql, Object... parameters) {
        ArrayList<Booking> bookings = new ArrayList<>();
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    bookings.add(read(rows));
                }
            }
        } catch (SQLException ex) {
            LOG.error("could not read bookings", ex);
        }
        return bookings;
    }

    public static ArrayList<Booking> findAll() {
        return query(SELECT + "ORDER BY b.id");
    }

    public static ArrayList<Booking> findByCustomer(int customerId) {
        return query(SELECT + "WHERE b.customer_id = ? ORDER BY b.id", customerId);
    }

    public static ArrayList<Booking> findByCar(int carId) {
        return query(SELECT + "WHERE b.car_id = ? ORDER BY b.id", carId);
    }

    /**
     * Matches on the car's current registration number. Changing a car's registration
     * used to orphan its bookings, because the number was compared against the copy
     * frozen inside each booking rather than against the car.
     */
    public static ArrayList<Booking> findByCarRegNo(String regNo) {
        return query(SELECT + "JOIN car c ON c.id = b.car_id "
                + "WHERE c.reg_no = ? COLLATE NOCASE ORDER BY b.id", regNo);
    }

    /**
     * @return the cars that are currently out
     */
    public static ArrayList<Car> findBookedCars() {
        ArrayList<Car> cars = new ArrayList<>();
        String sql = "SELECT car_id FROM booking WHERE return_time IS NULL ORDER BY id";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Car car = CarDao.findById(rows.getInt("car_id"));
                if (car != null) {
                    cars.add(car);
                }
            }
        } catch (SQLException ex) {
            LOG.error("could not list booked cars", ex);
        }
        return cars;
    }

    public static boolean insert(Booking booking) {
        String sql = "INSERT INTO booking (customer_id, car_id, rent_time, return_time, amount_charged) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = Database.connection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(statement, booking);
            if (statement.executeUpdate() != 1) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    booking.setID(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException ex) {
            LOG.error("could not save a new booking", ex);
            return false;
        }
    }

    public static boolean update(Booking booking) {
        String sql = "UPDATE booking SET customer_id = ?, car_id = ?, rent_time = ?, return_time = ?, amount_charged = ? WHERE id = ?";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            bindFields(statement, booking);
            statement.setInt(6, booking.getID());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("could not update a booking", ex);
            return false;
        }
    }

    private static void bindFields(PreparedStatement statement, Booking booking) throws SQLException {
        statement.setInt(1, booking.getCustomer() == null ? 0 : booking.getCustomer().getID());
        statement.setInt(2, booking.getCar() == null ? 0 : booking.getCar().getID());
        statement.setLong(3, booking.getRentTime());
        if (booking.getReturnTime() == 0) {
            statement.setNull(4, Types.INTEGER);
        } else {
            statement.setLong(4, booking.getReturnTime());
        }
        if (booking.getAmountCharged() == 0) {
//            nothing charged until the car is back
            statement.setNull(5, Types.INTEGER);
        } else {
            statement.setInt(5, booking.getAmountCharged());
        }
    }

    /**
     * Deletes one booking by id. A row that is not there is not an error and, unlike
     * the routine this replaces, does not take another record with it.
     */
    public static boolean delete(int id) {
        try (PreparedStatement statement = Database.connection()
                .prepareStatement("DELETE FROM booking WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            LOG.error("could not delete a booking", ex);
            return false;
        }
    }
}
