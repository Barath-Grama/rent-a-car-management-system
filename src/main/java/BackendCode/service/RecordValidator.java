package BackendCode.service;

import BackendCode.Car;
import BackendCode.Person;

/**
 * Checks what was typed into a form, one field at a time.
 * <p>
 * These rules were spelled out separately in each Add and Update dialog, in slightly
 * different words and occasionally in a different order. They live here now; the
 * screens read the result and put each message beside the box it belongs to.
 *
 * @author @Barath-Grama
 */
public final class RecordValidator {

    private RecordValidator() {
    }

    /**
     * @return what is wrong with a customer or car owner's details, field by field
     */
    public static ValidationResult validatePerson(String cnic, String name, String contact) {
        ValidationResult.Builder problems = ValidationResult.builder();

        if (isBlank(cnic)) {
            problems.problem(ValidationResult.CNIC, "Enter CNIC !");
        } else if (!Person.isCNICValid(cnic.trim())) {
            problems.problem(ValidationResult.CNIC, "Invalid CNIC ! (13 digits, no dashes)");
        }

        if (isBlank(name)) {
            problems.problem(ValidationResult.NAME, "Enter Name !");
        } else if (!Person.isNameValid(name.trim())) {
            problems.problem(ValidationResult.NAME, "Invalid Name ! (letters and spaces only)");
        }

        if (isBlank(contact)) {
            problems.problem(ValidationResult.CONTACT, "Enter Contact Number !");
        } else if (!Person.isContactNoValid(contact.trim())) {
            problems.problem(ValidationResult.CONTACT, "Invalid Contact Number ! (11 digits starting 03)");
        }

        return problems.build();
    }

    /**
     * @return what is wrong with a car's details, field by field
     */
    public static ValidationResult validateCar(String maker, String name, String regNo,
                                               String ownerId, String rentPerHour) {
        ValidationResult.Builder problems = ValidationResult.builder();

        if (isBlank(maker)) {
            problems.problem(ValidationResult.MAKER, "Enter Maker's Name !");
        } else if (!Car.isNameValid(maker.trim())) {
            problems.problem(ValidationResult.MAKER, "Invalid Maker's Name !");
        }

        if (isBlank(name)) {
            problems.problem(ValidationResult.NAME, "Enter Car Name !");
        } else if (!Car.isNameValid(name.trim())) {
            problems.problem(ValidationResult.NAME, "Invalid Car Name !");
        }

        if (isBlank(regNo)) {
            problems.problem(ValidationResult.REG_NO, "Enter Reg No !");
        } else if (!Car.isRegNoValid(regNo.trim())) {
            problems.problem(ValidationResult.REG_NO, "Invalid Reg no ! (letters, dash, digits)");
        }

        problems.problem(ValidationResult.OWNER_ID, positiveNumber(ownerId, "Owner ID"));
        problems.problem(ValidationResult.RENT_PER_HOUR, positiveNumber(rentPerHour, "Rent"));

        return problems.build();
    }

    /**
     * @return the message for a field that must hold a positive whole number, or null
     *         when it does
     */
    private static String positiveNumber(String value, String label) {
        if (isBlank(value)) {
            return "Enter " + label + " !";
        }
        if (!Person.isIDvalid(value.trim())) {
//            isIDvalid already rejects blanks, non-digits, zero, negatives and
//            anything past what an int can hold, without throwing on any of them
            return "Invalid " + label + " ! (a whole number above zero)";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
