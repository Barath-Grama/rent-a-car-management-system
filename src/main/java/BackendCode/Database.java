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
 * @author @AbdullahShahid01
 */
public final class Database {

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);

    private static final String DEFAULT_FILE = "rentacar.db";

    private static Connection connection;
    private static String fileName = DEFAULT_FILE;

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
        SerImporter.importIfPresent(connection);
        return connection;
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
            LOG.error("transaction failed and was rolled back", ex);
            return false;
        }
        boolean previousAutoCommit = true;
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
                LOG.error("transaction failed and was rolled back", rollbackFailure);
            }
            return false;
        } finally {
            try {
                target.setAutoCommit(previousAutoCommit);
            } catch (SQLException ex) {
                LOG.error("transaction failed and was rolled back", ex);
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
