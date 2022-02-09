package donationmanager;

import lombok.AllArgsConstructor;
import technology.tabula.RectangularTextContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class TransactionManager {
    private List<List<RectangularTextContainer>> table;
    private List<Transaction> transactions = new ArrayList<>();

    public List<Transaction> makeTransactionList() {
        for (int rowNumber = 0; rowNumber < this.table.size(); rowNumber++) {
            List<RectangularTextContainer> row = this.table.get(rowNumber);

            String date = row.get(0).getText();
            String text = row.get(1).getText();
            String date2 = row.get(2).getText();
            String debit = row.get(3).getText();
            String credit = row.get(4).getText();
            String balance = row.get(5).getText();

            if (date.isEmpty()) {
                continue;
            }

            String dateOnNextRow = this.table.get(rowNumber + 1).get(0).getText();
            if (text.equals("Postcheckeingang") && dateOnNextRow.isEmpty()) {
                handleMultipleEntry(rowNumber);
            }

            else {
                addTransactionToList(EntryType.SINGLE, rowNumber);
            }
        }
        return null;
    }

    private void handleMultipleEntry(int startingRowNumber) {
        for (int rowNumber = startingRowNumber + 1; rowNumber < this.table.size(); rowNumber++) {

            String dateOnNextRow = this.table.get(rowNumber + 1).get(0).getText();
            if (!dateOnNextRow.isEmpty()) {
                return;
            }

            String text = this.table.get(rowNumber).get(1).getText();
            if (isRowStartOfNewEntry(text)) {
                addTransactionToList(EntryType.MULTIPLE, rowNumber);
            }
        }
    }

    private boolean isRowStartOfNewEntry(String text) {
        return Pattern.matches("(^|\\s)(\\d|')+\\.\\d\\d($|\\s)", text);
    }

    private void addTransactionToList(EntryType entryType, int rowNumber) {
        Transaction transaction = extractTransaction(EntryType.SINGLE, rowNumber);
        transactions.add(transaction);
    }

    private Transaction extractTransaction(EntryType entryType, int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);

        String dirtyName = row.get(1).getText();
        String name = dirtyName.replace("Gutschrift ", "");

        String amount = extractAmount(entryType, rowNumber);
        List<String> miscText = extractMiscText(entryType, rowNumber);

        if (entryType == EntryType.MULTIPLE) {
            rowNumber = getStartingRowNumber(rowNumber);
        }

        TransactionType transactionType = extractTransactionType(rowNumber);
        String date = row.get(0).getText();

        Transaction transaction = new Transaction(name, amount, transactionType, date, miscText);
    }

    private int getStartingRowNumber(int rowNumber) {
        for (int i = rowNumber; i >= 0; i--) {
            List<RectangularTextContainer> row = this.table.get(i);
            RectangularTextContainer dateField = row.get(0);

            if (!dateField.getText().isEmpty()) {
                return i;
            }
        }

        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer nameField = row.get(1);
        throw new RuntimeException(String.format(
                "No starting row was found at the multiple entry with: %s", nameField.getText()));
    }

    private String extractAmount(EntryType entryType, int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer dateField = row.get(0);

        if (entryType == EntryType.SINGLE) {
            return extractSingleEntryAmount(rowNumber);
        }
        else {
            return extractMultipleEntryAmount(rowNumber);
        }
    }

    private String extractSingleEntryAmount(int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer dateField = row.get(0);
        RectangularTextContainer nameField = row.get(1);
        RectangularTextContainer debitField = row.get(3);
        RectangularTextContainer creditField = row.get(4);

        if (!debitField.getText().isEmpty()) {
            return debitField.getText();
        }
        else if (!creditField.getText().isEmpty()) {
            return creditField.getText();
        }
        else {
            throw new RuntimeException(String.format("No amount was found for Transaction: %s on %s",
                                                     nameField.getText(), dateField.getText()));
        }
    }

    private String extractMultipleEntryAmount(int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer dateField = row.get(0);
        RectangularTextContainer nameField = row.get(1);

        Pattern pattern = Pattern.compile("(^|\\s)(\\d|')+\\.\\d\\d($|\\s)");
        Matcher matcher = pattern.matcher(nameField.getText());
        String amount = matcher.group();

        if (amount.isEmpty()) {
            throw new RuntimeException(String.format("No amount was found for Transaction: %s on %s",
                                                     nameField.getText(), dateField.getText()));
        }
        return amount;
    }

    private List<String> extractMiscText(EntryType entryType, int rowNumber) {
        if (entryType == EntryType.MULTIPLE) {
            return extractMultipleEntryText();
        }
        else {
            return extractSingleEntryText();
        }
    }

    private List<String> extractSingleEntryText() {
    }

    private List<String> extractMultipleEntryText() {
        List<String> miscText = new ArrayList<>();

        for (int i = rowNumber + 1; i < this.table.size(); i++) {
            List<RectangularTextContainer> row = this.table.get(i);
            RectangularTextContainer textField = row.get(1);

            List<RectangularTextContainer> nextRow = this.table.get(i + 1);
            RectangularTextContainer nextTextField = nextRow.get(1);

            miscText.add(textField.getText());

            boolean doesNextRowHaveDate = !nextRow.get(0).getText().isEmpty();
            if (isRowStartOfNewEntry(nextTextField.getText()) || doesNextRowHaveDate) {
                return miscText;
            }
        }
    }

    private TransactionType extractTransactionType(int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer dateField = row.get(0);
        RectangularTextContainer nameField = row.get(1);
        RectangularTextContainer debitField = row.get(3);
        RectangularTextContainer creditField = row.get(4);

        if (!debitField.getText().isEmpty()) {
            return TransactionType.DEBIT;
        }
        else if (!creditField.getText().isEmpty()) {
            return TransactionType.CREDIT;
        }
        else {
            throw new RuntimeException(String.format("No transaction amount was found for: %s on %s",
                                                     nameField.getText(), dateField.getText()));
        }
    }
}



