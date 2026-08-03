package BackendCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one SQLite connection the program uses, and the schema it runs against.
 * <p>
 * The database is a single file in the working directory, which keeps the program as
 * easy to run as it was when it wrote {@code .ser} files there. The connection is
 * opened once and kept, because this is a single-user desktop application and every
 * caller runs on the event dispatch thread.
 * <p>
 * Foreign keys are off by default in SQLite and must be switched on per connection,
 * so that is done here rather than left to the schema file: the {@code ON DELETE
 * CASCADE} rules only do anything once this pragma is set.
 *
 * @author @Barath-Grama
 */
public final class Database {

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);

    private static final String DEFAULT_FILE = "rentacar.db";

    private static Connection connection;
    private static String fileName = DEFAULT_FILE;
    /** True while {@link #inTransaction} is running, so a nested call can be refused. */
    private static boolean running;

    private Database() {
    }

    /**
     * Points the database at a different file. Only useful to tests, which need a
     * fresh database per case; must be called before the connection is first opened
     * or it closes the existing one.
     *
     * @param name the SQLite file to use
     */
    public static synchronized void useFile(String name) {
        close();
        fileName = name;
    }

    /**
     * @return the file the database currently lives in
     */
    public static synchronized String fileName() {
        return fileName;
    }

    /**
     * Opens the connection on first use, applies the schema, and imports any legacy
     * {@code .ser} data that has not been carried over yet.
     *
     * @return the shared connection
     * @throws SQLException if the database cannot be opened or prepared
     */
    public static synchronized Connection connection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        applySchema(connection);
        migrate(connection);
        SerImporter.importIfPresent(connection);
        return connection;
    }

    /**
     * Brings a database created by an earlier version up to date.
     * <p>
     * The schema script only uses {@code CREATE TABLE IF NOT EXISTS}, so a table that
     * already exists is left exactly as it was. Anything added to a table after the
     * fact has to be applied here as well, or it only ever appears on machines that
     * started from scratch.
     */
    private static void migrate(Connection target) throws SQLException {
        addAmountCharged(target);
        addDeletedFlags(target);
    }

    /**
     * Records are retired rather than erased, so every table that can be removed from
     * needs somewhere to say so. Bookings are deliberately excluded: they are the
     * history being preserved, and nothing deletes one.
     */
    private static void addDeletedFlags(Connection target) throws SQLException {
        for (String table : new String[]{"car_owner", "customer", "car"}) {
            if (hasColumn(target, table, "deleted")) {
                continue;
            }
            LOG.info("migrating: adding {}.deleted", table);
            try (Statement statement = target.createStatement()) {
                statement.execute("ALTER TABLE " + table
                        + " ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
            }
        }
    }

    private static void addAmountCharged(Connection target) throws SQLException {
        if (hasColumn(target, "booking", "amount_charged")) {
            return;
        }
        LOG.info("migrating: adding booking.amount_charged");
        try (Statement statement = target.createStatement()) {
            statement.execute("ALTER TABLE booking ADD COLUMN amount_charged INTEGER");
//            Existing finished rentals never recorded what they charged, so the only
//            figure available is a reconstruction from the car's current rate. That is
//            the very thing the column exists to avoid, but it beats reporting nothing
//            for past rentals, and it is a one-off: every return from now on records
//            the real amount.
            int filled = statement.executeUpdate(
                    "UPDATE booking SET amount_charged = ("
                    + "  SELECT c.rent_per_hour * MAX(1, (booking.return_time - booking.rent_time + 3599999) / 3600000)"
                    + "  FROM car c WHERE c.id = booking.car_id)"
                    + " WHERE return_time IS NOT NULL");
            if (filled > 0) {
                LOG.info("backfilled {} finished bookings from the current rate (approximate)", filled);
            }
        }
    }

    private static boolean hasColumn(Connection target, String table, String column) throws SQLException {
        try (Statement statement = target.createStatement();
             java.sql.ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                if (column.equalsIgnoreCase(rows.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void applySchema(Connection target) throws SQLException {
        String script = readSchema();
        try (Statement statement = target.createStatement()) {
//            SQLite's JDBC driver executes one statement per call
            for (String sql : script.split(";")) {
                if (!sql.trim().isEmpty()) {
                    statement.execute(sql);
                }
            }
        }
    }

    private static String readSchema() throws SQLException {
        InputStream in = Database.class.getResourceAsStream("/schema.sql");
        if (in == null) {
            throw new SQLException("schema.sql is missing from the classpath");
        }
        StringBuilder script = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
//                strip comments so they cannot be mistaken for a statement when split on ';'
                int comment = line.indexOf("--");
                script.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
            }
        } catch (IOException ex) {
            throw new SQLException("could not read schema.sql", ex);
        }
        return script.toString();
    }

    /**
     * A piece of work that must either happen completely or not at all.
     */
    @FunctionalInterface
    public interface UnitOfWork {
        /**
         * @return false to roll back without an exception, when the work decides
         *         part way through that it should not go ahead
         * @throws SQLException to roll back because something failed
         */
        boolean run() throws SQLException;
    }

    /**
     * Runs the given work as one transaction against the shared connection.
     * <p>
     * Every DAO writes through that same connection, so turning auto-commit off here
     * enrolls all of them. This is what stops a return from crediting an owner and
     * then failing to charge the customer, which under the old code left the two
     * disagreeing with no way to tell.
     *
     * @param work the statements to run together
     * @return true if the work committed
     */
    public static synchronized boolean inTransaction(UnitOfWork work) {
        Connection target;
        try {
            target = connection();
        } catch (SQLException ex) {
            LOG.error("could not open the database for a transaction", ex);
            return false;
        }
//        SQLite has no nested transactions, and this method is reentrant on a single
//        thread. An inner call would commit or roll back the outer one's work when it
//        finished, which surfaces later as inexplicably missing data. Nothing needs
//        nesting today; refusing it turns a silent corruption into a loud failure if
//        anything ever tries.
        if (running) {
            LOG.error("a transaction is already running on this connection; "
                    + "nesting would commit the outer one early");
            return false;
        }
        boolean previousAutoCommit = true;
        running = true;
        try {
            previousAutoCommit = target.getAutoCommit();
            target.setAutoCommit(false);
            if (work.run()) {
                target.commit();
                return true;
            }
            target.rollback();
            return false;
        } catch (SQLException ex) {
            LOG.error("transaction failed and was rolled back", ex);
            try {
                target.rollback();
            } catch (SQLException rollbackFailure) {
                LOG.error("the rollback itself failed", rollbackFailure);
            }
            return false;
        } finally {
            running = false;
            try {
                target.setAutoCommit(previousAutoCommit);
            } catch (SQLException ex) {
                LOG.error("could not restore auto-commit after the transaction", ex);
            }
        }
    }

    /**
     * Closes the connection. Tests call this between cases; the application does not
     * need to, since the file is consistent after every committed statement.
     */
    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                LOG.error("could not close the database", ex);
            }
            connection = null;
        }
    }
}
