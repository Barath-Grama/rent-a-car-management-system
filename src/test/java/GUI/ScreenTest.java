package GUI;

import BackendCode.AppUser;
import BackendCode.Booking;
import BackendCode.Car;
import BackendCode.CarOwner;
import BackendCode.Customer;
import BackendCode.Database;
import BackendCode.service.RentalService;
import BackendCode.service.UserService;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The screens themselves: real windows, real buttons, the real listeners.
 * <p>
 * These press the same {@code ActionListener} a click does, so they cover the wiring
 * no service test can reach -- that a button is connected at all, that it reads the
 * right text field, that it puts a validation message on the right label, and that it
 * says what the service told it to say.
 * <p>
 * Two things make it possible. {@link Dialogs} is swapped for a recording stand-in, so
 * a modal dialog no longer waits for a click that is never coming. And a window really
 * is created, so the suite needs a display: on a headless machine these skip rather
 * than fail, and CI runs them under Xvfb.
 */
@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless",
        disabledReason = "these open real windows; CI provides a display with Xvfb")
class ScreenTest {

    private RecordingDialogs dialogs;

    @BeforeEach
    void freshEverything() {
        UserService.signOut();
        Database.close();
        new File(Database.fileName()).delete();
        for (String legacy : new String[]{"Customer.ser", "CarOwner.ser", "Car.ser", "Booking.ser"}) {
            new File(legacy).delete();
        }
        dialogs = new RecordingDialogs();
        Dialogs.set(dialogs);
        UserService.signIn("admin", "123".toCharArray());

        new CarOwner(0, 0, "1111111111111", "Owner One", "03001111111").Add();
        new Customer(0, 0, "2222222222222", "Cust One", "03002222222").Add();
        new Car(0, "Toyota", "Corolla", "White", "Familycar", 4, "2020", "Good",
                "AAA-111", 100, CarOwner.SearchByID(1)).Add();
        new Car(0, "Honda", "Civic", "Black", "Familycar", 4, "2021", "Good",
                "BBB-222", 200, CarOwner.SearchByID(1)).Add();

        new Parent_JFrame();
    }

    @AfterEach
    void putTheRealDialogsBack() {
        Dialogs.set(null);
        Parent_JFrame.getMainFrame().dispose();
    }

    // ---------------- adding through a form ----------------

    @Test
    @DisplayName("the Add Customer screen saves what was typed")
    void addCustomerScreenSaves() {
        Customer_Add screen = new Customer_Add();
        screen.CNIC_TextField.setText("3333333333333");
        screen.Name_TextField.setText("Typed In");
        screen.Contact_TextField.setText("03003333333");

        screen.Add_Button.doClick();

        assertEquals(2, Customer.View().size());
        assertNotNull(Customer.SearchByCNIC("3333333333333"));
        assertTrue(dialogs.said("added successfully"), dialogs.messages().toString());
    }

    @Test
    @DisplayName("a bad field puts its message on that field's own label, and saves nothing")
    void addCustomerScreenShowsPerFieldProblems() {
        Customer_Add screen = new Customer_Add();
        screen.CNIC_TextField.setText("123");            // too short
        screen.Name_TextField.setText("Valid Name");
        screen.Contact_TextField.setText("nonsense");    // not a phone number

        screen.Add_Button.doClick();

        assertTrue(screen.CNICValidity_Label.getText().contains("Invalid CNIC"),
                "the CNIC label should carry the CNIC problem");
        assertEquals("", screen.NameValidity_Label.getText(),
                "the name was fine, so its label stays empty");
        assertTrue(screen.contactValidity_Label.getText().contains("Invalid Contact"),
                "the contact label should carry the contact problem");
        assertEquals(1, Customer.View().size(), "nothing should have been written");
    }

    @Test
    @DisplayName("the Add Car screen refuses a registration that is already taken")
    void addCarScreenRefusesDuplicate() {
        Car_Add screen = new Car_Add();
        screen.Maker_TextField.setText("Suzuki");
        screen.Name_TextField.setText("Swift");
        screen.RegNo_TextField.setText("AAA-111");        // car 1 already has this
        screen.OwnerID_TextField.setText("1");
        screen.RentPerHour_TextField.setText("300");

        screen.Add_Button.doClick();

        assertEquals(2, Car.View().size(), "no third car");
        assertTrue(dialogs.said("already registered"), dialogs.messages().toString());
    }

