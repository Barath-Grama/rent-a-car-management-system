package BackendCode.dao;

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
 * Reads and writes {@code car_owner} rows.
 *
 * @author @Barath-Grama
 */
public final class CarOwnerDao {

    private static final Logger LOG = LoggerFactory.getLogger(CarOwnerDao.class);

    private CarOwnerDao() {
    }

    private static CarOwner read(ResultSet rows) throws SQLException {
        return new CarOwner(rows.getInt("balance"), rows.getInt("id"),
                rows.getString("cnic"), rows.getString("name"), rows.getString("contact_no"));
    }

    public static ArrayList<CarOwner> findAll() {
        ArrayList<CarOwner> owners = new ArrayList<>();
        String sql = "SELECT id, cnic, name, contact_no, balance FROM car_owner WHERE deleted = 0 ORDER BY id";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                owners.add(read(rows));
            }
        } catch (SQLException ex) {
            LOG.error("could not list car owners", ex);
        }
        return owners;
    }

    public static CarOwner findById(int id) {
        String sql = "SELECT id, cnic, name, contact_no, balance FROM car_owner WHERE id = ? AND deleted = 0";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read car owner by id", ex);
            return null;
        }
    }

    public static CarOwner findByCnic(String cnic) {
        String sql = "SELECT id, cnic, name, contact_no, balance FROM car_owner WHERE cnic = ? COLLATE NOCASE AND deleted = 0";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setString(1, cnic);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read car owner by CNIC", ex);
            return null;
        }
    }

    public static ArrayList<CarOwner> findByName(String name) {
        ArrayList<CarOwner> owners = new ArrayList<>();
        String sql = "SELECT id, cnic, name, contact_no, balance FROM car_owner "
                + "WHERE name = ? COLLATE NOCASE AND deleted = 0 ORDER BY id";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    owners.add(read(rows));
                }
            }
        } catch (SQLException ex) {
            LOG.error("could not read car owners by name", ex);
        }
        return owners;
    }

    public static boolean insert(CarOwner owner) {
        String sql = "INSERT INTO car_owner (cnic, name, contact_no, balance) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = Database.connection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, owner.getCNIC());
            statement.setString(2, owner.getName());
            statement.setString(3, owner.getContact_No());
            statement.setInt(4, owner.getBalance());
            if (statement.executeUpdate() != 1) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    owner.setID(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException ex) {
            LOG.error("could not save a new car owner", ex);
            return false;
        }
    }

    public static boolean update(CarOwner owner) {
        String sql = "UPDATE car_owner SET cnic = ?, name = ?, contact_no = ?, balance = ? WHERE id = ?";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setString(1, owner.getCNIC());
            statement.setString(2, owner.getName());
            statement.setString(3, owner.getContact_No());
            statement.setInt(4, owner.getBalance());
            statement.setInt(5, owner.getID());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("could not update a car owner", ex);
            return false;
        }
    }

    /**
     * Retires the owner and, with them, their cars. Nothing is erased: the rows stay
     * so that every booking those cars were on still names a real car and a real
     * owner. Both statements run inside the caller's transaction.
     */
    public static boolean delete(int id) {
        try (PreparedStatement cars = Database.connection()
                .prepareStatement("UPDATE car SET deleted = 1 WHERE owner_id = ?");
             PreparedStatement owner = Database.connection()
                .prepareStatement("UPDATE car_owner SET deleted = 1 WHERE id = ?")) {
            cars.setInt(1, id);
            cars.executeUpdate();
            owner.setInt(1, id);
            return owner.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("could not retire a car owner", ex);
            return false;
        }
    }

    /** Reads an owner by id whether or not they have been retired, for history. */
    public static CarOwner findByIdIncludingRetired(int id) {
        String sql = "SELECT id, cnic, name, contact_no, balance FROM car_owner WHERE id = ?";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read car owner by id", ex);
            return null;
        }
    }
}
