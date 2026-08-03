package GUI;

import BackendCode.CarOwner;
import BackendCode.service.RegistryService;
import BackendCode.service.ServiceResult;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

public class CarOwner_Update {

    JButton OK_Button, Cancel_Button;
    JLabel ID_Label, IDValidity_Label;
    JTextField ID_TextField;
    JFrame frame = new JFrame();
    // read by the inner UpdateCarOwner_Inner window to fill in the record for the
    // entered ID; per-window, so two update windows cannot clobber each other
    private CarOwner carOwner;

    public CarOwner_Update() {
        frame.setTitle("Update CarOwner");
        frame.setLayout(new AbsoluteLayout());
        frame.setSize(new Dimension(450, 290));
        frame.setResizable(false);
        frame.setLocationRelativeTo(Parent_JFrame.getMainFrame());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Parent_JFrame.getMainFrame().setEnabled(true);
                frame.dispose();
            }
        });

        OK_Button = new JButton("OK");
        Cancel_Button = new JButton("Cancel");

        ID_Label = new JLabel("Enter ID to be Updated");
        IDValidity_Label = new JLabel();
        ID_TextField = new JTextField();

        ID_TextField.setPreferredSize(new Dimension(240, 22));

        ID_Label.setPreferredSize(new Dimension(175, 22));
        IDValidity_Label.setPreferredSize(new Dimension(240, 9));
        IDValidity_Label.setForeground(Color.red);
        frame.add(ID_Label, new AbsoluteConstraints(10, 5));
        frame.add(ID_TextField, new AbsoluteConstraints(195, 5));
        frame.add(IDValidity_Label, new AbsoluteConstraints(195, 30));
        frame.add(OK_Button, new AbsoluteConstraints(100, 225, 100, 22));
        frame.add(Cancel_Button, new AbsoluteConstraints(250, 225, 100, 22));

        OK_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ID = ID_TextField.getText().trim();
                if (!ID_TextField.getText().isEmpty()) {
                    if (CarOwner.isIDvalid(ID)) {
                        carOwner = CarOwner.SearchByID(Integer.parseInt(ID));
                        if (carOwner != null) {
                            Parent_JFrame.getMainFrame().setEnabled(false);
                            frame.dispose();
                            new UpdateCarOwner_Inner().setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(null, "Required ID is not found !");
                        }
                    } else {
                        IDValidity_Label.setText("Invalid ID !");
                    }
                } else {
                    IDValidity_Label.setText("Enter ID !");
                }
            }
        });

        Cancel_Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Parent_JFrame.getMainFrame().setEnabled(true);
                frame.dispose();
            }
        });
    }

    public final class UpdateCarOwner_Inner extends JFrame {

        // Swing windows are Serializable by inheritance and this one is never actually
        // serialized, but pinning the ID keeps the compiler quiet about it.
        private static final long serialVersionUID = 1L;

        JButton Update_Button, Cancel_Button;
        JLabel CNIC_Label, Name_Label, Contact_Label, CNICValidity_Label, contactValidity_Label, NameValidity_Label;
        JTextField CNIC_TextField, Name_TextField, Contact_TextField;

        public UpdateCarOwner_Inner() {
            super("Update CarOwner");
            setLayout(new AbsoluteLayout());
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
//                    the main frame was disabled before this window opened,
//                    so closing it has to hand control back
                    Parent_JFrame.getMainFrame().setEnabled(true);
                    dispose();
                }
            });

            setSize(new Dimension(450, 290));
            setResizable(false);
            setLocationRelativeTo(Parent_JFrame.getMainFrame());
            Update_Button = new JButton("Update");
            Cancel_Button = new JButton("Cancel");

            CNIC_Label = new JLabel("Enter CNIC (without dashes)");
            Name_Label = new JLabel("Enter Name");
            Contact_Label = new JLabel("Enter Contact");
            CNICValidity_Label = new JLabel();
            NameValidity_Label = new JLabel();
            contactValidity_Label = new JLabel();
            CNIC_TextField = new JTextField(carOwner.getCNIC());
            Name_TextField = new JTextField(carOwner.getName());
            Contact_TextField = new JTextField(carOwner.getContact_No());

            CNIC_TextField.setPreferredSize(new Dimension(240, 22));
            Name_TextField.setPreferredSize(new Dimension(240, 22));
            Contact_TextField.setPreferredSize(new Dimension(240, 22));

            CNIC_Label.setPreferredSize(new Dimension(175, 22));
            Name_Label.setPreferredSize(new Dimension(175, 22));
            Contact_Label.setPreferredSize(new Dimension(175, 22));
            CNICValidity_Label.setPreferredSize(new Dimension(240, 9));
            contactValidity_Label.setPreferredSize(new Dimension(240, 9));
            NameValidity_Label.setPreferredSize(new Dimension(240, 9));

            CNICValidity_Label.setForeground(Color.red);
            contactValidity_Label.setForeground(Color.red);
            NameValidity_Label.setForeground(Color.red);

            add(CNIC_Label, new AbsoluteConstraints(10, 5));
            add(CNIC_TextField, new AbsoluteConstraints(195, 5));
            add(CNICValidity_Label, new AbsoluteConstraints(195, 30));
            add(Name_Label, new AbsoluteConstraints(10, 42));
            add(Name_TextField, new AbsoluteConstraints(195, 42));
            add(NameValidity_Label, new AbsoluteConstraints(195, 66));
            add(Contact_Label, new AbsoluteConstraints(10, 77));
            add(Contact_TextField, new AbsoluteConstraints(195, 77));
            add(contactValidity_Label, new AbsoluteConstraints(195, 102));
            add(Update_Button, new AbsoluteConstraints(100, 225, 100, 22));
            add(Cancel_Button, new AbsoluteConstraints(250, 225, 100, 22));

            Update_Button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String cnic = CNIC_TextField.getText().trim();
                    String name = Name_TextField.getText().trim();
                    String contact = Contact_TextField.getText().trim();
                    if (!cnic.isEmpty()) {
                        if (CarOwner.isCNICValid(cnic)) {
                            CarOwner CO = CarOwner.SearchByCNIC(cnic);
//                            a CNIC already on record is only a clash when it belongs
//                            to somebody else, not when it is this owner's own
                            if (CO != null && !cnic.equals(carOwner.getCNIC())) {
                                cnic = null;
                                JOptionPane.showMessageDialog(null, "This CNIC is already registered !");
                            }
                        } else {
                            cnic = null;
                            CNICValidity_Label.setText("Invalid CNIC !");
                        }
                    } else {
                        cnic = null;
                        CNICValidity_Label.setText("Enter CNIC !");
                    }
                    if (!name.isEmpty()) {
                        if (CarOwner.isNameValid(name)) {
                        } else {
                            name = null;
                            NameValidity_Label.setText("Invalid Name !");
                        }
                    } else {
                        name = null;
                        NameValidity_Label.setText("Enter Name !");
                    }
                    if (!contact.isEmpty()) {
                        if (CarOwner.isContactNoValid(contact)) {
                        } else {
                            contact = null;
                            contactValidity_Label.setText("Invalid Contact Number!");
                        }
                    } else {
                        contact = null;
                        contactValidity_Label.setText("Enter Contact Number !");
                    }
                    if (cnic != null && name != null && contact != null) {
                        carOwner = new CarOwner(carOwner.getBalance(), carOwner.getID(), cnic, name, contact);
                        ServiceResult result = RegistryService.updateCarOwner(carOwner);
                        if (!result.isSuccess()) {
                            JOptionPane.showMessageDialog(null, result.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        Parent_JFrame.getMainFrame().getContentPane().removeAll();
                        CarOwner_Details cd = new CarOwner_Details();
                        Parent_JFrame.getMainFrame().add(cd.getMainPanel());
                        Parent_JFrame.getMainFrame().getContentPane().revalidate();
                        Parent_JFrame.getMainFrame().getContentPane().repaint();
                        JOptionPane.showMessageDialog(null, result.getMessage());
                        Parent_JFrame.getMainFrame().setEnabled(true);
                        dispose();
                    }
                }
            });

            Cancel_Button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Parent_JFrame.getMainFrame().setEnabled(true);
                    dispose();
                }
            });
        }

    }

}
