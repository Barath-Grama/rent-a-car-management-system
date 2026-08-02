package GUI;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sorting, filtering and export, shared by the four list screens.
 * <p>
 * Searching used to mean typing into one of two boxes, pressing a button, and
 * reading the result out of a message dialog that had a {@code toString()} dumped
 * into it -- while the table you were looking at did not move. Typing in the filter
 * box now narrows the table itself as you type, and any column header sorts.
 *
 * @author @AbdullahShahid01
 */
final class TableTools {

    private static final Logger LOG = LoggerFactory.getLogger(TableTools.class);

    private TableTools() {
    }

    /**
     * Makes the table sortable by clicking a header, and narrows it as the user types
     * in the filter box. Matching is case-insensitive across every column.
     *
     * @param table  the table to control
     * @param filter the box to listen to
     */
    static void attach(JTable table, JTextField filter) {
        final TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apply();
            }

            private void apply() {
                String text = filter.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                    return;
                }
                try {
//                    (?i) so "civic" finds "Civic"; Pattern.quote so a half-typed
//                    regex character is treated as text rather than blowing up
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                } catch (java.util.regex.PatternSyntaxException ex) {
                    sorter.setRowFilter(null);
                }
            }
        });
    }

    /**
     * Writes what the user is currently looking at to a CSV file: the rows the filter
     * has left, in the order the table is sorted, not whatever the model happens to
     * hold underneath.
     *
     * @param parent the window to hang the file chooser off
     * @param table  the table to export
     * @param suggestedName default file name
     */
    static void exportCsv(Component parent, JTable table, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(suggestedName));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();

        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(target), StandardCharsets.UTF_8))) {

            for (int column = 0; column < table.getColumnCount(); column++) {
                out.write(quote(table.getColumnName(column)));
                out.write(column == table.getColumnCount() - 1 ? "\n" : ",");
            }
//            getRowCount and getValueAt on the table, not the model, so the filter
//            and the sort order are what gets written
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int column = 0; column < table.getColumnCount(); column++) {
                    Object value = table.getValueAt(row, column);
                    out.write(quote(value == null ? "" : value.toString()));
                    out.write(column == table.getColumnCount() - 1 ? "\n" : ",");
                }
            }
            JOptionPane.showMessageDialog(parent,
                    table.getRowCount() + " rows written to\n" + target.getAbsolutePath());
        } catch (IOException ex) {
            LOG.error("could not write the CSV export", ex);
            JOptionPane.showMessageDialog(parent, "Could not write the file :\n" + ex.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Quotes a CSV field. Values here can hold commas and newlines -- the "cars given
     * for rent" column puts each car on its own line -- so this is not optional.
     */
    private static String quote(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
