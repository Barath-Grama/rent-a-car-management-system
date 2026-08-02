package BackendCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the login details entered on the Login screen.
 * <p>
 * The username and password used to sit in Login.java as string literals, which put
 * the real password in the source, in git history, and in plain text inside the
 * compiled .class file. They now live in an external properties file that is created
 * with the original admin / 123 defaults the first time the program runs, so the
 * behaviour is unchanged but the credential is no longer part of the code. Only a
 * SHA-256 hash of the password is stored, so reading the file does not reveal it.
 * <p>
 * This is not strong authentication -- there is no per-user salt and no work factor,
 * and anyone who can write the file can change the password. It is the level of
 * protection a single-workstation desktop program of this kind warrants; a real
 * deployment would want a proper user store.
 *
 * @author @AbdullahShahid01
 */
public class Credentials {

    private static final Logger LOG = LoggerFactory.getLogger(Credentials.class);

    private static final String FILE_NAME = "credentials.properties";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_HASH_KEY = "password.sha256";

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "123";

    /**
     * @param username what was typed in the username box
     * @param password what was typed in the password box
     * @return true if both match the stored credentials
     */
    public static boolean areValid(String username, char[] password) {
        Properties stored = load();
        String expectedUser = stored.getProperty(USERNAME_KEY, DEFAULT_USERNAME);
        String expectedHash = stored.getProperty(PASSWORD_HASH_KEY, hash(DEFAULT_PASSWORD));

        String actualHash = hash(new String(password));
//        a failed hash must never be treated as a match
        if (actualHash == null || expectedHash == null) {
            return false;
        }
        return username.trim().equalsIgnoreCase(expectedUser) && actualHash.equals(expectedHash);
    }

    /**
     * Reads the credentials file, creating it with the defaults when it is missing.
     */
    private static Properties load() {
        Properties properties = new Properties();
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            properties.setProperty(USERNAME_KEY, DEFAULT_USERNAME);
            properties.setProperty(PASSWORD_HASH_KEY, hash(DEFAULT_PASSWORD));
            save(properties, file);
            return properties;
        }
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            properties.load(input);
        } catch (IOException ex) {
            LOG.error("could not read the credentials file", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    LOG.error("could not read the credentials file", ex);
                }
            }
        }
        return properties;
    }

    private static void save(Properties properties, File file) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            properties.store(output, "Rent-A-Car login details. Delete this file to reset to admin / 123.");
        } catch (IOException ex) {
            LOG.error("could not write the credentials file", ex);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    LOG.error("could not write the credentials file", ex);
                }
            }
        }
    }

    /**
     * @return the SHA-256 of the given text as hex, or null if hashing failed
     */
    private static String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                String part = Integer.toHexString(bytes[i] & 0xff);
                if (part.length() == 1) {
                    hex.append('0');
                }
                hex.append(part);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            LOG.error("could not hash the password", ex);
            return null;
        } catch (IOException ex) {
            LOG.error("could not hash the password", ex);
            return null;
        }
    }
}
