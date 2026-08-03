package BackendCode.service;

import BackendCode.Booking;
import BackendCode.Car;
import BackendCode.CarOwner;
import BackendCode.Customer;
import BackendCode.Database;
import BackendCode.dao.BookingDao;
import BackendCode.dao.CarDao;
import BackendCode.dao.CarOwnerDao;
import BackendCode.dao.CustomerDao;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * The rules of renting a car out and taking it back.
 * <p>
 * These used to live inside Swing action listeners. {@code Booking_UnBookCar} read
 * the booking, worked out the bill, credited the owner and charged the customer, all
 * in the body of a button handler. That is why the money bug was possible: three
 * separate writes with nothing tying them together, each able to fail on its own and
 * leave the books disagreeing. It is also why none of it could be tested without
 * opening a window.
 * <p>
 * Everything that touches money now goes through one transaction.
 *
 * @author @Barath-Grama
 */
public final class RentalService {

    private static final long ONE_DAY = 24L * 60 * 60 * 1000;

    /** How a booking window is written back to the user. */
    private static final SimpleDateFormat WHEN = new SimpleDateFormat("dd-MM-yyyy hh:mm a");

    private RentalService() {
    }

    /**
     * Books a car to a customer.
     *
     * @param carId      the car to rent out
     * @param customerId the customer renting it
     * @return what happened, with a message for the user
     */
    public static ServiceResult bookCar(int carId, int customerId) {
        long now = System.currentTimeMillis();
        return reserve(carId, customerId, now, now + ONE_DAY, true);
    }

    /**
     * Reserves a car for a window, without collecting it.
     *
     * @param carId      the car to hold
     * @param customerId who it is held for
     * @param startsAt   when the hold begins
     * @param endsAt     when it ends
     * @return what happened, with a message for the user
     */
    public static ServiceResult reserve(int carId, int customerId, long startsAt, long endsAt) {
        return reserve(carId, customerId, startsAt, endsAt, false);
    }

    /**
     * @param collectNow true for a walk-in taking the car straight away, false for a
     *                   reservation somebody will come back for
     */
    private static ServiceResult reserve(int carId, int customerId, long startsAt, long endsAt,
                                         boolean collectNow) {
        Car car = CarDao.findById(carId);
        if (car == null) {
            return ServiceResult.failed("Car ID does not exist !");
        }
        Customer customer = CustomerDao.findById(customerId);
        if (customer == null) {
            return ServiceResult.failed("Customer ID does not exist !");
        }
        if (endsAt <= startsAt) {
            return ServiceResult.failed("The return date must be after the collection date.");
        }

        ArrayList<Booking> clashes = BookingDao.findClashing(carId, startsAt, endsAt);
        if (!clashes.isEmpty()) {
            Booking first = clashes.get(0);
            return ServiceResult.failed("This car is already spoken for between "
                    + WHEN.format(new Date(first.getStartsAt())) + " and "
                    + WHEN.format(new Date(first.getEndsAt())) + ".");
        }

        Booking booking = new Booking(0, customer, car, startsAt, endsAt,
                collectNow ? Long.valueOf(startsAt) : null, 0);
        if (!booking.Add()) {
            return ServiceResult.failed("The booking could not be saved.");
        }
        return ServiceResult.ok(collectNow
                ? "Car Successfully Booked !"
                : "Car reserved from " + WHEN.format(new Date(startsAt))
                  + " to " + WHEN.format(new Date(endsAt)) + " !");
    }

    /**
     * Hands over a car somebody reserved earlier.
     *
     * @param bookingId the reservation being collected
     * @return what happened, with a message for the user
     */
    public static ServiceResult collect(int bookingId) {
        for (Booking booking : BookingDao.findAwaitingCollection()) {
            if (booking.getID() != bookingId) {
                continue;
            }
            if (CarDao.isRented(booking.getCar().getID())) {
                return ServiceResult.failed("That car is still out with somebody else.");
            }
            booking.setRentTime(System.currentTimeMillis());
            if (!booking.Update()) {
                return ServiceResult.failed("The collection could not be saved.");
            }
            return ServiceResult.ok("Car collected !");
        }
        return ServiceResult.failed("No reservation is waiting to be collected under that ID.");
    }

