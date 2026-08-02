package BackendCode;

import BackendCode.dao.CustomerDao;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * A person who rents cars.
 * <p>
 * Persistence lives in {@link CustomerDao}; the methods here keep the names the
 * screens already call, so the user interface did not have to change when the flat
 * files were replaced by a database.
 * <p>
 * Still {@link Serializable} only so {@link SerImporter} can read the legacy
 * {@code .ser} files. See {@link Person#serialVersionUID} before touching that field.
 *
 * @author @AbdullahShahid01
 */
public class Customer extends Person implements Serializable {

    /** @see Person#serialVersionUID */
    private static final long serialVersionUID = -5667876069786539052L;

    private int Bill; // increases after every HOUR when a customers has Booked car(s)

    public Customer() {
        super();
    }

    public Customer(int Bill, int ID, String CNIC, String Name, String Contact_No) {
        super(ID, CNIC, Name, Contact_No);
        this.Bill = Bill;
    }

    public int getBill() {
        return Bill;
    }

    public void setBill(int Bill) {
        this.Bill = Bill;
    }

    @Override
    public String toString() {
        return super.toString() + "Customer{" + "Bill=" + Bill + '}' + "\n";
    }

    /**
     * Inserts this customer and takes the id the database assigns.
     *
     * @return true if the row was written
     */
    @Override
    public boolean Add() {
        return CustomerDao.insert(this);
    }

    /**
     * @return true if the row was updated
     */
    @Override
    public boolean Update() {
        return CustomerDao.update(this);
    }

    /**
     * Deletes this customer. Their bookings go with them, by cascade.
     *
     * @return true if the row was removed
     */
    @Override
    public boolean Remove() {
        return CustomerDao.delete(ID);
    }

    public static ArrayList<Customer> SearchByName(String name) {
        return CustomerDao.findByName(name);
    }

    public static Customer SearchByCNIC(String CustomerCNIC) {
        return CustomerDao.findByCnic(CustomerCNIC);
    }

    public static Customer SearchByID(int id) {
        return CustomerDao.findById(id);
    }

    public static ArrayList<Customer> View() {
        return CustomerDao.findAll();
    }
}
