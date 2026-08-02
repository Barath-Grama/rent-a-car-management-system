package GUI;

import BackendCode.service.UserService;
import BackendCode.dao.ReportingDao;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

/**
 * A read-only summary of the business: what has been earned, how much of the fleet is
 * working, and which cars and customers account for it.
 * <p>
 * Every figure comes from an aggregate query rather than from walking the tables in
 * Java, so adding rows does not make this screen slower.
 *
 * @author @Barath-Grama
 */
public final class Dashboard implements ActionListener {

    private static final Color INK = new Color(0x22, 0x22, 0x22);
    private static final Color MUTED = new Color(0x77, 0x77, 0x77);
    private static final Color CARD = new Color(0xFA, 0xFA, 0xFA);

    private JPanel MainPanel;
    private JButton Back_Button, Logout_Button, Refresh_Button;

    public Dashboard() {
        MainPanel = new JPanel();
        Parent_JFrame.getMainFrame().setTitle("Dashboard - Rent-A-Car Management System");
        MainPanel.setLayout(new AbsoluteLayout());
        MainPanel.setMinimumSize(new Dimension(1366, 730));
        MainPanel.setBackground(Color.WHITE);

        JLabel heading = new JLabel("Business Overview");
        heading.setFont(new Font("Tahoma", Font.BOLD, 20));
        heading.setForeground(INK);
        MainPanel.add(heading, new AbsoluteConstraints(14, 10, 400, 30));

        // ---- the tiles across the top ----
        int totalCars = ReportingDao.totalCars();
        int carsOut = ReportingDao.carsOut();
        addTile(0, "Total revenue", format(ReportingDao.totalRevenue()) + " PKR");
        addTile(1, "Completed rentals", String.valueOf(ReportingDao.completedRentals()));
        addTile(2, "Cars out now", carsOut + " of " + totalCars);
        addTile(3, "Fleet utilisation", ReportingDao.utilisationPercent() + "%");
        addTile(4, "Customers", String.valueOf(ReportingDao.totalCustomers()));
        addTile(5, "Outstanding bills", format(ReportingDao.unpaidBills()) + " PKR");

        // ---- charts ----
        BarChart revenue = new BarChart("Revenue by month", ReportingDao.revenueByMonth(), "");
        MainPanel.add(revenue, new AbsoluteConstraints(14, 160, 660, 240));

        BarChart topCars = new BarChart("Top earning cars", ReportingDao.topCarsByRevenue(5), "");
        MainPanel.add(topCars, new AbsoluteConstraints(690, 160, 660, 240));

        BarChart topCustomers = new BarChart("Top customers by spend",
                ReportingDao.topCustomersBySpend(5), "");
        MainPanel.add(topCustomers, new AbsoluteConstraints(14, 415, 660, 200));

        BarChart fleet = new BarChart("Cars on the fleet, by owner", ReportingDao.fleetByOwner(), "");
        MainPanel.add(fleet, new AbsoluteConstraints(690, 415, 660, 200));

        Refresh_Button = new JButton("Refresh");
        Back_Button = new JButton("Back");
        Logout_Button = new JButton("Logout");
        MainPanel.add(Refresh_Button, new AbsoluteConstraints(14, 630, 130, 22));
        MainPanel.add(Back_Button, new AbsoluteConstraints(1106, 630, 100, 22));
        MainPanel.add(Logout_Button, new AbsoluteConstraints(1236, 630, 100, 22));

        Refresh_Button.addActionListener(this);
        Back_Button.addActionListener(this);
        Logout_Button.addActionListener(this);
    }

    /** One headline figure with its caption, laid out in a row of six. */
    private void addTile(int slot, String caption, String value) {
        JPanel tile = new JPanel();
        tile.setLayout(new AbsoluteLayout());
        tile.setBackground(CARD);
        tile.setBorder(BorderFactory.createLineBorder(new Color(0xDD, 0xDD, 0xDD)));

        JLabel captionLabel = new JLabel(caption, SwingConstants.CENTER);
        captionLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));
        captionLabel.setForeground(MUTED);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Tahoma", Font.BOLD, 19));
        valueLabel.setForeground(INK);

        tile.add(captionLabel, new AbsoluteConstraints(0, 12, 216, 18));
        tile.add(valueLabel, new AbsoluteConstraints(0, 36, 216, 26));
        MainPanel.add(tile, new AbsoluteConstraints(14 + slot * 224, 52, 216, 90));
    }

    /** Groups thousands so a large figure can be read at a glance. */
    private static String format(int amount) {
        return String.format("%,d", amount);
    }

    public JPanel getMainPanel() {
        return MainPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "Refresh": {
//                every figure is read in the constructor, so a rebuild is the refresh
                show(new Dashboard().getMainPanel());
            }
            break;
            case "Back": {
                Parent_JFrame.getMainFrame().setTitle("Rent-A-Car Management System [REBORN]");
                show(new MainMenu().getMainPanel());
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

    private static void show(JPanel panel) {
        Parent_JFrame.getMainFrame().getContentPane().removeAll();
        Parent_JFrame.getMainFrame().add(panel);
        Parent_JFrame.getMainFrame().getContentPane().revalidate();
        Parent_JFrame.getMainFrame().getContentPane().repaint();
    }
}
