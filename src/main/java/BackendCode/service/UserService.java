package BackendCode.service;

import BackendCode.AppUser;
import BackendCode.Database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Signing in, and who is signed in.
 * <p>
 * Replaces the properties file that held a SHA-256 of the password. A plain digest is
 * the wrong tool for a password: it is built to be fast, which is exactly what an
 * attacker guessing millions of candidates wants. BCrypt is deliberately slow and
 * salts each hash, so two people choosing the same password still store different
 * values and a stolen file cannot be attacked with a precomputed table.
 * <p>
 * On a database with no users at all this seeds the {@code admin} / {@code 123}
 * account the project has always documented, so a fresh checkout still starts.
 *
 * @author @Barath-Grama
 */
public final class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "123";
    /** Cost 10 is roughly 100ms per check here: unnoticeable to a person, expensive in bulk. */
    private static final int COST = 10;

    /**
     * Verified against when the username is not on record, so that path costs the same
     * as a real check. Computed once at class load rather than per attempt.
     */
    private static final String DECOY_HASH = BCrypt.hashpw("decoy", BCrypt.gensalt(COST));

    private static AppUser signedIn;

    private UserService() {
    }

    /**
     * Checks a username and password, and remembers the user on success.
     *
     * @param username what was typed in the username box
     * @param password what was typed in the password box
     * @return the signed-in user, or null if the details were wrong
     */
    public static AppUser signIn(String username, char[] password) {
        seedDefaultUserIfEmpty();
        String sql = "SELECT id, username, password_hash, role FROM app_user WHERE username = ? COLLATE NOCASE";
        try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
            statement.setString(1, username == null ? "" : username.trim());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
//                    Check against a fixed decoy hash so an unknown username costs the
//                    same one BCrypt verification as a known one, and cannot be picked
//                    out by how quickly it is rejected. Generating a salt and hashing
//                    here instead would be two operations, making an unknown username
//                    measurably *slower* -- the same leak, inverted.
                    BCrypt.checkpw(new String(password), DECOY_HASH);
                    return null;
                }
                if (!BCrypt.checkpw(new String(password), rows.getString("password_hash"))) {
                    return null;
                }
                signedIn = new AppUser(rows.getInt("id"), rows.getString("username"),
                        AppUser.Role.valueOf(rows.getString("role")));
                LOG.info("signed in as {}", signedIn);
                return signedIn;
            }
        } catch (SQLException ex) {
            LOG.error("could not check the sign-in details", ex);
            return null;
        }
    }

    /**
     * @return who is signed in, or null once they have signed out
     */
    public static AppUser current() {
        return signedIn;
    }

    /**
     * @return true if nobody is signed in or they are not allowed to manage accounts
     */
    public static boolean currentCanManageAccounts() {
        return signedIn != null && signedIn.canManageAccounts();
    }

    public static void signOut() {
        signedIn = null;
    }

    /**
     * Adds a user. Only meaningful to an administrator.
     *
     * @return what happened, with a message for the user
     */
    public static ServiceResult addUser(String username, char[] password, AppUser.Role role) {
        if (password == null || password.length < 3) {
            return ServiceResult.failed("The password must be at least 3 characters.");
        }
//        Past this point every exit wipes the password, including the rejected ones.
//        The username check sits inside the try so an invalid name cannot leave the
//        characters sitting in memory.
        String sql = "INSERT INTO app_user (username, password_hash, role) VALUES (?, ?, ?)";
        try {
            if (username == null || username.trim().isEmpty()) {
                return ServiceResult.failed("Enter a username.");
            }
            try (PreparedStatement statement = Database.connection().prepareStatement(sql)) {
                statement.setString(1, username.trim());
                statement.setString(2, BCrypt.hashpw(new String(password), BCrypt.gensalt(COST)));
                statement.setString(3, role.name());
                statement.executeUpdate();
                return ServiceResult.ok("User " + username.trim() + " added.");
            }
        } catch (SQLException ex) {
            LOG.error("could not add the user", ex);
            return ServiceResult.failed("That username is already taken.");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Creates the documented default account the first time the program runs against
     * a database that has no users, so a fresh checkout is not locked out of itself.
     */
    private static void seedDefaultUserIfEmpty() {
        try (PreparedStatement count = Database.connection()
                .prepareStatement("SELECT COUNT(*) FROM app_user");
             ResultSet rows = count.executeQuery()) {
            if (rows.next() && rows.getInt(1) > 0) {
                return;
            }
        } catch (SQLException ex) {
            LOG.error("could not check for existing users", ex);
            return;
        }
        try (PreparedStatement insert = Database.connection().prepareStatement(
                "INSERT INTO app_user (username, password_hash, role) VALUES (?, ?, 'ADMIN')")) {
            insert.setString(1, DEFAULT_USERNAME);
            insert.setString(2, BCrypt.hashpw(DEFAULT_PASSWORD, BCrypt.gensalt(COST)));
            insert.executeUpdate();
            LOG.info("created the default {} account; change its password", DEFAULT_USERNAME);
        } catch (SQLException ex) {
            LOG.error("could not create the default account", ex);
        }
    }
}