    /**
     * Takes a car back: closes its open booking, charges the customer for the time it
     * was out and credits the owner the same amount.
     * <p>
     * The three writes commit together or not at all. Both the owner and the customer
     * are re-read here rather than taken from the booking, which is a habit worth
     * keeping even though the database no longer stores stale copies of them.
     *
     * @param carId the car being returned
     * @return what happened, with a message for the user
     */
    public static ServiceResult returnCar(int carId) {
        Car car = CarDao.findById(carId);
        if (car == null) {
            return ServiceResult.failed("Car ID does not exist !");
        }
        Booking open = openBookingFor(carId);
        if (open == null) {
            return ServiceResult.failed("This car is not booked !");
        }

        open.setReturnTime(System.currentTimeMillis());
        int bill = open.calculateBill();
//        record what was charged rather than leaving it to be recomputed later from a
//        rate that may since have changed
        open.setAmountCharged(bill);

        boolean committed = Database.inTransaction(() -> {
            if (!BookingDao.update(open)) {
                return false;
            }
            CarOwner owner = CarOwnerDao.findById(open.getCar().getCarOwner().getID());
            if (owner == null) {
                return false;
            }
            owner.setBalance(owner.getBalance() + bill);
            if (!CarOwnerDao.update(owner)) {
                return false;
            }
            Customer customer = CustomerDao.findById(open.getCustomer().getID());
            if (customer == null) {
                return false;
            }
            customer.setBill(customer.getBill() + bill);
            return CustomerDao.update(customer);
        });

        if (!committed) {
            return ServiceResult.failed("The return could not be completed, so nothing was changed.");
        }
        return ServiceResult.ok("Car Successfully UnBooked !\nRent charged: " + bill);
    }

    /**
     * @return the car's booking that has not been returned, or null if it is not out
     */
    public static Booking openBookingFor(int carId) {
        ArrayList<Booking> bookings = BookingDao.findByCar(carId);
        for (int i = bookings.size() - 1; i >= 0; i--) {
//            a reservation nobody has collected is not a car that can be brought back
            if (bookings.get(i).isOut()) {
                return bookings.get(i);
            }
        }
        return null;
    }

    /**
     * Retires a car, refusing while it is still out. Its booking history stays: the
     * rentals happened, and the accounts have to keep showing them.
     *
     * @param carId the car to remove
     * @return what happened, with a message for the user
     */
    public static ServiceResult removeCar(int carId) {
        Car car = CarDao.findById(carId);
        if (car == null) {
            return ServiceResult.failed("Car ID not found !");
        }
        if (CarDao.isRented(carId)) {
            return ServiceResult.failed("This car is currently booked !"
                    + "\nUnBook it before removing it.");
        }
        if (!car.Remove()) {
            return ServiceResult.failed("The car could not be removed.");
        }
        return ServiceResult.ok("Record successfully Removed !");
    }

    /**
     * Retires an owner and their cars. Nothing is erased: every booking those cars
     * were on still names a real car and a real owner. Refuses while any of their cars
     * is still out, so a rental in progress cannot be retired out from under the
     * customer holding the car.
     *
     * @param ownerId the owner to remove
     * @return what happened, with a message for the user
     */
    public static ServiceResult removeOwner(int ownerId) {
//        Enforced here rather than only in the screen that offers the button, so the
//        rule holds for any caller and can be tested without opening a window. The
//        screen asks the same question first, but only to avoid walking someone
//        through a confirmation it is going to refuse.
        if (!UserService.currentCanManageAccounts()) {
            return ServiceResult.failed("Only an administrator can remove a car owner.");
        }
        CarOwner owner = CarOwnerDao.findById(ownerId);
        if (owner == null) {
            return ServiceResult.failed("This ID does not exist !");
        }
        StringBuilder stillOut = new StringBuilder();
        for (Car car : CarDao.findByOwner(ownerId)) {
            if (CarDao.isRented(car.getID())) {
                stillOut.append("\n").append(car.getID()).append(": ").append(car.getName());
            }
        }
        if (stillOut.length() > 0) {
            return ServiceResult.failed("This Car Owner still has cars that are booked :"
                    + stillOut + "\nUnBook them before removing the owner.");
        }
        if (!owner.Remove()) {
            return ServiceResult.failed("The car owner could not be removed.");
        }
        return ServiceResult.ok("Record successfully Removed !");
    }

    /**
     * Retires a customer. Their booking history stays, so what they rented and what
     * they were charged remains on the books.
     *
     * @param customerId the customer to remove
     * @return what happened, with a message for the user
     */
    public static ServiceResult removeCustomer(int customerId) {
//        see removeOwner: the rule belongs with the change, not only with the button
        if (!UserService.currentCanManageAccounts()) {
            return ServiceResult.failed("Only an administrator can remove a customer.");
        }
        Customer customer = CustomerDao.findById(customerId);
        if (customer == null) {
            return ServiceResult.failed("This ID does not exist !");
        }
//        the same rule cars get: somebody holding one of our cars cannot be retired
        for (Booking booking : BookingDao.findByCustomer(customerId)) {
            if (booking.getReturnTime() == 0) {
                return ServiceResult.failed("This customer still has a car out ("
                        + booking.getCar().getName() + ")."
                        + "\nTake it back before removing them.");
            }
        }
        if (!customer.Remove()) {
            return ServiceResult.failed("The customer could not be removed.");
        }
        return ServiceResult.ok("Record successfully Removed !");
    }
}
