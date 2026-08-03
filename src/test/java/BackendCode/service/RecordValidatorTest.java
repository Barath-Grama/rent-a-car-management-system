package BackendCode.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The form rules, and which box each complaint belongs to.
 * <p>
 * The point of validating by field rather than returning one sentence: a screen has a
 * red label beside every input and needs to know which one to fill in.
 */
class RecordValidatorTest {

    @Nested
    @DisplayName("people")
    class People {

        @Test
        @DisplayName("a complete, well-formed person passes")
        void validPersonPasses() {
            ValidationResult result =
                    RecordValidator.validatePerson("1234567890123", "Abdul Shahid", "03001234567");

            assertTrue(result.isValid());
            assertTrue(result.failedFields().isEmpty());
            assertEquals("", result.messageFor(ValidationResult.CNIC));
        }

        @Test
        @DisplayName("each bad field is reported against itself, not lumped together")
        void problemsAreKeyedByField() {
            ValidationResult result = RecordValidator.validatePerson("123", "Ali9", "12345");

            assertFalse(result.isValid());
            assertEquals(3, result.failedFields().size());
            assertTrue(result.messageFor(ValidationResult.CNIC).contains("Invalid CNIC"));
            assertTrue(result.messageFor(ValidationResult.NAME).contains("Invalid Name"));
            assertTrue(result.messageFor(ValidationResult.CONTACT).contains("Invalid Contact"));
        }

        @Test
        @DisplayName("an empty field says to fill it in rather than that it is invalid")
        void emptyIsDistinctFromInvalid() {
            ValidationResult result = RecordValidator.validatePerson("", "", "");

            assertEquals("Enter CNIC !", result.messageFor(ValidationResult.CNIC));
            assertEquals("Enter Name !", result.messageFor(ValidationResult.NAME));
            assertEquals("Enter Contact Number !", result.messageFor(ValidationResult.CONTACT));
        }

        @Test
        @DisplayName("only the fields that are wrong are reported")
        void goodFieldsStaySilent() {
            ValidationResult result =
                    RecordValidator.validatePerson("1234567890123", "Ali", "not-a-number");

            assertEquals(1, result.failedFields().size());
            assertEquals("", result.messageFor(ValidationResult.CNIC));
            assertEquals("", result.messageFor(ValidationResult.NAME));
            assertFalse(result.messageFor(ValidationResult.CONTACT).isEmpty());
        }

        @Test
        @DisplayName("whitespace around a good value does not make it bad")
        void valuesAreTrimmedBeforeChecking() {
            assertTrue(RecordValidator.validatePerson(
                    "  1234567890123  ", " Abdul Shahid ", " 03001234567 ").isValid());
        }

        @Test
        @DisplayName("null is treated as empty rather than throwing")
        void nullIsHandled() {
            ValidationResult result = RecordValidator.validatePerson(null, null, null);

            assertFalse(result.isValid());
            assertEquals(3, result.failedFields().size());
        }
    }

    @Nested
    @DisplayName("cars")
    class Cars {

        @Test
        @DisplayName("a complete, well-formed car passes")
        void validCarPasses() {
            assertTrue(RecordValidator.validateCar("Toyota", "Corolla", "AAA-111", "1", "100")
                    .isValid());
        }

        @Test
        @DisplayName("each bad field is reported against itself")
        void problemsAreKeyedByField() {
            ValidationResult result =
                    RecordValidator.validateCar("", "Civic@", "-123", "0", "abc");

            assertEquals(5, result.failedFields().size());
            assertEquals("Enter Maker's Name !", result.messageFor(ValidationResult.MAKER));
            assertTrue(result.messageFor(ValidationResult.NAME).contains("Invalid"));
            assertTrue(result.messageFor(ValidationResult.REG_NO).contains("Invalid"));
            assertTrue(result.messageFor(ValidationResult.OWNER_ID).contains("Invalid Owner ID"));
            assertTrue(result.messageFor(ValidationResult.RENT_PER_HOUR).contains("Invalid Rent"));
        }

        @Test
        @DisplayName("a rent or owner id that would overflow an int is rejected, not thrown on")
        void overflowIsRejected() {
            ValidationResult result =
                    RecordValidator.validateCar("Toyota", "Corolla", "AAA-111", "99999999999", "99999999999");

            assertFalse(result.isValid());
            assertFalse(result.messageFor(ValidationResult.OWNER_ID).isEmpty());
            assertFalse(result.messageFor(ValidationResult.RENT_PER_HOUR).isEmpty());
        }

        @Test
        @DisplayName("a car name may contain digits where a person's name may not")
        void carNamesAllowDigits() {
            assertTrue(RecordValidator.validateCar("Audi", "A4", "AAA-111", "1", "100").isValid());
            assertFalse(RecordValidator.validatePerson("1234567890123", "A4", "03001234567").isValid());
        }
    }

    @Test
    @DisplayName("a summary gathers every problem for a caller with nowhere to put them")
    void summaryGathersEverything() {
        ValidationResult result = RecordValidator.validatePerson("", "Ali9", "");

        String summary = result.summary();
        assertTrue(summary.contains("Enter CNIC !"));
        assertTrue(summary.contains("Invalid Name"));
        assertTrue(summary.contains("Enter Contact Number !"));
        assertEquals(3, summary.split("\n").length);
    }

    @Test
    @DisplayName("a valid result converts to a successful service result and vice versa")
    void convertsToServiceResult() {
        assertTrue(RecordValidator.validatePerson("1234567890123", "Ali", "03001234567")
                .asServiceResult().isSuccess());
        assertFalse(RecordValidator.validatePerson("", "", "").asServiceResult().isSuccess());
    }
}
