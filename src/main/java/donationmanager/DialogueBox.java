package donationmanager;

import javax.swing.*;

public class DialogueBox {
    /**
     * Shows a dialogue box asking if two different names belong to the same person
     *
     * @param firstName  The first name to be shown in the dialogue
     * @param secondName The second name to be shown in the dialogue
     * @return The selected option (Can be JOptionPane.YES_OPTION or JOptionPane.NO_OPTION)
     */
    public int showNameComparisonDialogue(String firstName, String secondName) {
        String message = String.format("Is %s = %s?", firstName, secondName);

        return JOptionPane.showConfirmDialog(null, message, "", JOptionPane.YES_NO_OPTION);
    }
}