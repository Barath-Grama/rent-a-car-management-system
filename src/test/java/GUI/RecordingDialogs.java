package GUI;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the real dialogs during a test: records what the screen said and
 * answers instantly.
 * <p>
 * Without this a UI test deadlocks on the first modal dialog, because the screen is
 * waiting for a click that is never coming.
 */
final class RecordingDialogs extends Dialogs {

    private final List<String> messages = new ArrayList<>();
    private boolean confirmAnswer = true;
    private Object chooseAnswer;

    /** What the next confirmation should answer. */
    RecordingDialogs answering(boolean agree) {
        this.confirmAnswer = agree;
        return this;
    }

    /** What the next picker should return. */
    RecordingDialogs choosing(Object value) {
        this.chooseAnswer = value;
        return this;
    }

    /** @return every message shown, in order */
    List<String> messages() {
        return messages;
    }

    /** @return the last thing the screen said, or an empty string if it said nothing */
    String last() {
        return messages.isEmpty() ? "" : messages.get(messages.size() - 1);
    }

    /** @return true if anything shown contained the given text */
    boolean said(String fragment) {
        for (String message : messages) {
            if (message.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void info(Component parent, String message) {
        messages.add(message);
    }

    @Override
    public void error(Component parent, String message) {
        messages.add(message);
    }

    @Override
    public void notPermitted(Component parent, String message) {
        messages.add(message);
    }

    @Override
    public boolean confirm(Component parent, String message, String title) {
        messages.add(message);
        return confirmAnswer;
    }

    @Override
    public Object choose(Component parent, String message, String title, Object[] options) {
        messages.add(message);
        return chooseAnswer;
    }
}
