package GUI;

import BackendCode.Car;
import BackendCode.Customer;
import BackendCode.service.RentalService;
import BackendCode.service.ServiceResult;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import javax.swing.*;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

/**
 *
 * @author @AbdullahShahid01
 */
public final class Booking_BookCar extends JFrame {

    // Swing windows are Serializable by inheritance and this one is never actually
    // serialized, but pinning the ID keeps the compiler quiet about it.
    private static final long serialVersionUID = 1L;

    JButton Book_Button, Cancel_Button;
    JLabel CarID_Label, CarIDValidity_Label, CustomerID_Label, CustomerIDValidity_Label;
    JTextField CarID_TextField, CustomerID_TextField;

    private Car car;
    private Customer customer;

    public Booking_BookCar() {
        super("Book Car");
        setLayout(new FlowLayout());
        setSize(new Dimension(300, 200));
        setResizable(false);
        setLocationRelativeTo(Parent_JFrame.getMainFrame());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Parent_JFrame.getMainFrame().setEnabled(true);
                dispose();
            }
        });

        Book_Button = new JButton("Book");
        Cancel_Button = new JButton("Cancel");

        CarID_Label = new JLabel("Enter Car ID to be Booked");
        CarIDValidity_Label = new JLabel();
        CarID_TextField = new JTextField();

        CustomerID_Label = new JLabel("Enter Customer ID");
        CustomerIDValidity_Label = new JLabel();
        CustomerID_TextField = new JTextField();

        CarID_TextField.setPreferredSize(new Dimension(240, 22));
        CarIDValidity_Label.setPreferredSize(new Dimension(415, 9));

        CustomerID_TextField.setPreferredSize(new Dimension(240, 22));
        CustomerIDValidity_Label.setPreferredSize(new Dimension(415, 9));

        Book_Button.setPreferredSize(new Dimension(100, 22));
        Cancel_Button.setPreferredSize(new Dimension(100, 22));

        CarIDValidity_Label.setForeground(Color.red);
        CustomerIDValidity_Label.setForeground(Color.red);

        add(CarID_Label);
        add(CarID_TextField);
        add(CarIDValidity_Label);

        add(CustomerID_Label);
        add(CustomerID_TextField);
        add(CustomerIDValidity_Label);

        add(Book_Button);
        add(Cancel_Button);

        Book_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String CarID = CarID_TextField.getText().trim();
                if (!CarID.isEmpty()) {
                    try {
                        if (Integer.parseInt(CarID) > 0) {
                            CarIDValidity_Label.setText("");
                            car = Car.SearchByID(Integer.parseInt(CarID));
                            if (car != null) {
                                if (!car.isRented()) {
                                    CarIDValidity_Label.setText("");
                                } else {
                                    car = null;
                                    CarID = null; // stops the confirmation below from using the null car
                                    JOptionPane.showMessageDialog(null, "This car is already booked !");
                                }
                            } else {
                                CarID = null;
                                CarIDValidity_Label.setText("                                                            Car ID does not exists !");
                            }
                        } else {
                            CarID = null;
                            CarIDValidity_Label.setText("                                                            ID cannot be '0' or negative !");
                        }
                    } catch (NumberFormatException ex) {
                        CarID = null;
                        CarIDValidity_Label.setText("                                                            Invalid ID !");
                    }
                } else {
                    CarID = null;
                    CarIDValidity_Label.setText("                                                            Enter Car ID !");
                }

                String customerID = CustomerID_TextField.getText().trim();
                if (!customerID.isEmpty()) {
                    try {
                        if (Integer.parseInt(customerID) > 0) {
                            CustomerIDValidity_Label.setText("");
                            customer = Customer.SearchByID(Integer.parseInt(customerID));
                            if (customer != null) {
                                CustomerIDValidity_Label.setText("");
                            } else {
                                customerID = null;
                                JOptionPane.showMessageDialog(null, "Customer ID does not exists !");
                            }
                        } else {
                            customerID = null;
                            CustomerIDValidity_Label.setText("                                                            ID cannot be '0' or negative !");
                        }
                    } catch (NumberFormatException ex) {
                        customerID = null;
                        CustomerIDValidity_Label.setText("                                                            Invalid ID !");
                    }
                } else {
                    customerID = null;
                    CustomerIDValidity_Label.setText("                                                            Enter Customer ID !");
                }

                if (CarID != null && customerID != null) {
                    setEnabled(false);
                    int showConfirmDialog = JOptionPane.showConfirmDialog(null,
                            "You are about to Book the Car: \n" + car.toString() + "\n against the Customer: \n"
                            + customer.toString() + "\n Are you sure you want to continue??",
                            "Book Confirmation", JOptionPane.OK_CANCEL_OPTION);
                    if (showConfirmDialog == 0) {
                        ServiceResult result = RentalService.bookCar(car.getID(), customer.getID());
                        if (!result.isSuccess()) {
                            JOptionPane.showMessageDialog(null, result.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            setEnabled(true);
                            return;
                        }
                        Parent_JFrame.getMainFrame().getContentPane().removeAll();
                        Booking_Details cd = new Booking_Details();
                        Parent_JFrame.getMainFrame().add(cd.getMainPanel());
                        Parent_JFrame.getMainFrame().getContentPane().revalidate();
                        Parent_JFrame.getMainFrame().getContentPane().repaint();
                        JOptionPane.showMessageDialog(null, result.getMessage());
                        Parent_JFrame.getMainFrame().setEnabled(true);
                        dispose();
                    } else {
                        setEnabled(true); // cancelling the confirmation must not leave this window dead
                    }
                }
            }
        }
        );
        Cancel_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Parent_JFrame.getMainFrame().setEnabled(true);
                dispose();
            }
        });
    }
}
