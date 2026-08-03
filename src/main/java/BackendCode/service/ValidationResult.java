package BackendCode.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * What was wrong with a filled-in record, field by field.
 * <p>
 * A {@link ServiceResult} carries one sentence, which is right for "this car is already
 * booked" and useless for a form: it cannot say <em>which</em> of five boxes to put a
 * red label beside. That is why validation stayed in the screens when everything else
 * moved to services. Keying the failures by field lets the rule live in one place and
 * still tell each screen what to highlight.
 *
 * @author @Barath-Grama
 */
public final class ValidationResult {

    /** Field keys, so a screen and the validator cannot disagree about a spelling. */
    public static final String CNIC = "cnic";
    public static final String NAME = "name";
    public static final String CONTACT = "contact";
    public static final String MAKER = "maker";
    public static final String REG_NO = "regNo";
    public static final String OWNER_ID = "ownerId";
    public static final String RENT_PER_HOUR = "rentPerHour";
    public static final String STARTS_AT = "startsAt";
    public static final String ENDS_AT = "endsAt";

    private final Map<String, String> problems;

    private ValidationResult(Map<String, String> problems) {
        this.problems = problems;
    }

    /**
     * Collects field problems one at a time.
     */
    public static final class Builder {

        private final Map<String, String> problems = new LinkedHashMap<>();

        /**
         * Records a problem, keeping the first one found for a field: the earliest
         * check is the most specific, and "enter a CNIC" is more use than the
         * "invalid CNIC" that would follow it. A null message means the field passed,
         * so a check can report its outcome either way without the caller branching.
         */
        public Builder problem(String field, String message) {
            if (message != null && !problems.containsKey(field)) {
                problems.put(field, message);
            }
            return this;
        }

        /**
         * Records a problem only when the condition holds.
         */
        public Builder problemIf(boolean broken, String field, String message) {
            if (broken) {
                problem(field, message);
            }
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(Collections.unmodifiableMap(problems));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isValid() {
        return problems.isEmpty();
    }

    /**
     * @param field one of the constants above
     * @return what is wrong with that field, or an empty string if nothing is
     */
    public String messageFor(String field) {
        String message = problems.get(field);
        return message == null ? "" : message;
    }

    /**
     * @return the fields that have a problem, in the order they were checked
     */
    public Set<String> failedFields() {
        return problems.keySet();
    }

    /**
     * @return every problem on one line, for a caller with nowhere to put them
     *         individually
     */
    public String summary() {
        StringBuilder text = new StringBuilder();
        for (String message : problems.values()) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(message);
        }
        return text.toString();
    }

    /**
     * @return this as a service result, so a caller that only reports one message can
     *         treat validation like any other refusal
     */
    public ServiceResult asServiceResult() {
        return isValid() ? ServiceResult.ok("") : ServiceResult.failed(summary());
    }
}
