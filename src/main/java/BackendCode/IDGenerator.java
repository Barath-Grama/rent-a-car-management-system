package BackendCode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Hands out record IDs that are never handed out twice.
 * <p>
 * Deriving the next ID from the records that happen to be in a .ser file right now
 * is not enough: deleting the newest record lowers the highest ID in the file, so
 * the next record added takes the ID the deleted one had. A retired ID coming back
 * matters because Booking.ser stores its Customer and Car by ID -- an old booking
 * would silently re-bind to whichever new record inherited the number.
 * <p>
 * So the last ID issued is kept in its own small text file that Remove() never
 * touches. When that file is missing -- an existing install, or the .ser files that
 * ship with the project -- it falls back to the highest ID currently on record, which
 * seeds the counter correctly from whatever data is already there.
 */
class IDGenerator {

    /**
     * @param counterFileName file holding the last ID issued for this record type
     * @param highestExistingID highest ID among the records currently stored
     * @return the next unused ID, already recorded as issued
     */
    static int next(String counterFileName, int highestExistingID) {
        File file = new File(counterFileName);
        int lastIssued = highestExistingID;

        if (file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                if (line != null) {
                    int stored = Integer.parseInt(line.trim());
                    if (stored > lastIssued) {
                        lastIssued = stored;
                    }
                }
            } catch (IOException ex) {
                System.out.println(ex);
            } catch (NumberFormatException ex) {
//                unreadable counter, fall back to the highest ID on record
                System.out.println(ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
            }
        }

        int nextID = lastIssued + 1;

        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(nextID + "");
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        }
        return nextID;
    }
}
