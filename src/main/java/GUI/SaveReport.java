package GUI;

import javax.swing.JOptionPane;

/**
 * Turns the result of a BackendCode save into something the user actually sees.
 * <p>
 * Add(), Update() and Remove() used to return void and print any I/O failure to
 * standard output, which nobody running the program ever looks at. The screen then
 * went on to announce "Record Successfully Added !" whether or not anything had been
 * written. They now report whether the write reached the file, and every caller puts
 * the answer through here so a failure is shown instead of swallowed.
 *
 * @author @AbdullahShahid01
 */
class SaveReport {

    /**
     * @param saved what Add() / Update() / Remove() returned
     * @return the same value, after telling the user when it is false
     */
    static boolean check(boolean saved) {
        if (!saved) {
            JOptionPane.showMessageDialog(null,
                    "The change could not be written to disk, so nothing was saved."
                    + "\nCheck that the program folder is writable, then try again.",
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
        return saved;
    }
}
