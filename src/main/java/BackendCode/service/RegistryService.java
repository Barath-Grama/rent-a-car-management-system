package BackendCode.service;

import BackendCode.Car;
import BackendCode.CarOwner;
import BackendCode.Customer;
import BackendCode.dao.CarDao;
import BackendCode.dao.CarOwnerDao;
import BackendCode.dao.CustomerDao;

/**
 * Adding and editing the records the business is made of, and writing off what is
 * owed.
 * <p>
 * These rules used to sit in the screens: each Add dialog looked up the CNIC or
 * registration itself to see whether it was taken, and each Update dialog repeated the
 * same check with the extra wrinkle that a record is allowed to keep its own value.
 * Six screens, four slightly different versions of one rule.
 * <p>
 * What stays in the screens is per-field validation, because that drives a red label
 * beside the individual box that is wrong, and a single result cannot say which one
 * that is. What arrives here is a filled-in record; what goes back is whether it was
 * accepted and what to tell the user.
 *
 * @author @Barath-Grama
 */
public final class RegistryService {

    private RegistryService() {
    }

    // ---------------- customers ----------------

    /**
     * @param customer a filled-in customer with no id yet
     * @return what happened, with a message for the user
     */
    public static ServiceResult addCustomer(Customer customer) {
        ServiceResult fields = validate(customer.getCNIC(), customer.getName(), customer.getContact_No());
        if (!fields.isSuccess()) {
            return fields;
        }
        if (CustomerDao.findByCnic(customer.getCNIC()) != null) {
            return ServiceResult.failed("This CNIC is already registered !");
        }
        if (!customer.Add()) {
            return ServiceResult.failed("The customer could not be saved.");
        }
        return ServiceResult.ok("Customer added successfully !");
    }

    /**
     * @param customer the edited customer, carrying the id of the record to replace
     * @return what happened, with a message for the user
     */
    public static ServiceResult updateCustomer(Customer customer) {
        ServiceResult fields = validate(customer.getCNIC(), customer.getName(), customer.getContact_No());
        if (!fields.isSuccess()) {
            return fields;
        }
        Customer holder = CustomerDao.findByCnic(customer.getCNIC());
//        a CNIC already on record is only a clash when somebody else holds it
        if (holder != null && holder.getID() != customer.getID()) {
            return ServiceResult.failed("This CNIC is already registered !");
        }
        if (!customer.Update()) {
            return ServiceResult.failed("The customer could not be saved.");
        }
        return ServiceResult.ok("Record Successfully Updated !");
    }

    // ---------------- car owners ----------------

    public static ServiceResult addCarOwner(CarOwner owner) {
        ServiceResult fields = validate(owner.getCNIC(), owner.getName(), owner.getContact_No());
        if (!fields.isSuccess()) {
            return fields;
        }
        if (CarOwnerDao.findByCnic(owner.getCNIC()) != null) {
            return ServiceResult.failed("This CNIC is already registered !");
        }
        if (!owner.Add()) {
            return ServiceResult.failed("The car owner could not be saved.");
        }
        return ServiceResult.ok("Car Owner added successfully !");
    }

    public static ServiceResult updateCarOwner(CarOwner owner) {
        ServiceResult fields = validate(owner.getCNIC(), owner.getName(), owner.getContact_No());
        if (!fields.isSuccess()) {
            return fields;
        }
        CarOwner holder = CarOwnerDao.findByCnic(owner.getCNIC());
        if (holder != null && holder.getID() != owner.getID()) {
            return ServiceResult.failed("This CNIC is already registered !");
        }
        if (!owner.Update()) {
            return ServiceResult.failed("The car owner could not be saved.");
        }
        return ServiceResult.ok("Record Successfully Updated !");
    }

    // ---------------- cars ----------------

