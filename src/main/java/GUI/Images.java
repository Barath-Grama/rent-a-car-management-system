package GUI;

import java.net.URL;
import javax.swing.ImageIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the application's images from the classpath.
 * <p>
 * They used to be loaded as {@code new ImageIcon("LoginImage.JPG")}, which resolves
 * against the current working directory. That meant the program only showed its
 * images when it happened to be started from the project folder, and showed nothing
 * at all once packaged into a jar and launched from anywhere else. Reading them from
 * the classpath instead makes them travel with the jar.
 * <p>
 * Resource names are case-sensitive inside a jar even on Windows, so the names here
 * must match the files in {@code src/main/resources/images} exactly.
 *
 * @author @Barath-Grama
 */
final class Images {

    private static final Logger LOG = LoggerFactory.getLogger(Images.class);

    private Images() {
//        utility holder, never instantiated
    }

    /**
     * @param fileName file name under {@code src/main/resources/images}
     * @return the image, or an empty icon if it is missing, so that a packaging
     *         mistake leaves a blank label rather than taking the window down
     */
    static ImageIcon load(String fileName) {
        URL url = Images.class.getResource("/images/" + fileName);
        if (url == null) {
            LOG.warn("missing image resource: /images/" + fileName);
            return new ImageIcon();
        }
        return new ImageIcon(url);
    }
}
