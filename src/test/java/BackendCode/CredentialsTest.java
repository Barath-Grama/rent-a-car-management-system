package BackendCode;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Login details used to be string literals in Login.java, which put the password in
 * the source, in git history and in the compiled class file.
 */
class CredentialsTest {

    @BeforeEach
    void freshFiles() {
        DataFiles.reset();
    }

    @Test
    @DisplayName("the documented default still logs in")
    void defaultCredentialsAccepted() {
        assertTrue(Credentials.areValid("admin", "123".toCharArray()));
    }

    @Test
    @DisplayName("the username is matched case-insensitively and trimmed")
    void usernameIsLenient() {
        assertTrue(Credentials.areValid("  Admin  ", "123".toCharArray()));
    }

    @Test
    @DisplayName("a wrong password or user is refused")
    void wrongCredentialsRefused() {
        assertFalse(Credentials.areValid("admin", "1234".toCharArray()));
        assertFalse(Credentials.areValid("admin", "".toCharArray()));
        assertFalse(Credentials.areValid("root", "123".toCharArray()));
    }

    @Test
    @DisplayName("the credentials file is created on first use and holds only a hash")
    void fileIsCreatedWithoutStoringThePassword() throws Exception {
        Credentials.areValid("admin", "123".toCharArray());

        File file = new File("credentials.properties");
        assertTrue(file.exists(), "first use should write the defaults out");

        String body = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(body.contains("password.sha256"), "the hash should be what is stored");
        assertFalse(body.contains("=123"), "the password itself must not be in the file");
    }
}
