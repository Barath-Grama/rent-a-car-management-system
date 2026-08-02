package GUI;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author @AbdullahShahid01
 */
public class Runner {

    private static final JFrame FRAME = new JFrame();
    private final ImageIcon icon;
    private final JLabel L1;

    public static JFrame getFrame() {
        return FRAME;
    }

    public Runner() {
        
        icon = Images.load("WelcomeImage.jpg");
        L1 = new JLabel(icon);
        FRAME.setUndecorated(true);
        FRAME.setSize(new Dimension(1000, 534));
        FRAME.setLocationRelativeTo(null);
        FRAME.add(L1);
    }

    /**
     * Clears whatever the shared frame is showing and puts a fresh Login panel on
     * it. FRAME is a single static instance that gets disposed once the user logs
     * in, so logging back out has to reuse and clear it -- building another Runner
     * and adding to it just stacks a second panel on top of the stale one.
     */
    public static void showLogin() {
        FRAME.getContentPane().removeAll();
        FRAME.add(new Login().getMainPanel());
        FRAME.getContentPane().revalidate();
        FRAME.getContentPane().repaint();
        FRAME.getContentPane().repaint();
        FRAME.setVisible(true);
    }

    public static void main(String[] args) {
//        Swing components may only be built and touched on the event dispatch thread.
//        Everything after this runs from event handlers, which are already on it.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Runner();
                FRAME.setVisible(true);

//                Hold the welcome image for a second, then swap in the login form.
//                A Thread.sleep here would freeze the event thread and leave the
//                splash unpainted, so a Swing timer does the waiting instead.
                Timer splashTimer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showLogin();
                    }
                });
                splashTimer.setRepeats(false);
                splashTimer.start();
            }
        });
    }
}
