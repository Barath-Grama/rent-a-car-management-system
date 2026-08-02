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
     * Closes the connection. Tests call this between cases; the application does not
     * need to, since the file is consistent after every committed statement.
     */
    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                System.out.println(ex);
            }
            connection = null;
        }
    }
}
