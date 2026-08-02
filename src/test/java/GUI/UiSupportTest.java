package GUI;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two pieces of UI plumbing that can be checked without opening a window.
 * <p>
 * This test lives in the GUI package because {@link Images} is package-private. It
 * builds components but never realises a peer, so it runs headless.
 */
class UiSupportTest {

    @Test
    @DisplayName("AbsoluteLayout places components at the pixel positions it is given")
    void absoluteLayoutPositionsComponents() {
        // Every screen in this application is laid out with this class. It used to come
        // from a NetBeans IDE-global library that the repository never defined, so the
        // project could not be built outside a configured NetBeans. It now lives in the
        // source tree, which means its behaviour is ours to guarantee.
        JPanel panel = new JPanel();
        panel.setLayout(new AbsoluteLayout());

        JButton sized = new JButton("sized");
        JButton natural = new JButton("natural");
        natural.setPreferredSize(new Dimension(77, 33));

        panel.add(sized, new AbsoluteConstraints(70, 220, 200, 99));
        panel.add(natural, new AbsoluteConstraints(10, 5));
        panel.setSize(1366, 730);
        panel.doLayout();

        assertEquals(new Rectangle(70, 220, 200, 99), sized.getBounds(),
                "an explicit width and height should be used as given");
        assertEquals(new Rectangle(10, 5, 77, 33), natural.getBounds(),
                "a constraint without a size should fall back to the preferred size");
    }

    @Test
    @DisplayName("AbsoluteLayout asks for a size that fits its furthest component")
    void absoluteLayoutPreferredSizeSpansContent() {
        JPanel panel = new JPanel();
        AbsoluteLayout layout = new AbsoluteLayout();
        panel.setLayout(layout);
        panel.add(new JButton("a"), new AbsoluteConstraints(70, 220, 200, 99));

        Dimension preferred = layout.preferredLayoutSize(panel);

        assertEquals(270, preferred.width);
        assertEquals(319, preferred.height);
    }

    @Test
    @DisplayName("AbsoluteLayout refuses a constraint it cannot use")
    void absoluteLayoutRejectsWrongConstraint() {
        AbsoluteLayout layout = new AbsoluteLayout();
        assertThrows(IllegalArgumentException.class,
                () -> layout.addLayoutComponent(new JButton(), "top-left"));
    }

    @Test
    @DisplayName("every image resolves from the classpath")
    void imagesLoadFromClasspath() {
        // These were loaded relative to the working directory, so the program only
        // showed them when started from the project folder and showed nothing once
        // packaged as a jar. Names are case-sensitive inside a jar.
        for (String name : new String[]{"LoginImage.JPG", "MainMenuImage.jpeg", "WelcomeImage.jpg"}) {
            assertNotNull(Images.class.getResource("/images/" + name),
                    name + " should be on the classpath");
            assertTrue(Images.load(name).getIconWidth() > 0,
                    name + " should decode to a real image");
        }
    }

    @Test
    @DisplayName("a missing image degrades to a blank icon rather than taking the window down")
    void missingImageDoesNotThrow() {
        assertEquals(-1, Images.load("NoSuchImage.png").getIconWidth());
    }
}
