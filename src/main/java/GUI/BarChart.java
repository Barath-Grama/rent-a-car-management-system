package GUI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;
import javax.swing.JPanel;

/**
 * A plain bar chart, drawn directly with Java2D.
 * <p>
 * Two bar charts did not seem worth a two megabyte charting library and its LGPL
 * terms in a jar somebody might redistribute, so this draws them: axis, bars, value
 * labels and a message when there is nothing to show. Anything more involved --
 * zooming, legends, time axes -- and a real library would earn its place.
 *
 * @author @AbdullahShahid01
 */
final class BarChart extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BAR = new Color(0x2F, 0x6F, 0xB0);
    private static final Color BAR_TOP = new Color(0x4A, 0x93, 0xD9);
    private static final Color AXIS = new Color(0x88, 0x88, 0x88);
    private static final Color TEXT = new Color(0x33, 0x33, 0x33);

    private final String title;
    private final Map<String, Integer> data;
    private final String valueSuffix;

    /**
     * @param title       heading drawn above the plot
     * @param data        bar label to value, drawn in iteration order
     * @param valueSuffix appended to each value label, e.g. " PKR"
     */
    BarChart(String title, Map<String, Integer> data, String valueSuffix) {
        this.title = title;
        this.data = data;
        this.valueSuffix = valueSuffix;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        g.setColor(new Color(0xDD, 0xDD, 0xDD));
        g.drawRect(0, 0, width - 1, height - 1);

        g.setColor(TEXT);
        g.setFont(new Font("Tahoma", Font.BOLD, 14));
        g.drawString(title, 14, 24);

        int left = 14;
        int right = width - 14;
        int top = 42;
        int bottom = height - 34;

        if (data.isEmpty()) {
            g.setFont(new Font("Tahoma", Font.PLAIN, 12));
            g.setColor(AXIS);
            String none = "Nothing to show yet";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(none, (width - fm.stringWidth(none)) / 2, height / 2);
            g.dispose();
            return;
        }

        int max = 1;
        for (Integer value : data.values()) {
            max = Math.max(max, value == null ? 0 : value);
        }

        g.setColor(AXIS);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(left, bottom, right, bottom);

        int count = data.size();
        int slot = (right - left) / count;
        int barWidth = Math.max(8, Math.min(70, slot - 16));
        g.setFont(new Font("Tahoma", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();

        int index = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int value = entry.getValue() == null ? 0 : entry.getValue();
            int barHeight = (int) Math.round((double) (bottom - top) * value / max);
            int x = left + index * slot + (slot - barWidth) / 2;
            int y = bottom - barHeight;

            g.setColor(BAR);
            g.fillRect(x, y, barWidth, barHeight);
            g.setColor(BAR_TOP);
            g.fillRect(x, y, barWidth, Math.min(4, barHeight));

            g.setColor(TEXT);
            String valueLabel = value + valueSuffix;
            g.drawString(valueLabel, x + (barWidth - fm.stringWidth(valueLabel)) / 2, y - 4);

            String label = shorten(entry.getKey(), slot - 6, fm);
            g.setColor(AXIS);
            g.drawString(label, x + (barWidth - fm.stringWidth(label)) / 2, bottom + 16);

            index++;
        }
        g.dispose();
    }

    /** Trims a label with an ellipsis so neighbouring bars do not run into each other. */
    private static String shorten(String label, int available, FontMetrics fm) {
        if (fm.stringWidth(label) <= available) {
            return label;
        }
        String trimmed = label;
        while (trimmed.length() > 1 && fm.stringWidth(trimmed + "...") > available) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "...";
    }
}
