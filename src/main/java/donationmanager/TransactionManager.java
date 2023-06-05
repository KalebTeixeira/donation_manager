package donationmanager;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import technology.tabula.RectangularTextContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("rawtypes")
@RequiredArgsConstructor
public class TransactionManager {
    @NonNull
    private List<List<RectangularTextContainer>> table;
    private final List<Transaction> transactions = new ArrayList<>();

    public List<Transaction> makeTransactionList() {
        for (int rowNumber = 0; rowNumber < this.table.size(); rowNumber++) {
            List<RectangularTextContainer> row = this.table.get(rowNumber);

            String date = row.get(0).getText();
            String text = row.get(1).getText();

            if (date.isEmpty()) {
                continue;
            }

            String dateOnNextRow = getDateOnNextRow(rowNumber);

            if ((text.equals("Postcheckeingang") ||
                 text.equals("Belast. E-Banking")) && dateOnNextRow.isEmpty()) {
                handleMultipleEntry(rowNumber);
            }

            else {
                addTransactionToList(EntryType.SINGLE, rowNumber);
            }
        }
        return this.transactions;
    }

    private void handleMultipleEntry(int startingRowNumber) {
        for (int rowNumber = startingRowNumber + 1; rowNumber < this.table.size(); rowNumber++) {

            String dateOnNextRow = getDateOnNextRow(rowNumber);
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
        return !extractAmountFromText(text).isEmpty();
    }

    private void addTransactionToList(EntryType entryType, int rowNumber) {
        Transaction transaction = extractTransaction(entryType, rowNumber);
        transactions.add(transaction);
    }

    private Transaction extractTransaction(EntryType entryType, int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);

        String name = row.get(1).getText();
        name = name.replace("Gutschrift ", "");
        if (entryType == EntryType.MULTIPLE) {
            String amount = extractAmountFromText(name);
            name = name.replace(amount, "");
        }

        String amount = extractAmount(entryType, rowNumber);
        List<String> miscText = extractMiscText(entryType, rowNumber);

        if (entryType == EntryType.MULTIPLE) {
            rowNumber = getStartingRowNumber(rowNumber);
        }

        TransactionType transactionType = extractTransactionType(rowNumber);
        String date = table.get(rowNumber).get(0).getText();

        return new Transaction(name, amount, transactionType, date, miscText);
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

        String amount = extractAmountFromText(nameField.getText()).trim();

        if (amount.isEmpty()) {
            throw new RuntimeException(String.format("No amount was found for Transaction: %s on %s",
                                                     nameField.getText(), dateField.getText()));
        }

        return amount;
    }

    private List<String> extractMiscText(EntryType entryType, int rowNumber) {
        List<String> miscText = new ArrayList<>();

        for (int i = rowNumber + 1; i < this.table.size(); i++) {
            List<RectangularTextContainer> row = this.table.get(i);
            RectangularTextContainer textField = row.get(1);
            RectangularTextContainer dateField = row.get(0);

            boolean continueCondition = false;
            if (entryType == EntryType.SINGLE) {
                continueCondition = dateField.getText().isEmpty();
            }
            else if (entryType == EntryType.MULTIPLE) {
                continueCondition = dateField.getText().isEmpty() && !isRowStartOfNewEntry(
                        textField.getText());
            }

            if (!continueCondition) {
                break;
            }

            miscText.add(textField.getText());
        }
        return miscText;
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

    private String getDateOnNextRow(int rowNumber) {
        String dateOnNextRow = "";
        if (rowNumber + 1 < this.table.size()) {
            dateOnNextRow = this.table.get(rowNumber + 1).get(0).getText();
        }
        return dateOnNextRow;
    }

    private String extractAmountFromText(String text) {
        Pattern pattern = Pattern.compile("(^|\\s)(\\d|')+\\.\\d\\d($|\\s)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        else {
            return "";
        }
    }

    @SuppressWarnings("unused")
    public void printTransactions() {
        for (Transaction transaction : transactions) {
            System.out.printf("Name: %s%n", transaction.getName());
            System.out.printf("Amount: %s%n", transaction.getAmount());
            System.out.printf("Type: %s%n", transaction.getTransactionType());
            System.out.printf("Date: %s%n", transaction.getDate());
            System.out.println("Text: ");
            for (String line : transaction.getMiscText()) {
                System.out.printf("      %s%n", line);
            }
            System.out.printf("Category: %s%n", transaction.getDonationCategory());
            System.out.println("--------------------------");
        }
    }
}



