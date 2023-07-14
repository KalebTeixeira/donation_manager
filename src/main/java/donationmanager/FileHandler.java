package donationmanager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class FileHandler {
    private final JFileChooser fileChooser = new JFileChooser();

    /**
     * Uses a file chooser dialogue to open a specific file.
     *
     * @param title  The title of the file chooser dialogue.
     * @param filter The extension filter to be used.
     * @return The opened file.
     * @throws RuntimeException If the dialogue is closed by the user.
     */
    public File chooseFileToOpen(String title, FileFilter filter) {
        configureFileChooser(title, filter);

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }

        throw new RuntimeException("No file chosen");
    }

    /**
     * Uses a file chooser dialogue to save a file.
     *
     * @param title  The title of the file chooser dialogue.
     * @param filter The extension filter to be used.
     * @return The file to be saved.
     * @throws RuntimeException If the dialogue is closed by the user.
     */

    public File chooseFileToSave(String title, FileFilter filter) throws RuntimeException {
        configureFileChooser(title, filter);

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        throw new RuntimeException("No file chosen");
    }

    /**
     * Configures the file chooser with the given parameters.
     *
     * @param title  The title of the file chooser dialogue.
     * @param filter The extension filter to be used.
     */
    private void configureFileChooser(String title, FileFilter filter) {
        fileChooser.setDialogTitle(title);
        fileChooser.setFileFilter(new FileNameExtensionFilter(filter.description, filter.extensions));
    }
}
