package BackendCode;

import BackendCode.dao.CarDao;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author @AbdullahShahid01
 */
public class Car implements Serializable {

    /** @see Person#serialVersionUID */
    private static final long serialVersionUID = 4702469846254473835L;

    private int ID;
    private String Maker, Name, Colour, Type;
    int SeatingCapacity;
    String Model, Condition, RegNo;
    private int RentPerHour;
    private CarOwner carOwner;

    public Car() {
    }

    public Car(int ID, String Maker, String Name, String Colour, String Type, int SeatingCapacity, String Model, String Condition, String RegNo, int RentPerHour, CarOwner carOwner) {
        this.ID = ID;
        this.Maker = Maker;
        this.Name = Name;
        this.Colour = Colour;
        this.Type = Type;
        this.SeatingCapacity = SeatingCapacity;
        this.Model = Model;
        this.Condition = Condition;
        this.RegNo = RegNo;
        this.RentPerHour = RentPerHour;
        this.carOwner = carOwner;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getMaker() {
        return Maker;
    }

    public void setMaker(String Maker) {
        this.Maker = Maker;
    }

    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }

    public String getColour() {
        return Colour;
    }

    public void setColour(String Colour) {
        this.Colour = Colour;
    }

    public String getType() {
        return Type;
    }

    public void setType(String Type) {
        this.Type = Type;
    }

    public int getSeatingCapacity() {
        return SeatingCapacity;
    }

    public void setSeatingCapacity(int SeatingCapacity) {
        this.SeatingCapacity = SeatingCapacity;
    }

    public String getModel() {
        return Model;
    }

    public void setModel(String Model) {
        this.Model = Model;
    }

    public String getCondition() {
        return Condition;
    }

    public void setCondition(String Condition) {
        this.Condition = Condition;
    }

    public String getRegNo() {
        return RegNo;
    }

    public void setRegNo(String RegNo) {
        this.RegNo = RegNo;
    }

    public int getRentPerHour() {
        return RentPerHour;
    }

    public void setRentPerHour(int RentPerHour) {
        this.RentPerHour = RentPerHour;
    }

    public CarOwner getCarOwner() {
        return carOwner;
    }

    public void setCarOwner(CarOwner carOwner) {
        this.carOwner = carOwner;
    }

    @Override
    public String toString() {
        return "Car_new{" + "ID=" + ID + ", Maker=" + Maker + ", Name=" + Name + ", Colour=" + Colour + ", \nType=" + Type + ", SeatingCapacity=" + SeatingCapacity + ", Model=" + Model + ", Condition=" + Condition + ", RegNo=" + RegNo + ", RentPerHour=" + RentPerHour + ", \ncarOwner=" + carOwner.toString() + '}' + "\n";
    }

    /**
     * @return true if the row was written
     */
    public boolean Add() {
        return CarDao.insert(this);
    }

    /**
     * aik new Object bna k us se Update() ka method call krte hein aur us new
     * object mein jo Car ID hai agar usi ID ki koi car pehle mojood ha tou us
     * se replace ho jay gi
     *
     * @return true if the row was updated
     */
    public boolean Update() {
        return CarDao.update(this);
    }

    /**
     * Deletes this car. Its bookings go with it, by cascade.
     *
     * @return true if the row was removed
     */
    public boolean Remove() {
        return CarDao.delete(ID);
    }

    public static ArrayList<Car> SearchByName(String name) {
        return CarDao.findByName(name);
    }

    public static Car SearchByID(int id) {
        return CarDao.findById(id);
    }

    public static Car SearchByRegNo(String regNo) {
        return CarDao.findByRegNo(regNo);
    }

    public static ArrayList<Car> View() {
        return CarDao.findAll();
    }

    /**
     * @return true if this car is out on a booking that has not been returned
     */
    public boolean isRented() {
        return CarDao.isRented(ID);
    }

    public static boolean isNameValid(String Name) {
//        an empty name is not a valid name
        if (Name.isEmpty()) {
            return false;
        }
        boolean flag = true;
        for (int i = 0; i < Name.length(); i++) {
//            Name can contain white spaces
            if (!Character.isLetter(Name.charAt(i)) && !Character.isDigit(Name.charAt(i))
                    && Name.charAt(i) != ' ') {
                flag = false;
                break;
            }
        }
        return flag;
    }

    public static boolean isRegNoValid(String RegNo) {
        // reg no must contain letters followed by digits, both separated by '-' dash
        // EXAMPLE: ASD-2343
        String[] token = RegNo.split("-");
        if (token.length == 2) {
//            both halves have to be there: "-123" splits into an empty half and "123",
//            and an empty half passes a loop that never runs
            if (token[0].isEmpty() || token[1].isEmpty()) {
                return false;
            }
            for (int i = 0; i < token[0].length(); i++) {
                if (!Character.isLetter(token[0].charAt(i))) {
                    return false;
                }
            }
            for (int i = 0; i < token[1].length(); i++) {
                if (!Character.isDigit(token[1].charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }


}
