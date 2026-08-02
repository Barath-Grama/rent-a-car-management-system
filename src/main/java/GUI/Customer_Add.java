package GUI;

import BackendCode.Customer;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

public class Customer_Add {

    JButton Add_Button, Cancel_Button;
    JLabel CNIC_Label, Name_Label, Contact_Label, CNICValidity_Label, contactValidity_Label, NameValidity_Label;
    JTextField CNIC_TextField, Name_TextField, Contact_TextField;
    JFrame frame = new JFrame();

    public Customer_Add() {
        frame.setTitle("Add Customer");
        frame.setLayout(new AbsoluteLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Parent_JFrame.getMainFrame().setEnabled(true);
                frame.dispose();
            }
        });

        frame.setSize(new Dimension(450, 290));
        frame.setResizable(false);
        frame.setLocationRelativeTo(Parent_JFrame.getMainFrame());

        Add_Button = new JButton("Add");
        Cancel_Button = new JButton("Cancel");

        CNIC_Label = new JLabel("Enter CNIC (without dashes)");
        Name_Label = new JLabel("Enter Name");
        Contact_Label = new JLabel("Enter Contact");
        CNICValidity_Label = new JLabel();
        NameValidity_Label = new JLabel();
        contactValidity_Label = new JLabel();
        CNIC_TextField = new JTextField();
        Name_TextField = new JTextField();
        Contact_TextField = new JTextField();

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

        frame.add(CNIC_Label, new AbsoluteConstraints(10, 5));
        frame.add(CNIC_TextField, new AbsoluteConstraints(195, 5));
        frame.add(CNICValidity_Label, new AbsoluteConstraints(195, 30));

        frame.add(Name_Label, new AbsoluteConstraints(10, 42));
        frame.add(Name_TextField, new AbsoluteConstraints(195, 42));
        frame.add(NameValidity_Label, new AbsoluteConstraints(195, 66));

        frame.add(Contact_Label, new AbsoluteConstraints(10, 77));
        frame.add(Contact_TextField, new AbsoluteConstraints(195, 77));
        frame.add(contactValidity_Label, new AbsoluteConstraints(195, 102));

        frame.add(Add_Button, new AbsoluteConstraints(100, 225, 100, 22));
        frame.add(Cancel_Button, new AbsoluteConstraints(250, 225, 100, 22));

        Add_Button.addActionListener(new Customer_Add_ActionListener());

        Cancel_Button.addActionListener(new Customer_Add_ActionListener());
    }

    private class Customer_Add_ActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getActionCommand()) {
                case "Add": {
                    String cnic = CNIC_TextField.getText().trim();
                    String name = Name_TextField.getText().trim();
                    String contact = Contact_TextField.getText().trim();

                    if (Customer.isCNICValid(cnic)) {
                        Customer customer = Customer.SearchByCNIC(cnic);
                        if (customer == null) {
                            if (Customer.isNameValid(name)) {
                                if (Customer.isContactNoValid(contact)) {
                                    // ID is Auto
                                    if (!SaveReport.check(new Customer(0, 0, cnic, name, contact).Add())) {
                                        return;
                                    }
                                    Parent_JFrame.getMainFrame().getContentPane().removeAll();
                                    Customer_Details cd = new Customer_Details();
                                    Parent_JFrame.getMainFrame().add(cd.getMainPanel());
                                    Parent_JFrame.getMainFrame().getContentPane().revalidate();
                                    Parent_JFrame.getMainFrame().getContentPane().repaint();
                                    Parent_JFrame.getMainFrame().setEnabled(true);
                                    JOptionPane.showMessageDialog(null, "Customer added successfully !");
                                    frame.dispose();
                                } else {
                                    JOptionPane.showMessageDialog(null, "Invalid contact no. !");
                                }
                            } else {
                                JOptionPane.showMessageDialog(null, "Invalid Name !");
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "This CNIC is already registered !");
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Invalid CNIC");
                    }
                    break;
                }
                case "Cancel": {
                    Parent_JFrame.getMainFrame().setEnabled(true);
                    frame.dispose();
                    break;
                }
            }
        }

    }
}
