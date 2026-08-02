package BackendCode.dao;

import BackendCode.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The aggregate queries behind the dashboard.
 * <p>
 * All of these read {@code booking.amount_charged}, the figure recorded when the car
 * came back, rather than working revenue out from the car's current rent_per_hour.
 * That rate is editable: recomputing would mean every price change quietly rewrote
 * the history of takings for that car.
 *
 * @author @Barath-Grama
 */
public final class ReportingDao {

    private static final Logger LOG = LoggerFactory.getLogger(ReportingDao.class);

    private ReportingDao() {
    }

    /** A single number, for the tiles across the top of the dashboard. */
    private static int scalar(String sql) {
        try (PreparedStatement statement = Database.connection().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException ex) {
            LOG.error("could not run a dashboard figure", ex);
            return 0;
        }
    }

    public static int totalRevenue() {
        return scalar("SELECT COALESCE(SUM(amount_charged), 0) FROM booking");
    }

    public static int completedRentals() {
        return scalar("SELECT COUNT(*) FROM booking WHERE return_time IS NOT NULL");
    }

    public static int carsOut() {
        return scalar("SELECT COUNT(*) FROM booking WHERE return_time IS NULL");
    }

    public static int totalCars() {
        return scalar("SELECT COUNT(*) FROM car");
    }

    public static int totalCustomers() {
        return scalar("SELECT COUNT(*) FROM customer");
    }

    public static int unpaidBills() {
        return scalar("SELECT COALESCE(SUM(bill), 0) FROM customer");
    }

    /**
     * @return share of the fleet currently out, 0 to 100
     */
    public static int utilisationPercent() {
        int cars = totalCars();
        return cars == 0 ? 0 : (int) Math.round(100.0 * carsOut() / cars);
    }

    /**
     * Runs a two-column query into an ordered label/value map.
     */
    private static Map<String, Integer> pairs(String sql) {
        Map<String, Integer> result = new LinkedHashMap<>();
        try (PreparedStatement statement = Database.connection().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.put(rows.getString(1), rows.getInt(2));
            }
        } catch (SQLException ex) {
            LOG.error("could not run a dashboard breakdown", ex);
        }
        return result;
    }

    /**
     * @return revenue per calendar month, oldest first, keyed "YYYY-MM"
     */
    public static Map<String, Integer> revenueByMonth() {
        return pairs(
            "SELECT strftime('%Y-%m', return_time / 1000, 'unixepoch') AS month, "
          + "       SUM(amount_charged) "
          + "FROM booking WHERE return_time IS NOT NULL AND amount_charged IS NOT NULL "
          + "GROUP BY month ORDER BY month");
    }

    /**
     * @return the highest earning cars, most first
     */
    public static Map<String, Integer> topCarsByRevenue(int limit) {
        return pairs(
            "SELECT c.name || ' (' || c.reg_no || ')', SUM(b.amount_charged) AS revenue "
          + "FROM booking b JOIN car c ON c.id = b.car_id "
          + "WHERE b.amount_charged IS NOT NULL "
          + "GROUP BY c.id ORDER BY revenue DESC LIMIT " + limit);
    }

    /**
     * @return the customers who have spent the most, most first
     */
    public static Map<String, Integer> topCustomersBySpend(int limit) {
        return pairs(
            "SELECT cu.name, SUM(b.amount_charged) AS spend "
          + "FROM booking b JOIN customer cu ON cu.id = b.customer_id "
          + "WHERE b.amount_charged IS NOT NULL "
          + "GROUP BY cu.id ORDER BY spend DESC LIMIT " + limit);
    }

    /**
     * @return how many cars each owner has on the fleet, most first
     */
    public static Map<String, Integer> fleetByOwner() {
        return pairs(
            "SELECT o.name, COUNT(c.id) AS cars "
          + "FROM car_owner o LEFT JOIN car c ON c.owner_id = o.id "
          + "GROUP BY o.id ORDER BY cars DESC");
    }
}
