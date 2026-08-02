package BackendCode;

import java.io.File;

/**
 * Gives each test an empty database to work against.
 * <p>
 * Surefire runs the suite in {@code target/test-workdir} so this never touches the
 * repository's own data, but tests share that one directory, so each has to clear it
 * for itself. The connection is closed first: SQLite holds the file open, and on
 * Windows a delete of an open file silently does nothing, which would leave the
 * previous test's rows in place.
 * <p>
 * The legacy {@code .ser} files are removed too. They are absent in the scratch
 * directory, but clearing them keeps {@link SerImporter} from firing if a test ever
 * writes one.
 */
final class DataFiles {

    private static final String[] LEGACY = {
        "Customer.ser", "CarOwner.ser", "Car.ser", "Booking.ser", "credentials.properties"
    };

    private DataFiles() {
    }

    static void reset() {
        Database.close();
        new File(Database.fileName()).delete();
        for (String name : LEGACY) {
            new File(name).delete();
        }
    }
}
