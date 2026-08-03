package GUI;

import BackendCode.Car;
import BackendCode.service.RentalService;
import BackendCode.service.ServiceResult;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;

/**
 *
 * @author @AbdullahShahid01
 */
public final class Booking_UnBookCar extends JFrame {

    // Swing windows are Serializable by inheritance and this one is never actually
    // serialized, but pinning the ID keeps the compiler quiet about it.
    private static final long serialVersionUID = 1L;

    JButton UnBook_Button, Cancel_Button;
    JLabel CarID_Label, CarIDValidity_Label;
    JTextField CarID_TextField;

    private Car car;

    public Booking_UnBookCar() {
        super("UnBook Car");
        setLayout(new FlowLayout());
        setSize(new Dimension(300, 145));
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

        UnBook_Button = new JButton("UnBook");
        Cancel_Button = new JButton("Cancel");

        CarID_Label = new JLabel("Enter Car ID to be UnBooked");
        CarIDValidity_Label = new JLabel();
        CarID_TextField = new JTextField();

        CarID_TextField.setPreferredSize(new Dimension(240, 22));
        CarIDValidity_Label.setPreferredSize(new Dimension(415, 9));

        UnBook_Button.setPreferredSize(new Dimension(100, 22));
        Cancel_Button.setPreferredSize(new Dimension(100, 22));

        CarIDValidity_Label.setForeground(Color.red);

        add(CarID_Label);
        add(CarID_TextField);
        add(CarIDValidity_Label);

        add(UnBook_Button);
        add(Cancel_Button);

        UnBook_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String carID = CarID_TextField.getText().trim();
                if (!carID.isEmpty()) {
                    try {
                        if (Integer.parseInt(carID) > 0) {
                            CarIDValidity_Label.setText("");
                            car = Car.SearchByID(Integer.parseInt(carID));
                            if (car != null) {
                                if (car.isRented()) {
                                    CarIDValidity_Label.setText("");
                                } else {
                                    car = null;
                                    Dialogs.get().info(null, "This car is not booked !");
                                }
                            } else {
                                car = null;
                                Dialogs.get().info(null, "Car ID does not exists !");
                            }
                        } else {
                            carID = null;
                            CarIDValidity_Label.setText("                                                            "
                                    + "ID cannot be '0' or negative !");
                        }
                    } catch (NumberFormatException ex) {
                        carID = null;
                        CarIDValidity_Label.setText("                                                            "
                                + "Invalid ID !");
                    }
                } else {
                    carID = null;
                    CarIDValidity_Label.setText("                                                            "
                            + "Enter Car ID !");
                }

                if (carID != null && car != null) {
                    setEnabled(false);
                    boolean showConfirmDialog = Dialogs.get().confirm(null, "You are about to UnBook this Car\n" + car.toString()
                            + "\n Are you sure you want to continue ??", "UnBook Confirmation");
                    if (showConfirmDialog) {
//                        Closing the booking, crediting the owner and charging the
//                        customer are one transaction inside the service. This window
//                        only reports what it decided.
                        ServiceResult result = RentalService.returnCar(car.getID());
                        if (!result.isSuccess()) {
                            Dialogs.get().error(null, result.getMessage());
                            setEnabled(true);
                            return;
                        }

                        Parent_JFrame.getMainFrame().getContentPane().removeAll();
                        Booking_Details cd = new Booking_Details();
                        Parent_JFrame.getMainFrame().add(cd.getMainPanel());
                        Parent_JFrame.getMainFrame().getContentPane().revalidate();
                        Parent_JFrame.getMainFrame().getContentPane().repaint();
                        Dialogs.get().info(null, result.getMessage());
                        Parent_JFrame.getMainFrame().setEnabled(true);
                        dispose();
                    } else {
                        setEnabled(true);
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
