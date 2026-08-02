package BackendCode;

import BackendCode.dao.CarDao;
import BackendCode.dao.CarOwnerDao;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Somebody who puts one or more cars up for rent and earns from them.
 * <p>
 * Persistence lives in {@link CarOwnerDao}. Still {@link Serializable} only so
 * {@link SerImporter} can read the legacy {@code .ser} files.
 *
 * @author @AbdullahShahid01
 */
public class CarOwner extends Person implements Serializable {

    /** @see Person#serialVersionUID */
    private static final long serialVersionUID = -9067758019325475740L;

    private int Balance; // increases after every HOUR when Owner's car(s) is booked

    public CarOwner() {
        super();
    }

    public CarOwner(int Balance, int ID, String CNIC, String Name, String Contact_No) {
        super(ID, CNIC, Name, Contact_No);
        this.Balance = Balance;
    }

    public int getBalance() {
        return Balance;
    }

    public void setBalance(int Balance) {
        this.Balance = Balance;
    }

    @Override
    public String toString() {
        return super.toString() + " CarOwner{" + "Balance=" + Balance + '}' + "\n";
    }

    /**
     * @return true if the row was written
     */
    @Override
    public boolean Add() {
        return CarOwnerDao.insert(this);
    }

    /**
     * @return true if the row was updated
     */
    @Override
    public boolean Update() {
        return CarOwnerDao.update(this);
    }

    /**
     * Deletes this owner. Their cars go with them by cascade, and each car's
     * bookings with it.
     *
     * @return true if the row was removed
     */
    @Override
    public boolean Remove() {
        return CarOwnerDao.delete(ID);
    }

    public static ArrayList<CarOwner> SearchByName(String name) {
        return CarOwnerDao.findByName(name);
    }

    public static CarOwner SearchByCNIC(String carOwnerCNIC) {
        return CarOwnerDao.findByCnic(carOwnerCNIC);
    }

    public static CarOwner SearchByID(int id) {
        return CarOwnerDao.findById(id);
    }

    public ArrayList<Car> getAllCars() {
        return CarDao.findByOwner(ID);
    }

    public static ArrayList<CarOwner> View() {
        return CarOwnerDao.findAll();
    }
}
