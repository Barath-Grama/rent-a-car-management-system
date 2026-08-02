package BackendCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * The field validators. These are pure functions over their input, so they need no
 * files and no fixture.
 */
class ValidationTest {

    @Nested
    @DisplayName("Person.isIDvalid")
    class IdValidation {

        @ParameterizedTest(name = "\"{0}\" is not a valid id")
        @ValueSource(strings = {"", " ", "0", "-5", "12a", "1.5", "99999999999", "2147483648"})
        void rejects(String id) {
            // Empty input and anything past Integer.MAX_VALUE used to reach
            // Integer.parseInt unguarded and throw NumberFormatException, which
            // escaped onto the event thread from four different screens.
            assertDoesNotThrow(() -> Person.isIDvalid(id));
            assertFalse(Person.isIDvalid(id));
        }

        @ParameterizedTest(name = "\"{0}\" is a valid id")
        @ValueSource(strings = {"1", "7", "007", "2147483647"})
        void accepts(String id) {
            assertTrue(Person.isIDvalid(id));
        }

        @Test
        @DisplayName("anything it accepts can then be parsed by the caller")
        void acceptedValuesParse() {
            // Every caller does Integer.parseInt right after this returns true.
            assertEquals(2147483647, Integer.parseInt("2147483647"));
            assertTrue(Person.isIDvalid("2147483647"));
        }
    }

    @Nested
    @DisplayName("name validation")
    class NameValidation {

        @Test
        @DisplayName("an empty name is rejected by both validators")
        void emptyRejected() {
            assertFalse(Person.isNameValid(""));
            assertFalse(Car.isNameValid(""));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Abdul Shahid", "Ali", "Mary Jane Watson"})
        void personNamesAccepted(String name) {
            assertTrue(Person.isNameValid(name));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Ali1", "Ali@", "Ali-Khan"})
        void personNamesRejected(String name) {
            assertFalse(Person.isNameValid(name));
        }

        @Test
        @DisplayName("car names may contain digits, person names may not")
        void carNamesAllowDigits() {
            assertTrue(Car.isNameValid("Corolla 2020"));
            assertFalse(Person.isNameValid("Corolla 2020"));
        }
    }

    @Nested
    @DisplayName("Car.isRegNoValid")
    class RegNoValidation {

        @ParameterizedTest
        @ValueSource(strings = {"ASD-2343", "A-1", "ABC-0123"})
        void accepts(String regNo) {
            assertTrue(Car.isRegNoValid(regNo));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "-123", "ABC-", "ABC", "ABC-12-34", "123-ABC", "AB1-234"})
        void rejects(String regNo) {
            // "-123" split into ["", "123"]; the empty letter half ran a loop zero
            // times and passed, so a registration with no letters was accepted.
            assertFalse(Car.isRegNoValid(regNo));
        }
    }

    @Nested
    @DisplayName("CNIC and contact number")
    class CnicAndContact {

        @ParameterizedTest
        @CsvSource({
            "1234567890123, true",
            "123456789012,  false",
            "12345678901234,false",
            "123456789012a, false",
            "'',            false"
        })
        void cnic(String value, boolean expected) {
            assertEquals(expected, Person.isCNICValid(value));
        }

        @ParameterizedTest
        @CsvSource({
            "03001234567, true",
            "04001234567, false",
            "0300123456,  false",
            "030012345678,false",
            "0300123456a, false",
            "'',          false"
        })
        void contactNo(String value, boolean expected) {
            assertEquals(expected, Person.isContactNoValid(value));
        }
    }
}
