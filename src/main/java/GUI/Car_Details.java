package GUI;

import BackendCode.service.UserService;
import BackendCode.Booking;
import BackendCode.Car;
import BackendCode.CarOwner;
import java.awt.Dimension;
import javax.swing.table.DefaultTableModel;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;

public final class Car_Details {

    // These are per-window state. They used to be static, which meant opening a
    // second one of these screens replaced the first one's widgets, leaving the
    // still-visible panel's listeners driving the newer screen's fields.
    private DefaultTableModel tablemodel;

    private JButton Add_Button,
            Update_Button, Remove_Button, BackButton, LogoutButton;
    private JScrollPane jScrollPane1;
    private JTable jTable1;
    private JTextField Filter_TextField;
    private JLabel Filter_Label;
    private JButton ExportCsv_Button;
    private JPanel MainPanel;

    /**
     * @return the tablemodel
     */
    public DefaultTableModel getTablemodel() {
        return tablemodel;
    }

    public JPanel getMainPanel() {
        return MainPanel;
    }

    public Car_Details() {
        MainPanel = new JPanel();
        Parent_JFrame.getMainFrame().setTitle("Car Details - Rent-A-Car Management System");
        MainPanel.setLayout(new AbsoluteLayout());
        MainPanel.setMinimumSize(new Dimension(1366, 730));



        Add_Button = new JButton("Add");
        Update_Button = new JButton("Update");
        Remove_Button = new JButton("Remove");
        BackButton = new JButton("Back");
        LogoutButton = new JButton("Logout");
        

        jScrollPane1 = new JScrollPane();
        jTable1 = new JTable();
//ID,  Maker,  Name,  Colour,  Type,  SeatingCapacity,  Model,  Condition,  RegNo, RentPerHour,  IsRented RentDate, carOwner customer

        Filter_Label = new JLabel("Filter");
        Filter_TextField = new JTextField();
        Filter_TextField.setToolTipText("Narrows the table as you type. Click a column header to sort.");
        ExportCsv_Button = new JButton("Export CSV");

        String[] columns = {"Sr#", "ID", "Maker", "Name", "Colour", "Type", "Seats", "Model", "Condition",
            "Reg No.", "Rent/hour", "Car Owner"};
        tablemodel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                //all cells false
                return false;
            }
        };

        jTable1 = new JTable(getTablemodel());
        jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//        jTable1.setPreferredScrollableViewportSize(new Dimension(2000, 550));
        jScrollPane1 = new JScrollPane();
        jScrollPane1.setViewportView(jTable1);
        jTable1.setFillsViewportHeight(true);// makes the size of table equal to that of scroll pane to fill the table in the scrollpane
        TableTools.attach(jTable1, Filter_TextField);
        ArrayList<Car> Car_objects = Car.View();
        for (int i = 0; i < Car_objects.size(); i++) {
//ID,  Maker,  Name,  Colour,  Type,  SeatingCapacity,  Model,  Condition,  RegNo, 
//RentPerHour,  IsRented RentDate, carOwner customer
            int ID = Car_objects.get(i).getID();
            String maker = Car_objects.get(i).getMaker();
            String Name = Car_objects.get(i).getName();
            String color = Car_objects.get(i).getColour();
            String type = Car_objects.get(i).getType();
            int seatingCapacity = Car_objects.get(i).getSeatingCapacity();
            String model = Car_objects.get(i).getModel();
            String condition = Car_objects.get(i).getCondition();
            String regNo = Car_objects.get(i).getRegNo();
            int rentPerHour = Car_objects.get(i).getRentPerHour();
            CarOwner carOwner = Car_objects.get(i).getCarOwner();

            String[] one_s_Record = {((i + 1) + ""), "" + ID, maker, Name, color, type, seatingCapacity + "",
                model, condition, regNo, rentPerHour + "", carOwner.getID() + ": " + carOwner.getName()};
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
        jTable1.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(8).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(9).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(10).setCellRenderer(centerRenderer);
        jTable1.getColumnModel().getColumn(11).setCellRenderer(centerRenderer);
        

        // adjusting size of each column
        jTable1.getColumnModel().getColumn(0).setMinWidth(20);
        jTable1.getColumnModel().getColumn(1).setMinWidth(20);
        jTable1.getColumnModel().getColumn(2).setMinWidth(170);
        jTable1.getColumnModel().getColumn(3).setMinWidth(170);
        jTable1.getColumnModel().getColumn(4).setMinWidth(140);
        jTable1.getColumnModel().getColumn(5).setMinWidth(150);
        jTable1.getColumnModel().getColumn(6).setMinWidth(90);
        jTable1.getColumnModel().getColumn(7).setMinWidth(90);
        jTable1.getColumnModel().getColumn(8).setMinWidth(160);
        jTable1.getColumnModel().getColumn(9).setMinWidth(170);
        jTable1.getColumnModel().getColumn(10).setMinWidth(150);
        jTable1.getColumnModel().getColumn(11).setMinWidth(150);
       

        jTable1.getTableHeader().setReorderingAllowed(false);

        MainPanel.add(Filter_Label, new AbsoluteConstraints(10, 15, 45, 22));
        MainPanel.add(Filter_TextField, new AbsoluteConstraints(60, 15, 320, 22));
        MainPanel.add(ExportCsv_Button, new AbsoluteConstraints(395, 15, 130, 22));
        MainPanel.add(jScrollPane1, new AbsoluteConstraints(10, 60, 1330, 550));
        MainPanel.add(Remove_Button, new AbsoluteConstraints(785, 625, 130, 22));
        MainPanel.add(Add_Button, new AbsoluteConstraints(450, 625, 130, 22));
        MainPanel.add(Update_Button, new AbsoluteConstraints(620, 625, 130, 22));
        MainPanel.add(BackButton, new AbsoluteConstraints(1106, 625, 100, 22));
        MainPanel.add(LogoutButton, new AbsoluteConstraints(1236, 625, 100, 22));
        
        Add_Button.addActionListener(new Car_Details_ActionListener());
        Update_Button.addActionListener(new Car_Details_ActionListener());
        Remove_Button.addActionListener(new Car_Details_ActionListener());
        ExportCsv_Button.addActionListener(new Car_Details_ActionListener());
        BackButton.addActionListener(new Car_Details_ActionListener());
        LogoutButton.addActionListener(new Car_Details_ActionListener());
        
    }

    private class Car_Details_ActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            switch (e.getActionCommand()) {

                case "Add": {
                    Parent_JFrame.getMainFrame().setEnabled(false);
                    Car_Add ac = new Car_Add();
                    ac.setVisible(true);
                }
                break;
                case "Update": {
                    Parent_JFrame.getMainFrame().setEnabled(false);
                    Car_Update ac = new Car_Update();
                    ac.setVisible(true);
                }
                break;
                case "Remove": {
                    Parent_JFrame.getMainFrame().setEnabled(false);
                    Car_Remove ac = new Car_Remove();
                    ac.setVisible(true);
                }
                break;
                case "Export CSV": {
                    TableTools.exportCsv(Parent_JFrame.getMainFrame(), jTable1, "cars.csv");
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
            }
        }
    }
}
