package donationmanager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class FileHandler {
    private final JFileChooser fileChooser = new JFileChooser("..");

    /**
     * @return
     * @throws IOException
     */
    public File openFile() throws IOException {
        configureFileChooser("Choose the file with all donors",
                             "Excel files",
                             "xlsx",
                             "xlsm",
                             "xlsb",
                             "xls");

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }

        throw new IOException("No file chosen");
    }

    public File saveFile() throws IOException {
        configureFileChooser("Choose Where to save the output",
                             "Excel files",
                             "xlsx",
                             "xlsm",
                             "xlsb",
                             "xls");

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        throw new IOException("No file chosen");
    }

    private void configureFileChooser(String title, String... filter) {
        fileChooser.setDialogTitle(title);

        String filterDescription = filter[0];
        String[] filterExtensions = Arrays.stream(filter, 1, filter.length).toArray(String[]::new);
        fileChooser.setFileFilter(new FileNameExtensionFilter(filterDescription, filterExtensions));
    }
}
