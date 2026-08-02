package BackendCode;

import BackendCode.dao.BookingDao;
import BackendCode.dao.CarDao;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author @AbdullahShahid01
 */
public class Booking implements Serializable {

    /** @see Person#serialVersionUID */
    private static final long serialVersionUID = 6454849438621591085L;

    private static final long ONE_HOUR_IN_MS = 1000L * 60 * 60;

    private int ID;
    private Customer customer;
    private Car car;
    private long RentTime, ReturnTime; // stores System time when the Book() method is called

    public Booking() {
    }

    public Booking(int ID, Customer customer, Car car, long RentTime, long ReturnTime) {
        this.ID = ID;
        this.customer = customer;
        this.car = car;
        this.RentTime = RentTime;
        this.ReturnTime = ReturnTime;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public long getRentTime() {
        return RentTime;
    }

    public void setRentTime(long RentTime) {
        this.RentTime = RentTime;
    }

    public long getReturnTime() {
        return ReturnTime;
    }

    public void setReturnTime(long ReturnTime) {
        this.ReturnTime = ReturnTime;
    }

    @Override
    public String toString() {
        return "Booking{" + "ID=" + ID + ", \ncustomer=" + customer.toString() + ", \ncar=" + car.toString() + ", \nRentTime=" + RentTime + ", ReturnTime=" + ReturnTime + '}' + "\n";
    }

    /**
     * @return true if the row was written
     */
    public boolean Add() {
        this.ReturnTime = 0;
        return BookingDao.insert(this);
    }

    /**
     * @return true if the row was updated
     */
    public boolean Update() {
        return BookingDao.update(this);
    }

    /**
     * Deletes this booking. Unlike the routine this replaces, an id that is not
     * there is simply a no-op and does not take another record with it.
     *
     * @return true if the delete ran
     */
    public boolean Remove() {
        return BookingDao.delete(ID);
    }

    /**
     * Rent is charged per hour, and any part of an hour counts as a whole hour, so a
     * booking of 1h 59m is two hours rather than the one that truncating would give.
     * A booking that has not been returned yet has no bill.
     *
     * @return the rent owed for this booking, never negative
     */
    public int calculateBill() {
        // rent calculation
        long rentTime = this.getRentTime();
        long returnTime = this.getReturnTime();
        if (returnTime <= rentTime) {
//            still out, or a clock that went backwards -- either way there is
//            nothing to charge yet, and a negative bill would corrupt the balances
            return 0;
        }
        long totalTime = returnTime - rentTime;
//        round any started hour up to a full hour
        long hours = (totalTime + ONE_HOUR_IN_MS - 1) / ONE_HOUR_IN_MS;

        int rentPerHour = this.getCar().getRentPerHour();
        return (int) (rentPerHour * hours);
    }

    public static ArrayList<Booking> SearchByCustomerID(int CustomerID) {
        return BookingDao.findByCustomer(CustomerID);
    }

    public static ArrayList<Booking> SearchByCarRegNo(String CarRegNo) {
        return BookingDao.findByCarRegNo(CarRegNo);
    }

    public static ArrayList<Booking> SearchByCarID(int carID) {
        return BookingDao.findByCar(carID);
    }

    public static ArrayList<Booking> View() {
        return BookingDao.findAll();
    }

    /**
     * @return the cars that are currently out
     */
    public static ArrayList<Car> getBookedCars() {
        return BookingDao.findBookedCars();
    }

    /**
     * @return the cars with no outstanding booking
     */
    public static ArrayList<Car> getUnbookedCars() {
        return CarDao.findUnbooked();
    }
}
