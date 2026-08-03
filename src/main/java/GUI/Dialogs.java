package GUI;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * Everything the screens say to the user, behind one seam.
 * <p>
 * The screens called {@link JOptionPane} directly, which is why none of them could be
 * tested: a modal dialog blocks the event thread until somebody clicks it, and in a
 * test there is nobody. Going through here lets a test install an implementation that
 * records what was asked and answers instantly, so the real listener runs to
 * completion and can be checked.
 * <p>
 * It also means every screen asks the same way, instead of one passing a title and
 * another not, and one comparing the confirm result against {@code 0} while the
 * constant is called {@code OK_OPTION}.
 *
 * @author @Barath-Grama
 */
public class Dialogs {

    private static Dialogs current = new Dialogs();

    /**
     * @return whoever is answering the user at the moment
     */
    public static Dialogs get() {
        return current;
    }

    /**
     * Swaps in a different implementation. Tests use this; the program never does.
     *
     * @param replacement the one to use, or null to go back to real dialogs
     */
    public static void set(Dialogs replacement) {
        current = replacement == null ? new Dialogs() : replacement;
    }

    /**
     * Tells the user something went as intended.
     */
    public void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message);
    }

    /**
     * Tells the user something did not work.
     */
    public void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Tells the user they are not allowed to do that.
     */
    public void notPermitted(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Not permitted", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Asks before doing something that cannot be undone.
     *
     * @return true if the user agreed
     */
    public boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    /**
     * Asks the user to pick one of a list.
     *
     * @return what they picked, or null if they cancelled
     */
    public Object choose(Component parent, String message, String title, Object[] options) {
        return JOptionPane.showInputDialog(parent, message, title,
                JOptionPane.PLAIN_MESSAGE, null, options, null);
    }
}
