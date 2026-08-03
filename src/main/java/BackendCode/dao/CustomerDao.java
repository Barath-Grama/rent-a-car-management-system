package BackendCode.dao;

import BackendCode.Customer;
import BackendCode.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes {@code customer} rows.
 * <p>
 * Every statement is prepared, so a name with an apostrophe in it is data rather than
 * syntax. Failures are reported as {@code false} to the caller, which is what puts an
 * error on screen instead of a success message.
 *
 * @author @Barath-Grama
 */
public final class CustomerDao {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerDao.class);

    private CustomerDao() {
    }

    /** Reads a customer by id whether or not it has been retired, for history. */
    public static Customer findByIdIncludingRetired(int id) {
        String sql = "SELECT id, cnic, name, contact_no, bill FROM customer WHERE id = ?";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read customer by id", ex);
            return null;
        }
    }

    private static Customer read(ResultSet rows) throws SQLException {
        return new Customer(rows.getInt("bill"), rows.getInt("id"),
                rows.getString("cnic"), rows.getString("name"), rows.getString("contact_no"));
    }

    public static ArrayList<Customer> findAll() {
        ArrayList<Customer> customers = new ArrayList<>();
        String sql = "SELECT id, cnic, name, contact_no, bill FROM customer WHERE deleted = 0 ORDER BY id";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                customers.add(read(rows));
            }
        } catch (SQLException ex) {
            LOG.error("could not list customers", ex);
        }
        return customers;
    }

    public static Customer findById(int id) {
        String sql = "SELECT id, cnic, name, contact_no, bill FROM customer WHERE id = ? AND deleted = 0";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read customer by id", ex);
            return null;
        }
    }

    public static Customer findByCnic(String cnic) {
        String sql = "SELECT id, cnic, name, contact_no, bill FROM customer WHERE cnic = ? COLLATE NOCASE AND deleted = 0";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setString(1, cnic);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException ex) {
            LOG.error("could not read customer by CNIC", ex);
            return null;
        }
    }

    public static ArrayList<Customer> findByName(String name) {
        ArrayList<Customer> customers = new ArrayList<>();
        String sql = "SELECT id, cnic, name, contact_no, bill FROM customer "
                + "WHERE name = ? COLLATE NOCASE AND deleted = 0 ORDER BY id";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    customers.add(read(rows));
                }
            }
        } catch (SQLException ex) {
            LOG.error("could not read customers by name", ex);
        }
        return customers;
    }

    /**
     * Inserts the customer and writes the id the database assigned back onto it.
     *
     * @return false if the row was not written, so the caller can say so
     */
    public static boolean insert(Customer customer) {
        String sql = "INSERT INTO customer (cnic, name, contact_no, bill) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = Database.connection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customer.getCNIC());
            statement.setString(2, customer.getName());
            statement.setString(3, customer.getContact_No());
            statement.setInt(4, customer.getBill());
            if (statement.executeUpdate() != 1) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
//                    AUTOINCREMENT never hands out an id that has been used before,
//                    which is what the old high-water-mark counter file was for
                    customer.setID(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException ex) {
            LOG.error("could not save a new customer", ex);
            return false;
        }
    }

    public static boolean update(Customer customer) {
        String sql = "UPDATE customer SET cnic = ?, name = ?, contact_no = ?, bill = ? WHERE id = ?";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setString(1, customer.getCNIC());
            statement.setString(2, customer.getName());
            statement.setString(3, customer.getContact_No());
            statement.setInt(4, customer.getBill());
            statement.setInt(5, customer.getID());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("could not update a customer", ex);
            return false;
        }
    }

    /**
     * Retires the customer. The row stays so their rental history keeps its meaning;
     * every read filters retired records out, so they disappear from the program.
     */
    public static boolean delete(int id) {
        try (PreparedStatement statement = Database.connection()
                .prepareStatement("UPDATE customer SET deleted = 1 WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LOG.error("could not delete a customer", ex);
            return false;
        }
    }
}
