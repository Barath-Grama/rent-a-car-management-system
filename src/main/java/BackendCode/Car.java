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
public class Car implements Serializable {

    /** @see BackendCode.Person#serialVersionUID */
    private static final long serialVersionUID = 1L;

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
     * Rewrites Car.ser from the given list.
     *
     * @return false if the data did not reach the file, so that callers can tell the
     *         user instead of reporting a success that did not happen
     */
    private static boolean writeAll(ArrayList<Car> car) {
        ObjectOutputStream outputStream = null;
        boolean written = false;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream("Car.ser"));
            for (int i = 0; i < car.size(); i++) {
                outputStream.writeObject(car.get(i));
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
        ArrayList<Car> car = Car.View();
        // Auto ID... one past the highest ID ever issued, so that removing the
        // newest record does not hand its ID to the next one added.
        int highestID = 0;
        for (int i = 0; i < car.size(); i++) {
            if (car.get(i).ID > highestID) {
                highestID = car.get(i).ID;
            }
        }
        this.ID = IDGenerator.next("Car.id", highestID);
        car.add(this);
        return writeAll(car);
    }

    /**
     * aik new Object bna k us se Update() ka method call krte hein aur us new
     * object mein jo Car ID hai agar usi ID ki koi car pehle mojood ha tou us
     * se replace ho jay gi
     *
     * @return true if the record reached the file
     */
    public boolean Update() {
        ArrayList<Car> car = Car.View();

        // for loop for replacing the new Car object with old one with same ID
        for (int i = 0; i < car.size(); i++) {
            if (car.get(i).ID == ID) {
                car.set(i, this);
            }
        }
        return writeAll(car);
    }

    /**
     * @return true if the record was removed from the file
     */
    public boolean Remove() {

        ArrayList<Car> car = Car.View();
        // for loop for deleting the required Car
        for (int i = 0; i < car.size(); i++) {
            if ((car.get(i).ID == ID)) {
                car.remove(i);
                i--; // the next record has shifted down into this index
            }
        }
        return writeAll(car);
    }

    public static ArrayList<Car> SearchByName(String name) {
        ArrayList<Car> car = Car.View();
        ArrayList<Car> s = new ArrayList<>();
        for (int i = 0; i < car.size(); i++) {
            if (car.get(i).Name.equalsIgnoreCase(name)) {
                s.add(car.get(i));
            }
        }
        return s;
    }

    public static Car SearchByID(int id) {
        ArrayList<Car> car = Car.View();
        for (int i = 0; i < car.size(); i++) {
            if (car.get(i).ID == id) {
                return car.get(i);
            }
        }
        return null;
    }

    public static Car SearchByRegNo(String regNo) {
        ArrayList<Car> car = Car.View();
        for (int i = 0; i < car.size(); i++) {
            if (car.get(i).RegNo.equalsIgnoreCase(regNo)) {
                return car.get(i);
            }
        }
        return null;
    }

    public static ArrayList<Car> View() {
        ArrayList<Car> carList = new ArrayList<>(0);
        ObjectInputStream inputStream = null;
        try {
// open file for reading
            inputStream = new ObjectInputStream(new FileInputStream("Car.ser"));
            boolean EOF = false;
// Keep reading file until file ends
            while (!EOF) {
                try {
                    Car myObj = (Car) inputStream.readObject();
                    carList.add(myObj);
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
        refreshOwners(carList);
        return carList;
    }

    /**
     * Every Car in Car.ser carries its own serialized copy of its CarOwner, taken
     * when the Car was saved. Once that owner is edited through CarOwner_Update,
     * the embedded copy is out of date. This swaps each embedded copy for the
     * current record from CarOwner.ser, matched on ID.
     *
     * @param cars the freshly deserialized cars, updated in place
     */
    private static void refreshOwners(ArrayList<Car> cars) {
        if (cars.isEmpty()) {
            return;
        }
        ArrayList<CarOwner> owners = CarOwner.View();
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            if (car.carOwner == null) {
                continue;
            }
            for (int j = 0; j < owners.size(); j++) {
                if (owners.get(j).getID() == car.carOwner.getID()) {
                    car.carOwner = owners.get(j);
                    break;
                }
            }
        }
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

    public boolean isRented() {
        ArrayList<Car> BookedCars = Booking.getBookedCars();
        for (int i = 0; i < BookedCars.size(); i++) {
            if (BookedCars.get(i).ID == this.ID) {
                return true;
            }
        }
        return false;
    }

}
