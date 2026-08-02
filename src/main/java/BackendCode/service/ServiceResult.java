package BackendCode.service;

/**
 * What a service call did, and what to tell the user if it did not work.
 * <p>
 * The screens used to decide this for themselves, which is why the same rule was
 * spelled out differently on different windows and why a failed write could still be
 * followed by a success dialog. A service returns one of these and the screen only
 * has to display it.
 *
 * @author @AbdullahShahid01
 */
public final class ServiceResult {

    private final boolean success;
    private final String message;

    private ServiceResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * @param message what to show the user on success
     */
    public static ServiceResult ok(String message) {
        return new ServiceResult(true, message);
    }

    /**
     * @param message why it did not go ahead, phrased for the user
     */
    public static ServiceResult failed(String message) {
        return new ServiceResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