    @Test
    @DisplayName("the Add Car screen refuses an owner who does not exist")
    void addCarScreenRefusesUnknownOwner() {
        Car_Add screen = new Car_Add();
        screen.Maker_TextField.setText("Suzuki");
        screen.Name_TextField.setText("Swift");
        screen.RegNo_TextField.setText("ZZZ-999");
        screen.OwnerID_TextField.setText("404");
        screen.RentPerHour_TextField.setText("300");

        screen.Add_Button.doClick();

        assertEquals(2, Car.View().size());
        assertTrue(dialogs.said("Owner ID"), dialogs.messages().toString());
    }

    // ---------------- booking ----------------

    @Test
    @DisplayName("the Book screen books the car it was given")
    void bookScreenBooks() {
        Booking_BookCar screen = new Booking_BookCar();
        screen.CarID_TextField.setText("1");
        screen.CustomerID_TextField.setText("1");

        screen.Book_Button.doClick();

        assertEquals(1, Booking.View().size());
        assertTrue(Car.SearchByID(1).isRented());
        assertTrue(dialogs.said("Successfully Booked"), dialogs.messages().toString());
    }

    @Test
    @DisplayName("cancelling the confirmation books nothing and leaves the window usable")
    void bookScreenHonoursCancel() {
        Booking_BookCar screen = new Booking_BookCar();
        screen.CarID_TextField.setText("1");
        screen.CustomerID_TextField.setText("1");
        dialogs.answering(false);

        screen.Book_Button.doClick();

        assertEquals(0, Booking.View().size(), "cancelling must not book the car");
        assertTrue(screen.isEnabled(),
                "the window must not be left disabled after a cancelled confirmation");
    }

    @Test
    @DisplayName("the Book screen takes a reservation window")
    void bookScreenReservesAWindow() {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm");
        long day = 24L * 60 * 60 * 1000;
        Booking_BookCar screen = new Booking_BookCar();
        screen.CarID_TextField.setText("1");
        screen.CustomerID_TextField.setText("1");
        screen.From_TextField.setText(format.format(new java.util.Date(System.currentTimeMillis() + day)));
        screen.To_TextField.setText(format.format(new java.util.Date(System.currentTimeMillis() + 2 * day)));

        screen.Book_Button.doClick();

        assertEquals(1, Booking.View().size());
        assertTrue(Booking.View().get(0).isAwaitingCollection(),
                "a future window is a reservation, not a car going out now");
        assertFalse(Car.SearchByID(1).isRented());
    }

    @Test
    @DisplayName("a date the screen cannot read is reported and books nothing")
    void bookScreenRejectsUnreadableDate() {
        Booking_BookCar screen = new Booking_BookCar();
        screen.CarID_TextField.setText("1");
        screen.CustomerID_TextField.setText("1");
        screen.From_TextField.setText("next tuesday");

        screen.Book_Button.doClick();

        assertEquals(0, Booking.View().size());
        assertTrue(screen.WindowValidity_Label.getText().contains("dd-MM-yyyy"),
                "the screen should say what format it wants");
    }

    @Test
    @DisplayName("the Unbook screen returns the car and reports the rent")
    void unbookScreenReturns() {
        RentalService.bookCar(1, 1);
        Booking out = Booking.View().get(0);
        out.setRentTime(System.currentTimeMillis() - (2 * 60 * 60 * 1000L - 60_000));
        out.Update();

        Booking_UnBookCar screen = new Booking_UnBookCar();
        screen.CarID_TextField.setText("1");
        screen.UnBook_Button.doClick();

        assertFalse(Car.SearchByID(1).isRented());
        assertEquals(200, Customer.SearchByID(1).getBill());
        assertTrue(dialogs.said("Rent charged: 200"), dialogs.messages().toString());
    }

    // ---------------- removal ----------------

    @Test
    @DisplayName("the Remove Car screen refuses a car that is out")
    void removeCarScreenRefusesWhileOut() {
        RentalService.bookCar(1, 1);

        Car_Remove screen = new Car_Remove();
        screen.CarID_TextField.setText("1");
        screen.Remove_Button.doClick();

        assertNotNull(Car.SearchByID(1));
        assertTrue(dialogs.said("currently booked"), dialogs.messages().toString());
    }

