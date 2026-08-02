package BackendCode;

import java.io.File;

/**
 * Wipes the flat files the model persists to, so each test starts from nothing.
 * <p>
 * The model resolves every file against the current working directory. Surefire is
 * configured to run the suite in {@code target/test-workdir} so this never touches the
 * repository's own data, but tests still share that one directory, so each has to
 * clear it for itself.
 * <p>
 * Note the {@code .id} counter files: they are deliberately <em>not</em> removed by
 * {@code Remove()}, which is the whole point of the never-reuse-an-ID fix. A test that
 * forgets them here would see IDs carry over from the previous test and fail for a
 * reason that has nothing to do with what it is checking.
 */
final class DataFiles {

    private static final String[] ALL = {
        "Customer.ser", "CarOwner.ser", "Car.ser", "Booking.ser",
        "Customer.id", "CarOwner.id", "Car.id", "Booking.id",
        "credentials.properties"
    };

    private DataFiles() {
    }

    static void reset() {
        for (String name : ALL) {
            new File(name).delete();
        }
    }
}
