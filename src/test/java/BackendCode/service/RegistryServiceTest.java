package BackendCode.service;

import BackendCode.AppUser;
import BackendCode.Car;
import BackendCode.CarOwner;
import BackendCode.Customer;
import BackendCode.Database;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules the Add and Update screens used to carry themselves.
 * <p>
 * Each of the six screens had its own copy of "is this CNIC or registration already
 * taken", and the Update dialogs had the extra wrinkle that a record may keep its own
 * value. Now there is one of each, and it can be checked without a window.
 */
class RegistryServiceTest {

    @BeforeEach
    void freshDatabase() {
        UserService.signOut();
        Database.close();
        new File(Database.fileName()).delete();
        for (String legacy : new String[]{"Customer.ser", "CarOwner.ser", "Car.ser", "Booking.ser"}) {
            new File(legacy).delete();
        }
        UserService.signIn("admin", "123".toCharArray());
        RegistryService.addCarOwner(new CarOwner(0, 0, "1111111111111", "Owner", "03001111111"));
    }

    private static Car carWith(String regNo) {
        return new Car(0, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                regNo, 100, CarOwner.SearchByID(1));
    }

    // ---------------- uniqueness on add ----------------

    @Test
    @DisplayName("a customer with a CNIC already on record is refused")
    void duplicateCustomerCnicRefused() {
        assertTrue(RegistryService.addCustomer(
                new Customer(0, 0, "2222222222222", "First", "03002222222")).isSuccess());

        ServiceResult second = RegistryService.addCustomer(
                new Customer(0, 0, "2222222222222", "Second", "03003333333"));

        assertFalse(second.isSuccess());
        assertTrue(second.getMessage().contains("already registered"));
        assertEquals(1, Customer.View().size());
    }

    @Test
    @DisplayName("a car with a registration already on record is refused")
    void duplicateRegNoRefused() {
        assertTrue(RegistryService.addCar(carWith("AAA-111")).isSuccess());

        ServiceResult second = RegistryService.addCar(carWith("AAA-111"));

        assertFalse(second.isSuccess());
        assertTrue(second.getMessage().contains("already registered"));
        assertEquals(1, Car.View().size());
    }

    @Test
    @DisplayName("a car naming an owner who does not exist is refused, not left to the foreign key")
    void carWithMissingOwnerRefused() {
        Car orphan = new Car(0, "Honda", "Civic", "Black", "Familycar", 4, "2021", "Good",
                "BBB-222", 200, new CarOwner(0, 999, "9", "Ghost", "0300"));

        ServiceResult result = RegistryService.addCar(orphan);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Owner ID"));
        assertEquals(0, Car.View().size());
    }

    @Test
    @DisplayName("a car with no owner at all is refused")
    void carWithNoOwnerRefused() {
        Car orphan = new Car(0, "Honda", "Civic", "Black", "Familycar", 4, "2021", "Good",
                "BBB-222", 200, null);

        assertFalse(RegistryService.addCar(orphan).isSuccess());
    }

    // ---------------- uniqueness on update ----------------

    @Test
    @DisplayName("a record may keep its own CNIC when edited")
    void updateKeepingOwnCnicAllowed() {
        RegistryService.addCustomer(new Customer(0, 0, "2222222222222", "Before", "03002222222"));
        Customer existing = Customer.SearchByID(1);
        existing.setName("After");

        // The naive "is this CNIC taken?" check refuses this, because the record finds
        // itself. Every Update dialog had to remember that; now one place does.
        ServiceResult result = RegistryService.updateCustomer(existing);

        assertTrue(result.isSuccess(), result.getMessage());
        assertEquals("After", Customer.SearchByID(1).getName());
    }

    @Test
    @DisplayName("a record may not take a CNIC that somebody else holds")
    void updateTakingAnotherCnicRefused() {
        RegistryService.addCustomer(new Customer(0, 0, "2222222222222", "First", "03002222222"));
        RegistryService.addCustomer(new Customer(0, 0, "3333333333333", "Second", "03003333333"));

        Customer second = Customer.SearchByID(2);
        second.setCNIC("2222222222222");           // the first customer's
        ServiceResult result = RegistryService.updateCustomer(second);

        assertFalse(result.isSuccess());
        assertEquals("3333333333333", Customer.SearchByID(2).getCNIC(), "unchanged on disk");
    }

