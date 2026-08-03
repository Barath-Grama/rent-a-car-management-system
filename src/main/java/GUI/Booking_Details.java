package GUI;

import BackendCode.service.UserService;
import BackendCode.Booking;
import BackendCode.Car;
import BackendCode.Customer;
import java.awt.Dimension;
import javax.swing.table.DefaultTableModel;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;

public final class Booking_Details {

    // These are per-window state. They used to be static, which meant opening a
    // second one of these screens replaced the first one's widgets, leaving the
    // still-visible panel's listeners driving the newer screen's fields.
    private DefaultTableModel tablemodel;

    private JButton BackButton, LogoutButton, BookCar_Button, UnbookCar_Button;
    private JScrollPane jScrollPane1;
    private JTable jTable1;
    private JTextField Filter_TextField;
    private JLabel Filter_Label;
    private JButton ExportCsv_Button;
    private JPanel MainPanel;

    public Booking_Details() {
        MainPanel = new JPanel();
        Parent_JFrame.getMainFrame().setTitle("Booking Details - Rent-A-Car Management System");
        MainPanel.setLayout(new AbsoluteLayout());
        MainPanel.setMinimumSize(new Dimension(1366, 730));

        BackButton = new JButton("Back");
        LogoutButton = new JButton("Logout");
        BookCar_Button = new JButton("Book");
        UnbookCar_Button = new JButton("Unbook");

        jScrollPane1 = new JScrollPane();
        jTable1 = new JTable();
//ID,  Maker,  Name,  Colour,  Type,  SeatingCapacity,  Model,  Condition,  RegNo, RentPerHour,  IsRented RentDate, carOwner customer

        Filter_Label = new JLabel("Filter");
        Filter_TextField = new JTextField();
        Filter_TextField.setToolTipText("Narrows the table as you type. Click a column header to sort.");
        ExportCsv_Button = new JButton("Export CSV");

        String[] columns = {"Sr#", "ID", "Customer ID+Name", "Car Name", "Booked From", "Booked To", "Status"};
        tablemodel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                //all cells false
                return false;
            }
        };

        jTable1 = new JTable(getTablemodel());
        jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jScrollPane1 = new JScrollPane();
        jScrollPane1.setViewportView(jTable1);
        jTable1.setFillsViewportHeight(true);// makes the size of table equal to that of scroll pane to fill the table in the scrollpane
        TableTools.attach(jTable1, Filter_TextField);
        ArrayList<Booking> Booking_objects = Booking.View();
        for (int i = 0; i < Booking_objects.size(); i++) {
//ID,  Maker,  Name,  Colour,  Type,  SeatingCapacity,  Model,  Condition,  RegNo, 
//RentPerHour,  IsRented RentDate, carOwner customer
            int ID = Booking_objects.get(i).getID();
            String customer_ID_Name = Booking_objects.get(i).getCustomer().getID()
                    + ": " + Booking_objects.get(i).getCustomer().getName();
            String carName = Booking_objects.get(i).getCar().getName();
            String carID = Booking_objects.get(i).getCar().getID()+"";
//            hh, not HH: HH is the 24-hour clock, and pairing it with the am/pm
//            marker rendered half the day as nonsense like "21:22 pm"
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a dd-MM-yyyy");
            Date rentime = new Date(Booking_objects.get(i).getStartsAt());
            String rentTime = dateFormat.format(rentime);

            String status;
            if (Booking_objects.get(i).getReturnTime() != 0) {
                status = "Returned " + dateFormat.format(new Date(Booking_objects.get(i).getReturnTime()));
            } else if (Booking_objects.get(i).isOut()) {
                status = "Out since " + dateFormat.format(new Date(Booking_objects.get(i).getRentTime()));
            } else {
                status = "Reserved, not collected";
            }

            String bookedTo = dateFormat.format(new Date(Booking_objects.get(i).getEndsAt()));

            String[] one_s_Record = {((i + 1) + ""), "" + ID, customer_ID_Name, carID+": "+carName,
                rentTime, bookedTo, status};
            tablemodel.addRow(one_s_Record);
        }

        // center aligning the text in all the columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        jTable1.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        // adjusting size of each column
        jTable1.getColumnModel().getColumn(0).setMinWidth(80);
        jTable1.getColumnModel().getColumn(1).setMinWidth(80);
        jTable1.getColumnModel().getColumn(2).setMinWidth(400);
        jTable1.getColumnModel().getColumn(3).setMinWidth(300);
        jTable1.getColumnModel().getColumn(4).setMinWidth(230);
        jTable1.getColumnModel().getColumn(5).setMinWidth(200);
        jTable1.getColumnModel().getColumn(6).setMinWidth(230);

        jTable1.getTableHeader().setReorderingAllowed(false);

        MainPanel.add(Filter_Label, new AbsoluteConstraints(10, 15, 45, 22));
        MainPanel.add(Filter_TextField, new AbsoluteConstraints(60, 15, 320, 22));
        MainPanel.add(ExportCsv_Button, new AbsoluteConstraints(395, 15, 130, 22));
        MainPanel.add(jScrollPane1, new AbsoluteConstraints(10, 60, 1330, 550));
        MainPanel.add(BackButton, new AbsoluteConstraints(1106, 625, 100, 22));
        MainPanel.add(LogoutButton, new AbsoluteConstraints(1236, 625, 100, 22));
        MainPanel.add(BookCar_Button, new AbsoluteConstraints(10, 625, 130, 22));
        MainPanel.add(UnbookCar_Button, new AbsoluteConstraints(160, 625, 130, 22));


        ExportCsv_Button.addActionListener(new Booking_Details_ActionListener());
        BackButton.addActionListener(new Booking_Details_ActionListener());
        LogoutButton.addActionListener(new Booking_Details_ActionListener());
        BookCar_Button.addActionListener(new Booking_Details_ActionListener());
        UnbookCar_Button.addActionListener(new Booking_Details_ActionListener());
    }

    public DefaultTableModel getTablemodel() {
        return tablemodel;
    }

    public JPanel getMainPanel() {
        return MainPanel;
    }

    private class Booking_Details_ActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            switch (e.getActionCommand()) {

                case "Export CSV": {
                    TableTools.exportCsv(Parent_JFrame.getMainFrame(), jTable1, "bookings.csv");
                }
                break;
                case "Back": {
                    Parent_JFrame.getMainFrame().setTitle("Rent-A-Car Management System [REBORN]");
                    MainMenu mm = new MainMenu();
                    Parent_JFrame.getMainFrame().getContentPane().removeAll();
                    Parent_JFrame.getMainFrame().add(mm.getMainPanel());
                    Parent_JFrame.getMainFrame().getContentPane().revalidate();
                    Parent_JFrame.getMainFrame().getContentPane().repaint();
                }
                break;
                case "Logout": {
                    UserService.signOut();
                Parent_JFrame.getMainFrame().dispose();
                    Runner.showLogin();
                }
                break;
                case "Book": {
                    if (!Booking.getUnbookedCars().isEmpty()) {
                        Parent_JFrame.getMainFrame().setEnabled(false);
                        Booking_BookCar ac = new Booking_BookCar();
                        ac.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(null, "No UnBooked Cars are available !");
                    }
                }
                break;
                case "Unbook": {
                    if (!Booking.getBookedCars().isEmpty()) {
                        Parent_JFrame.getMainFrame().setEnabled(false);
                        Booking_UnBookCar ac = new Booking_UnBookCar();
                        ac.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(null, "No Booked Cars found !");
                    }
                }
                break;
            }
        }
    }
}
