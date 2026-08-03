package BackendCode.dao;

import BackendCode.Car;
import BackendCode.CarOwner;
import BackendCode.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes {@code car} rows, joining each one to its owner.
 * <p>
 * The join is the point. Car.ser used to hold a serialized copy of the owner taken
 * when the car was saved, so renaming an owner left every one of their cars still
 * showing the old name. Reading the owner through a join means a car can never
 * disagree with the record it points at.
 *
 * @author @Barath-Grama
 */
public final class CarDao {

    private static final Logger LOG = LoggerFactory.getLogger(CarDao.class);

    /** Both tables have id, name and cnic columns, so the owner's are aliased. */
    private static final String SELECT =
            "SELECT c.id, c.maker, c.name, c.colour, c.type, c.seating_capacity, c.model, "
          + "       c.condition, c.reg_no, c.rent_per_hour, "
          + "       o.id AS owner_id, o.cnic AS owner_cnic, o.name AS owner_name, "
          + "       o.contact_no AS owner_contact, o.balance AS owner_balance "
          + "FROM car c JOIN car_owner o ON o.id = c.owner_id ";

    /** Live cars only; retired ones stay in the table for history. */
    private static final String LIVE = "c.deleted = 0";

    private CarDao() {
    }

    private static Car read(ResultSet rows) throws SQLException {
        CarOwner owner = new CarOwner(rows.getInt("owner_balance"), rows.getInt("owner_id"),
                rows.getString("owner_cnic"), rows.getString("owner_name"),
                rows.getString("owner_contact"));
        return new Car(rows.getInt("id"), rows.getString("maker"), rows.getString("name"),
                rows.getString("colour"), rows.getString("type"), rows.getInt("seating_capacity"),
                rows.getString("model"), rows.getString("condition"), rows.getString("reg_no"),
                rows.getInt("rent_per_hour"), owner);
    }

    public static ArrayList<Car> findAll() {
        ArrayList<Car> cars = new ArrayList<>();
        try (PreparedStatement statement = Database.connection().prepareStatement(SELECT + "WHERE " + LIVE + " ORDER BY c.id");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                cars.add(read(rows));
            }
        } catch (SQLException ex) {
            LOG.error("could not list cars", ex);
        }
        return cars;
    }

    /** Reads a car by id whether or not it has been retired, for history. */
    public static Car findByIdIncludingRetired(int id) {
        try (PreparedStatement statement = Database.connection().prepareStatement(SELECT + "WHERE c.id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read car by id", ex);
            return null;
        }
    }

    public static Car findById(int id) {
        try (PreparedStatement statement = Database.connection().prepareStatement(SELECT + "WHERE c.id = ? AND " + LIVE)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read car by id", ex);
            return null;
        }
    }

    public static Car findByRegNo(String regNo) {
        try (PreparedStatement statement = Database.connection()
                .prepareStatement(SELECT + "WHERE c.reg_no = ? COLLATE NOCASE AND " + LIVE)) {
            statement.setString(1, regNo);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read car by registration number", ex);
            return null;
        }
    }

    public static ArrayList<Car> findByName(String name) {
        ArrayList<Car> cars = new ArrayList<>();
        try (PreparedStatement statement = Database.connection()
                .prepareStatement(SELECT + "WHERE c.name = ? COLLATE NOCASE AND " + LIVE + " ORDER BY c.id")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    cars.add(read(rows));
                }
            }
        } catch (SQLException ex) {
            LOG.error("could not read cars by name", ex);
        }
        return cars;
    }

    public static ArrayList<Car> findByOwner(int ownerId) {
        ArrayList<Car> cars = new ArrayList<>();
        try (PreparedStatement statement = Database.connection()
                .prepareStatement(SELECT + "WHERE c.owner_id = ? AND " + LIVE + " ORDER BY c.id")) {
            statement.setInt(1, ownerId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    cars.add(read(rows));
                }
            }
        } catch (SQLException ex) {
            LOG.error("could not list an owner's cars", ex);
        }
        return cars;
    }

    /**
     * @return true if the car has a booking that has not been returned yet
     */
    public static boolean isRented(int carId) {
        String sql = "SELECT 1 FROM booking WHERE car_id = ? "
                + "AND rent_time IS NOT NULL AND return_time IS NULL LIMIT 1";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setInt(1, carId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException ex) {
            LOG.error("could not check whether a car is out", ex);
            return false;
        }
    }

    /**
     * @return the cars with no outstanding booking, in id order
     */
    public static ArrayList<Car> findUnbooked() {
        ArrayList<Car> cars = new ArrayList<>();
        String sql = SELECT + "WHERE " + LIVE + " AND c.id NOT IN "
                + "(SELECT car_id FROM booking WHERE rent_time IS NOT NULL "
                + " AND return_time IS NULL) ORDER BY c.id";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                cars.add(read(rows));
            }
        } catch (SQLException ex) {
            LOG.error("could not list unbooked cars", ex);
        }
        return cars;
    }

    public static boolean insert(Car car) {
        String sql = "INSERT INTO car (maker, name, colour, type, seating_capacity, model, "
                + "condition, reg_no, rent_per_hour, owner_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = Database.connection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(statement, car);
            if (statement.executeUpdate() != 1) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    car.setID(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException ex) {
            LOG.error("could not save a new car", ex);
            return false;
        }
    }

    public static boolean update(Car car) {
        String sql = "UPDATE car SET maker = ?, name = ?, colour = ?, type = ?, seating_capacity = ?, "
                + "model = ?, condition = ?, reg_no = ?, rent_per_hour = ?, owner_id = ? WHERE id = ?";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            bindFields(statement, car);
            statement.setInt(11, car.getID());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("could not update a car", ex);
            return false;
        }
    }

    private static void bindFields(PreparedStatement statement, Car car) throws SQLException {
        statement.setString(1, car.getMaker());
        statement.setString(2, car.getName());
        statement.setString(3, car.getColour());
        statement.setString(4, car.getType());
        statement.setInt(5, car.getSeatingCapacity());
        statement.setString(6, car.getModel());
        statement.setString(7, car.getCondition());
        statement.setString(8, car.getRegNo());
        statement.setInt(9, car.getRentPerHour());
        statement.setInt(10, car.getCarOwner() == null ? 0 : car.getCarOwner().getID());
    }

    /**
     * Retires the car. The row stays so the bookings it was on still name a real car.
     */
    public static boolean delete(int id) {
        try (PreparedStatement statement = Database.connection()
                .prepareStatement("UPDATE car SET deleted = 1 WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("could not delete a car", ex);
            return false;
        }
    }
}
