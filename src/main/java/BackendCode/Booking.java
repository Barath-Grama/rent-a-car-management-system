package BackendCode;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author @AbdullahShahid01
 */
public class Booking implements Serializable {

    /** @see BackendCode.Person#serialVersionUID */
    private static final long serialVersionUID = 1L;

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
     * Rewrites Booking.ser from the given list.
     *
     * @return false if the data did not reach the file, so that callers can tell the
     *         user instead of reporting a success that did not happen
     */
    private static boolean writeAll(ArrayList<Booking> booking) {
        ObjectOutputStream outputStream = null;
        boolean written = false;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream("Booking.ser"));
            for (int i = 0; i < booking.size(); i++) {
                outputStream.writeObject(booking.get(i));
            }
            written = true;
        } catch (FileNotFoundException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
//                    the stream buffers, so a failed close can mean unflushed records
                    System.out.println(ex);
                    written = false;
                }
            }
        }
        return written;
    }

    /**
     * @return true if the record reached the file
     */
    public boolean Add() {
        ArrayList<Booking> booking = Booking.View();
        // Auto ID ... one past the highest ID ever issued, so that removing the
        // newest record does not hand its ID to the next one added.
        int highestID = 0;
        for (int i = 0; i < booking.size(); i++) {
            if (booking.get(i).ID > highestID) {
                highestID = booking.get(i).ID;
            }
        }
        this.ID = IDGenerator.next("Booking.id", highestID);
        this.ReturnTime = 0;
        booking.add(this);
        return writeAll(booking);
    }

    /**
     * @return true if the record reached the file
     */
    public boolean Update() {
        ArrayList<Booking> booking = Booking.View();

        // for loop for replacing the new Booking object with old one with same ID
        for (int i = 0; i < booking.size(); i++) {
            if (booking.get(i).ID == ID) {
                booking.set(i, this);
            }
        }
        return writeAll(booking);
    }

    /**
     * @return true if the record was removed from the file
     */
    public boolean Remove() {

        ArrayList<Booking> booking = Booking.View();
        // for loop for deleting the required Booking
        for (int i = 0; i < booking.size(); i++) {
            if ((booking.get(i).ID == ID)) {
                booking.remove(i);
                i--; // the next record has shifted down into this index
            }
        }
        return writeAll(booking);
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
        ArrayList<Booking> bookingList = new ArrayList<>(0);
        ArrayList<Booking> booking = Booking.View();
        for (int i = 0; i < booking.size(); i++) {
            if (booking.get(i).customer != null && booking.get(i).customer.getID() == CustomerID) {
                bookingList.add(booking.get(i));
            }
        }
        return bookingList;
    }

    public static ArrayList<Booking> SearchByCarRegNo(String CarRegNo) {
        ArrayList<Booking> bookingList = new ArrayList<>(0);
        ArrayList<Booking> booking = Booking.View();
        for (int i = 0; i < booking.size(); i++) {
            if (booking.get(i).car != null && booking.get(i).car.getRegNo().equalsIgnoreCase(CarRegNo)) {
                bookingList.add(booking.get(i));
            }
        }
        return bookingList;
    }

    public static ArrayList<Booking> SearchByCarID(int carID) {
        ArrayList<Booking> bookingList = new ArrayList<>(0);
        ArrayList<Booking> booking = Booking.View();
        for (int i = 0; i < booking.size(); i++) {
            if (booking.get(i).car != null && booking.get(i).car.getID() == carID) {
                bookingList.add(booking.get(i));
            }
        }
        return bookingList;
    }

    public static ArrayList<Booking> View() {
        ArrayList<Booking> bookingList = new ArrayList<>(0);
        ObjectInputStream inputStream = null;
        try {
// open file for reading
            inputStream = new ObjectInputStream(new FileInputStream("Booking.ser"));
            boolean EOF = false;
// Keep reading file until file ends
            while (!EOF) {
                try {
                    Booking myObj = (Booking) inputStream.readObject();
                    bookingList.add(myObj);
                } catch (ClassNotFoundException e) {
                    System.out.println(e);
                } catch (EOFException end) {
                    EOF = true;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        refreshReferences(bookingList);
        return bookingList;
    }

    /**
     * Every Booking in Booking.ser carries its own serialized copies of the Customer
     * and the Car, taken when the Booking was saved. Once those records are edited
     * through Customer_Update / Car_Update, the embedded copies are out of date --
     * and their Bill / Balance fields are stale enough to corrupt money if written
     * back. This swaps each embedded copy for the current record from its own file,
     * matched on ID. A record that no longer exists keeps its embedded copy, so
     * bookings for a deleted car still read as history.
     *
     * @param bookings the freshly deserialized bookings, updated in place
     */
    private static void refreshReferences(ArrayList<Booking> bookings) {
        if (bookings.isEmpty()) {
            return;
        }
        ArrayList<Customer> customers = Customer.View();
        ArrayList<Car> cars = Car.View();
        for (int i = 0; i < bookings.size(); i++) {
            Booking booking = bookings.get(i);
            if (booking.customer != null) {
                for (int j = 0; j < customers.size(); j++) {
                    if (customers.get(j).getID() == booking.customer.getID()) {
                        booking.customer = customers.get(j);
                        break;
                    }
                }
            }
            if (booking.car != null) {
                for (int j = 0; j < cars.size(); j++) {
                    if (cars.get(j).getID() == booking.car.getID()) {
                        booking.car = cars.get(j);
                        break;
                    }
                }
            }
        }
    }

    public static ArrayList<Car> getBookedCars() {
        ArrayList<Car> bookedCars = new ArrayList<>();
        ArrayList<Booking> bookings = Booking.View();
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).ReturnTime == 0) {
                bookedCars.add(bookings.get(i).car);
            }
        }
        return bookedCars;
    }

    public static ArrayList<Car> getUnbookedCars() {
        ArrayList<Car> allCars = Car.View();
        ArrayList<Car> bookedCars = Booking.getBookedCars();
        ArrayList<Car> unbookedCars = new ArrayList<>();
//        Car.View() and getBookedCars() read from two different files, so the same
//        car is two different objects. They have to be matched on ID, not on identity.
        for (int i = 0; i < allCars.size(); i++) {
            boolean isBooked = false;
            for (int j = 0; j < bookedCars.size(); j++) {
                if (allCars.get(i).getID() == bookedCars.get(j).getID()) {
                    isBooked = true;
                    break;
                }
            }
            if (!isBooked) {
                unbookedCars.add(allCars.get(i));
            }
        }
        return unbookedCars;
    }
}