    @Test
    @DisplayName("the Remove Car screen retires a free car but keeps its history")
    void removeCarScreenRetires() {
        RentalService.bookCar(1, 1);
        Booking out = Booking.View().get(0);
        out.setRentTime(System.currentTimeMillis() - 60 * 60 * 1000L);
        out.Update();
        RentalService.returnCar(1);

        Car_Remove screen = new Car_Remove();
        screen.CarID_TextField.setText("1");
        screen.Remove_Button.doClick();

        assertNull(Car.SearchByID(1), "gone from the fleet");
        assertEquals(1, Booking.View().size(), "but the rental stays on the books");
    }

    // ---------------- the list screens ----------------

    @Test
    @DisplayName("the fleet screen lists every live car and filters as you type")
    void carDetailsListsAndFilters() {
        Car_Details screen = new Car_Details();
        JTable table = findTable(screen.getMainPanel());
        JTextField filter = findFilter(screen.getMainPanel());
        assertEquals(2, table.getRowCount());

        filter.setText("civic");

        assertEquals(1, table.getRowCount(), "only the Civic matches");
        filter.setText("");
        assertEquals(2, table.getRowCount(), "clearing the filter brings them back");
    }

    @Test
    @DisplayName("a retired car disappears from the fleet screen")
    void retiredCarLeavesTheList() {
        assertEquals(2, findTable(new Car_Details().getMainPanel()).getRowCount());

        RentalService.removeCar(2);

        assertEquals(1, findTable(new Car_Details().getMainPanel()).getRowCount());
    }

    @Test
    @DisplayName("the bookings screen shows whether each booking is reserved, out or returned")
    void bookingDetailsShowsState() {
        long day = 24L * 60 * 60 * 1000;
        RentalService.bookCar(1, 1);                                        // out
        RentalService.reserve(2, 1, System.currentTimeMillis() + day,
                System.currentTimeMillis() + 2 * day);                      // reserved

        JTable table = findTable(new Booking_Details().getMainPanel());

        assertEquals(2, table.getRowCount());
        String firstStatus = String.valueOf(table.getValueAt(0, 6));
        String secondStatus = String.valueOf(table.getValueAt(1, 6));
        assertTrue(firstStatus.startsWith("Out since"), firstStatus);
        assertTrue(secondStatus.contains("Reserved"), secondStatus);
    }

    // ---------------- permissions, from the screen's side ----------------

    @Test
    @DisplayName("staff are refused when they try to clear a bill from the screen")
    void staffCannotClearBillFromTheScreen() {
        Customer customer = Customer.SearchByID(1);
        customer.setBill(500);
        customer.Update();
        UserService.addUser("desk", "desk123".toCharArray(), AppUser.Role.STAFF);
        UserService.signIn("desk", "desk123".toCharArray());

        Customer_Details screen = new Customer_Details();
        dialogs.choosing("1");
        clickButton(screen.getMainPanel(), "Clear Bill");

        assertEquals(500, Customer.SearchByID(1).getBill(), "the bill must be untouched");
        assertTrue(dialogs.said("administrator"), dialogs.messages().toString());
    }

    @Test
    @DisplayName("an administrator can clear a bill from the screen")
    void adminCanClearBillFromTheScreen() {
        Customer customer = Customer.SearchByID(1);
        customer.setBill(500);
        customer.Update();

        Customer_Details screen = new Customer_Details();
        dialogs.choosing("1");
        clickButton(screen.getMainPanel(), "Clear Bill");

        assertEquals(0, Customer.SearchByID(1).getBill());
        assertTrue(dialogs.said("Cleared"), dialogs.messages().toString());
    }

    // ---------------- helpers ----------------

    private static void clickButton(Container root, String text) {
        JButton button = find(root, JButton.class, text);
        assertNotNull(button, "no button labelled " + text);
        button.doClick();
    }

    private static JTable findTable(Container root) {
        return find(root, JTable.class, null);
    }

    private static JTextField findFilter(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextField && "Narrows the table as you type. Click a column header to sort."
                    .equals(((javax.swing.JComponent) c).getToolTipText())) {
                return (JTextField) c;
            }
            if (c instanceof Container) {
                JTextField found = findFilter((Container) c);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Container root, Class<T> type, String text) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)
                    && (text == null || (c instanceof JButton && text.equals(((JButton) c).getText())))) {
                return (T) c;
            }
            if (c instanceof Container) {
                T found = find((Container) c, type, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