    /**
     * @param car a filled-in car with no id yet, carrying the owner it belongs to
     * @return what happened, with a message for the user
     */
    public static ServiceResult addCar(Car car) {
        ServiceResult fields = validateCar(car);
        if (!fields.isSuccess()) {
            return fields;
        }
        ServiceResult owner = checkOwner(car);
        if (!owner.isSuccess()) {
            return owner;
        }
        if (CarDao.findByRegNo(car.getRegNo()) != null) {
            return ServiceResult.failed("This Car Registeration no is already registered !");
        }
        if (!car.Add()) {
            return ServiceResult.failed("The car could not be saved.");
        }
        return ServiceResult.ok("Record Successfully Added !");
    }

    /**
     * @param car the edited car, carrying the id of the record to replace
     * @return what happened, with a message for the user
     */
    public static ServiceResult updateCar(Car car) {
        ServiceResult fields = validateCar(car);
        if (!fields.isSuccess()) {
            return fields;
        }
        ServiceResult owner = checkOwner(car);
        if (!owner.isSuccess()) {
            return owner;
        }
        Car holder = CarDao.findByRegNo(car.getRegNo());
        if (holder != null && holder.getID() != car.getID()) {
            return ServiceResult.failed("This Car Registeration no is already registered !");
        }
        if (!car.Update()) {
            return ServiceResult.failed("The car could not be saved.");
        }
        return ServiceResult.ok("Record Successfully Updated !");
    }

    /**
     * The screens check these field by field so they can put each message beside the
     * box it belongs to. Repeating the check here is not redundancy: it is what stops
     * a record reaching the database unvalidated because a new caller forgot.
     */
    private static ServiceResult validate(String cnic, String name, String contact) {
        return RecordValidator.validatePerson(cnic, name, contact).asServiceResult();
    }

    private static ServiceResult validateCar(Car car) {
        return RecordValidator.validateCar(car.getMaker(), car.getName(), car.getRegNo(),
                car.getCarOwner() == null ? null : String.valueOf(car.getCarOwner().getID()),
                String.valueOf(car.getRentPerHour())).asServiceResult();
    }

    /**
     * A car with no owner, or one naming an owner who has since been deleted, cannot
     * satisfy the foreign key. Catching it here gives the user a sentence instead of a
     * constraint violation in the log.
     */
    private static ServiceResult checkOwner(Car car) {
        if (car.getCarOwner() == null || CarOwnerDao.findById(car.getCarOwner().getID()) == null) {
            return ServiceResult.failed("Owner ID doesnot exists !");
        }
        return ServiceResult.ok("");
    }

    // ---------------- writing off what is owed ----------------

    /**
     * Sets a customer's outstanding bill back to zero.
     * <p>
     * The permission check lives here rather than in the screen that offers the button:
     * a rule enforced at the point of the change cannot be missed by a new caller, and
     * it can be tested without opening a window.
     *
     * @return what happened, with a message for the user
     */
    public static ServiceResult clearBill(int customerId) {
        if (!UserService.currentCanManageAccounts()) {
            return ServiceResult.failed("Only an administrator can clear a customer's bill.");
        }
        Customer customer = CustomerDao.findById(customerId);
        if (customer == null) {
            return ServiceResult.failed("This ID does not exist !");
        }
        customer.setBill(0);
        if (!customer.Update()) {
            return ServiceResult.failed("The bill could not be cleared.");
        }
        return ServiceResult.ok("Bill Successfully Cleared !");
    }

    /**
     * Sets a car owner's accumulated balance back to zero, having paid it out.
     *
     * @return what happened, with a message for the user
     */
    public static ServiceResult clearBalance(int ownerId) {
        if (!UserService.currentCanManageAccounts()) {
            return ServiceResult.failed("Only an administrator can clear a car owner's balance.");
        }
        CarOwner owner = CarOwnerDao.findById(ownerId);
        if (owner == null) {
            return ServiceResult.failed("This ID does not exist !");
        }
        owner.setBalance(0);
        if (!owner.Update()) {
            return ServiceResult.failed("The balance could not be cleared.");
        }
        return ServiceResult.ok("Balance Successfully Cleared !");
    }
}