    @Test
    @DisplayName("a car may keep its own registration when edited")
    void updateKeepingOwnRegNoAllowed() {
        RegistryService.addCar(carWith("AAA-111"));
        Car existing = Car.SearchByID(1);
        existing.setRentPerHour(250);

        assertTrue(RegistryService.updateCar(existing).isSuccess());
        assertEquals(250, Car.SearchByID(1).getRentPerHour());
    }

    @Test
    @DisplayName("a car may not take a registration another car holds")
    void updateTakingAnotherRegNoRefused() {
        RegistryService.addCar(carWith("AAA-111"));
        RegistryService.addCar(carWith("BBB-222"));

        Car second = Car.SearchByID(2);
        second.setRegNo("AAA-111");
        assertFalse(RegistryService.updateCar(second).isSuccess());
        assertEquals("BBB-222", Car.SearchByID(2).getRegNo(), "unchanged on disk");
    }

    @Test
    @DisplayName("an owner may keep their own CNIC when edited")
    void updateOwnerKeepingOwnCnicAllowed() {
        CarOwner owner = CarOwner.SearchByID(1);
        owner.setName("Renamed");

        assertTrue(RegistryService.updateCarOwner(owner).isSuccess());
        assertEquals("Renamed", CarOwner.SearchByID(1).getName());
    }

    // ---------------- who may write off money ----------------

    @Test
    @DisplayName("an administrator can clear a bill and a balance")
    void adminCanClearMoney() {
        RegistryService.addCustomer(new Customer(500, 0, "2222222222222", "Cust", "03002222222"));
        CarOwner owner = CarOwner.SearchByID(1);
        owner.setBalance(700);
        RegistryService.updateCarOwner(owner);

        assertTrue(RegistryService.clearBill(1).isSuccess());
        assertTrue(RegistryService.clearBalance(1).isSuccess());

        assertEquals(0, Customer.SearchByID(1).getBill());
        assertEquals(0, CarOwner.SearchByID(1).getBalance());
    }

    @Test
    @DisplayName("staff cannot clear a bill or a balance, and nothing changes when they try")
    void staffCannotClearMoney() {
        RegistryService.addCustomer(new Customer(500, 0, "2222222222222", "Cust", "03002222222"));
        CarOwner owner = CarOwner.SearchByID(1);
        owner.setBalance(700);
        RegistryService.updateCarOwner(owner);

        UserService.addUser("desk", "desk123".toCharArray(), AppUser.Role.STAFF);
        assertNotNull(UserService.signIn("desk", "desk123".toCharArray()));

        // The screen hides these buttons from staff, but the rule is enforced here, so
        // it holds for any caller rather than depending on a window behaving itself.
        assertFalse(RegistryService.clearBill(1).isSuccess());
        assertFalse(RegistryService.clearBalance(1).isSuccess());
        assertEquals(500, Customer.SearchByID(1).getBill(), "the bill must be untouched");
        assertEquals(700, CarOwner.SearchByID(1).getBalance(), "the balance must be untouched");
    }

    @Test
    @DisplayName("staff cannot remove an owner or a customer either")
    void staffCannotRemoveAccounts() {
        RegistryService.addCustomer(new Customer(0, 0, "2222222222222", "Cust", "03002222222"));
        UserService.addUser("desk", "desk123".toCharArray(), AppUser.Role.STAFF);
        UserService.signIn("desk", "desk123".toCharArray());

        assertFalse(RentalService.removeCustomer(1).isSuccess());
        assertFalse(RentalService.removeOwner(1).isSuccess());
        assertEquals(1, Customer.View().size());
        assertEquals(1, CarOwner.View().size());
    }

    @Test
    @DisplayName("clearing money for an id that is not there fails rather than throwing")
    void clearingUnknownIdFails() {
        assertFalse(RegistryService.clearBill(999).isSuccess());
        assertFalse(RegistryService.clearBalance(999).isSuccess());
    }
}
