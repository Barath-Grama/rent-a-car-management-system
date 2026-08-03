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
    /** The window the car is spoken for: set when the booking is made. */
    private long startsAt;
    private long endsAt;

    /**
     * When the customer actually collected the car, or null for a reservation nobody
     * has turned up for yet. Distinct from {@link #startsAt}: a car booked for Tuesday
     * and collected on Tuesday afternoon has one window and a different pickup.
     */
    private Long collectedAt;

    /**
     * Dead to the program, kept alive for the files.
     * <p>
     * The legacy {@code .ser} records declare this as a primitive {@code long}. Java
     * serialization matches fields on name <em>and</em> type and rejects the whole
     * object when they disagree -- before {@code readObject} gets a chance to fix
     * anything up. Changing this field to a {@code Long} therefore made every legacy
     * booking unreadable, silently, and no test caught it because the fixtures were
     * written by the current classes. So the old field stays exactly as it was, and
     * {@link #readObject} copies it into {@link #collectedAt}.
     */
    private long RentTime;

    private long ReturnTime; // stores System time when the car came back, 0 while it is out

    /**
     * What was actually charged when the car came back, or null while it is still out.
     * <p>
     * Recorded rather than recomputed: rent_per_hour is editable, so working the
     * figure out from the car's current rate would rewrite past takings every time
     * somebody adjusted a price.
     * <p>
     * Nullable, not 0-for-absent. A sentinel value that is also a legitimate reading is
     * the mistake {@code ReturnTime} makes -- 0 is a real epoch millisecond, which is
     * why "still out" and "returned in 1970" are the same value there. Zero happens not
     * to be a reachable charge today, because every rental bills at least one hour at a
     * rate validated above zero, but that is a coincidence of the current rules rather
     * than something the type guarantees.
     * <p>
     * Adding this field does not stop {@link SerImporter} reading the old .ser files.
     * That is precisely what pinning {@code serialVersionUID} buys: the stream has no
     * value for it and deserialization leaves it null.
     */
    private Integer amountCharged;

    /**
     * When this reservation was given up as a no-show, or null if it was not.
     * <p>
     * A hold that nobody collects has to stop blocking the car at some point, or one
     * forgotten booking takes it off the fleet permanently. Recorded rather than
     * inferred from the clock: whether somebody turned up is a fact about what
     * happened, and a derived answer would change retroactively every time the grace
     * period was adjusted.
     */
    private Long expiredAt;

    public Booking() {
    }

    /**
     * Fills in what a legacy {@code .ser} record cannot carry.
     * <p>
     * Those files predate both the reservation window and the nullable collection
     * time. The old primitive {@code RentTime} is copied across, and the window is
     * reconstructed as the time the car was actually out -- a day for one still on
     * loan, which is the shortest honest guess.
     *
     * @see #RentTime
     */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (collectedAt == null && RentTime != 0) {
            collectedAt = RentTime;
        }
        if (startsAt == 0 && collectedAt != null) {
            startsAt = collectedAt;
            endsAt = ReturnTime != 0 ? ReturnTime : collectedAt + 24L * 60 * 60 * 1000;
        }
    }

    /**
     * An immediate rental: the window is the moment of collection onward, and the car
     * is taken straight away. Kept for the callers and tests that predate reservations.
     */
    public Booking(int ID, Customer customer, Car car, long RentTime, long ReturnTime) {
        this(ID, customer, car, RentTime, RentTime, RentTime, ReturnTime);
    }

    /**
     * @param startsAt   when the car is spoken for from
     * @param endsAt     when it is due back
     * @param RentTime   when it was actually collected, or null if it has not been
     * @param ReturnTime when it was actually returned, or 0 if it has not been
     */
    public Booking(int ID, Customer customer, Car car, long startsAt, long endsAt,
                   Long RentTime, long ReturnTime) {
        this.ID = ID;
        this.customer = customer;
        this.car = car;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.collectedAt = RentTime;
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

    /**
     * @return when the car was collected, or null for a reservation not yet collected
     */
    public Long getRentTime() {
        return collectedAt;
    }

    public void setRentTime(Long RentTime) {
        this.collectedAt = RentTime;
    }

    public long getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(long startsAt) {
        this.startsAt = startsAt;
    }

    public long getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(long endsAt) {
        this.endsAt = endsAt;
    }

    /**
     * @return true if the car has been collected and not yet brought back
     */
    public boolean isOut() {
        return collectedAt != null && ReturnTime == 0;
    }

    /**
     * @return true if this is a live reservation still waiting to be collected
     */
    public boolean isAwaitingCollection() {
        return collectedAt == null && ReturnTime == 0 && expiredAt == null;
    }

    /**
     * @return true if nobody turned up for this reservation in time
     */
    public boolean isExpired() {
        return expiredAt != null;
    }

    /**
     * @return when it was given up, or null if it was not
     */
    public Long getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Long expiredAt) {
        this.expiredAt = expiredAt;
    }

    /**
     * @param graceMillis how long after the window opens a customer has to turn up
     * @param now         the moment to judge against
     * @return true if this hold has run out of time and should be given up
     */
    public boolean hasLapsed(long graceMillis, long now) {
        return isAwaitingCollection() && now > startsAt + graceMillis;
    }

    /**
     * @return true if this booking's window overlaps the given one. Touching at an
     *         endpoint is not an overlap: a car due back at noon can go out again at
     *         noon.
     */
    public boolean overlaps(long otherStart, long otherEnd) {
        return startsAt < otherEnd && otherStart < endsAt;
    }

    /**
     * @return what this booking charged, or null if the car has not come back
     */
    public Integer getAmountCharged() {
        return amountCharged;
    }

    public void setAmountCharged(Integer amountCharged) {
        this.amountCharged = amountCharged;
    }

    public long getReturnTime() {
        return ReturnTime;
    }

    public void setReturnTime(long ReturnTime) {
        this.ReturnTime = ReturnTime;
    }

    @Override
    public String toString() {
//        String.valueOf rather than customer.toString(): a Booking built with the
//        no-arg constructor has neither, and a toString that throws is worse than one
//        that prints null. See Car#toString.
        return "Booking{" + "ID=" + ID + ", \ncustomer=" + String.valueOf(customer) + ", \ncar=" + String.valueOf(car) + ", \nRentTime=" + RentTime + ", ReturnTime=" + ReturnTime + '}' + "\n";
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
        Long collected = this.getRentTime();
        if (collected == null) {
//            never collected, so nothing was used and nothing is owed
            return 0;
        }
        long rentTime = collected;
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
