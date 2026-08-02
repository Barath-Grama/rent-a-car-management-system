package BackendCode.service;

import BackendCode.AppUser;
import BackendCode.Database;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Signing in, and what each role is allowed to do.
 */
class UserServiceTest {

    @BeforeEach
    void freshDatabase() {
        UserService.signOut();
        Database.close();
        new File(Database.fileName()).delete();
        for (String legacy : new String[]{"Customer.ser", "CarOwner.ser", "Car.ser", "Booking.ser"}) {
            new File(legacy).delete();
        }
    }

    @Test
    @DisplayName("the documented default account is seeded and signs in")
    void defaultAccountWorks() {
        AppUser user = UserService.signIn("admin", "123".toCharArray());

        assertNotNull(user, "admin/123 should still work on a fresh database");
        assertEquals(AppUser.Role.ADMIN, user.getRole());
    }

    @Test
    @DisplayName("the username is matched without regard to case or surrounding spaces")
    void usernameIsLenient() {
        assertNotNull(UserService.signIn("  Admin ", "123".toCharArray()));
    }

    @Test
    @DisplayName("a wrong password or unknown user is refused")
    void wrongDetailsRefused() {
        assertNull(UserService.signIn("admin", "wrong".toCharArray()));
        assertNull(UserService.signIn("nobody", "123".toCharArray()));
        assertNull(UserService.signIn("admin", "".toCharArray()));
    }

    @Test
    @DisplayName("the stored value is a salted hash, not the password")
    void passwordIsNotStored() throws Exception {
        UserService.signIn("admin", "123".toCharArray());

        try (java.sql.Statement statement = Database.connection().createStatement();
             java.sql.ResultSet rows = statement.executeQuery(
                     "SELECT password_hash FROM app_user WHERE username = 'admin'")) {
            assertTrue(rows.next());
            String stored = rows.getString(1);
            assertFalse(stored.contains("123"), "the password itself must not be in the database");
            assertTrue(stored.startsWith("$2"), "should be a BCrypt hash");
        }
    }

    @Test
    @DisplayName("two accounts with the same password store different hashes")
    void hashesAreSalted() throws Exception {
        UserService.signIn("admin", "123".toCharArray());          // seeds the default
        UserService.addUser("second", "123".toCharArray(), AppUser.Role.STAFF);

        try (java.sql.Statement statement = Database.connection().createStatement();
             java.sql.ResultSet rows = statement.executeQuery(
                     "SELECT password_hash FROM app_user ORDER BY id")) {
            rows.next();
            String first = rows.getString(1);
            rows.next();
            String second = rows.getString(1);
            // Without a per-account salt these would be identical, and one cracked
            // password would give away every account that shared it.
            assertFalse(first.equals(second), "the same password should not hash to the same value");
        }
    }

    @Test
    @DisplayName("staff may sign in but may not manage accounts")
    void staffIsRestricted() {
        UserService.signIn("admin", "123".toCharArray());
        assertTrue(UserService.addUser("desk", "desk123".toCharArray(), AppUser.Role.STAFF).isSuccess());

        AppUser staff = UserService.signIn("desk", "desk123".toCharArray());

        assertNotNull(staff);
        assertEquals(AppUser.Role.STAFF, staff.getRole());
        assertFalse(staff.canManageAccounts());
        assertFalse(UserService.currentCanManageAccounts());
    }

    @Test
    @DisplayName("an administrator may manage accounts")
    void adminIsAllowed() {
        UserService.signIn("admin", "123".toCharArray());

        assertTrue(UserService.currentCanManageAccounts());
    }

    @Test
    @DisplayName("signing out drops the permissions with the session")
    void signOutClearsPermissions() {
        UserService.signIn("admin", "123".toCharArray());
        assertTrue(UserService.currentCanManageAccounts());

        UserService.signOut();

        assertNull(UserService.current());
        assertFalse(UserService.currentCanManageAccounts(),
                "nobody signed in must not inherit the last user's rights");
    }

    @Test
    @DisplayName("a duplicate username is refused")
    void duplicateUsernameRefused() {
        UserService.signIn("admin", "123".toCharArray());

        ServiceResult result = UserService.addUser("admin", "other".toCharArray(), AppUser.Role.STAFF);

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("a username or password that is too short is refused")
    void weakInputRefused() {
        UserService.signIn("admin", "123".toCharArray());

        assertFalse(UserService.addUser("  ", "goodpass".toCharArray(), AppUser.Role.STAFF).isSuccess());
        assertFalse(UserService.addUser("someone", "x".toCharArray(), AppUser.Role.STAFF).isSuccess());
    }
}
