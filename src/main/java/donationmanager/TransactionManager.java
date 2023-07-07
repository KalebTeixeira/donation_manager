package donationmanager;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import technology.tabula.RectangularTextContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class TransactionManager {
    @NonNull
    private List<List<RectangularTextContainer>> table;
    private final List<Transaction> transactions = new ArrayList<>();

    /**
     * Makes a list of Transactions based on the extracted table.
     *
     * @return A list of Transactions
     */
    public List<Transaction> makeTransactionList() {
        for (int rowNumber = 0; rowNumber < this.table.size(); rowNumber++) {
            List<RectangularTextContainer> row = this.table.get(rowNumber);

            String date = row.get(0).getText();
            String text = row.get(1).getText();

            if (date.isBlank()) {
                continue;
            }

            String dateOnNextRow = getDateOnNextRow(rowNumber);

            if ((text.equals("Postcheckeingang") || text.equals("Belast. E-Banking")) && dateOnNextRow.isBlank()) {
                handleGroupedEntry(rowNumber);
            }

            else {
                Transaction transaction = extractTransaction(EntryType.SINGLE, rowNumber);
                this.transactions.add(transaction);
            }
        }
        return this.transactions;
    }


    /**
     * This method is used for entries in the bank statement that have multiple transactions grouped together.
     *
     * @param startingRowNumber The row number where the grouped entry starts
     */
    private void handleGroupedEntry(int startingRowNumber) {
        if (!canHandleGroupedEntry(startingRowNumber)) {
            return;
        }

        for (int rowNumber = startingRowNumber + 1; rowNumber < this.table.size(); rowNumber++) {

            String dateOnNextRow = getDateOnNextRow(rowNumber);
            if (!dateOnNextRow.isBlank()) {
                return;
            }

            String text = this.table.get(rowNumber).get(1).getText();
            if (isRowStartOfNewTransaction(text)) {
                Transaction transaction = extractTransaction(EntryType.GROUPED, rowNumber);
                this.transactions.add(transaction);
            }
        }
    }

    /**
     * Checks if the grouped entry can be handled by this program. Check the text fields in the entries for currency
     * amount patterns and sees if they, added together match the total. If not, this method returns false, the program
     * skips this entry and the user has to manually add it instead.
     *
     * @param startingRowNumber The row number where the grouped entry starts
     * @return A boolean saying if the program can handle this grouped entry
     * @throws RuntimeException if no total donated amount is found
     */
    private boolean canHandleGroupedEntry(int startingRowNumber) {
        double totalAmount;
        List<RectangularTextContainer> startingRow = this.table.get(startingRowNumber);
        String debitField = startingRow.get(3).getText();
        String creditField = startingRow.get(4).getText();
        if (!debitField.isBlank()) {
            totalAmount = Double.parseDouble(debitField.replaceAll("'", ""));
        }
        else if (!creditField.isBlank()) {
            totalAmount = Double.parseDouble(creditField.replaceAll("'", ""));
        }
        else {
            throw new RuntimeException("No total amount was found in grouped entry.");
        }

        double checkTotal = 0;
        for (int rowNumber = startingRowNumber + 1; rowNumber < this.table.size(); rowNumber++) {

            String dateOnNextRow = getDateOnNextRow(rowNumber);
            if (!dateOnNextRow.isBlank()) {
                return totalAmount == checkTotal;
            }

            String text = this.table.get(rowNumber).get(1).getText();
            double amount =
                    !extractAmountFromText(text).isBlank() ? Double.parseDouble(extractAmountFromText(text)) : 0;

            checkTotal += amount;
        }

        return totalAmount == checkTotal;
    }

    /**
     * Checks if the current row is the start of a new transaction in a grouped entry. It does this by looking if the
     * text field contains a currency amount pattern. (Because in grouped entries, the amount donated is included in the
     * text field instead of being separate.)
     *
     * @param text The String in the text field
     * @return a boolean saying if the current row is the start of a new transaction or not
     */
    private boolean isRowStartOfNewTransaction(String text) {
        return !extractAmountFromText(text).isBlank();
    }

    /**
     * Extracts a transaction from the table.
     *
     * @param entryType The type of entry the transaction is being extracted from. Can be either a regular, SINGLE
     *                  entry, or a GROUPED entry.
     * @param rowNumber The starting row of the entry to extract from.
     * @return A new Transaction containing all of its extracted information
     */
    private Transaction extractTransaction(EntryType entryType, int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);

        String name = row.get(1).getText();
        name = name.replace("Gutschrift ", "");
        if (entryType == EntryType.GROUPED) {
            String amount = extractAmountFromText(name);
            name = name.replace(amount, "");
        }

        String amount = extractAmount(entryType, rowNumber);
        List<String> miscText = extractExtraText(entryType, rowNumber);

        // For Grouped entries, the rest of the info (date and transaction type) can be read from
        // its starting row.
        if (entryType == EntryType.GROUPED) {
            rowNumber = getStartingRowNumber(rowNumber);
        }

        TransactionType transactionType = extractTransactionType(rowNumber);
        String date = table.get(rowNumber).get(0).getText();

        return new Transaction(name, amount, transactionType, date, miscText);
    }

    /**
     * Gets the index of the starting row of the current grouped entry.
     *
     * @param rowNumber The row number of the current Transaction in a grouped entry.
     * @return The index of the starting row of the current grouped entry.
     * @throws RuntimeException if no starting row is found
     */
    private int getStartingRowNumber(int rowNumber) {
        for (int i = rowNumber; i >= 0; i--) {
            List<RectangularTextContainer> row = this.table.get(i);
            RectangularTextContainer dateField = row.get(0);

            if (!dateField.getText().isBlank()) {
                return i;
            }
        }
        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer nameField = row.get(1);
        throw new RuntimeException(String.format("No starting row was found at the grouped entry with: %s",
                                                 nameField.getText()));
    }

    /**
     * Extracts the donated amount of the current entry from the table.
     *
     * @param entryType The type of the entry the transaction is being extracted from. Can be either a regular, SINGLE
     *                  entry, or a GROUPED entry.
     * @param rowNumber The row number of the current entry.
     * @return The extracted amount
     */
    private String extractAmount(EntryType entryType, int rowNumber) {
        String result;
        switch (entryType) {
            case SINGLE:
                result = extractSingleEntryAmount(rowNumber);
                break;
            case GROUPED:
                result = extractGroupedEntryAmount(rowNumber);
                break;
            default:
                throw new RuntimeException(String.format("No valid entry type for: %s",
                                                         table.get(rowNumber).get(1).getText()));
        }
        return result.replaceAll("'", "");
    }

    /**
     * Extracts the donated amount from a regular entry.
     *
     * @param rowNumber The row number of the current entry.
     * @return The extracted amount
     * @throws RuntimeException If not amount is found.
     */
    private String extractSingleEntryAmount(int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer dateField = row.get(0);
        RectangularTextContainer nameField = row.get(1);
        RectangularTextContainer debitField = row.get(3);
        RectangularTextContainer creditField = row.get(4);

        if (!debitField.getText().isBlank()) {
            return debitField.getText();
        }
        else if (!creditField.getText().isBlank()) {
            return creditField.getText();
        }
        else {
            throw new RuntimeException(String.format("No amount was found for entry: %s on " + "%s",
                                                     nameField.getText(),
                                                     dateField.getText()));
        }
    }

    /**
     * Extracts the donated amount for a transaction in a grouped entry.
     *
     * @param rowNumber The row number of the current transaction.
     * @return The extracted amount
     * @throws RuntimeException If not amount is found.
     */
    private String extractGroupedEntryAmount(int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer dateField = row.get(0);
        RectangularTextContainer nameField = row.get(1);

        String amount = extractAmountFromText(nameField.getText()).trim();

        if (amount.isBlank()) {
            throw new RuntimeException(String.format("No amount was found for Transaction: %s on " + "%s",
                                                     nameField.getText(),
                                                     dateField.getText()));
        }

        return amount;
    }

    /**
     * Extracts the extra text provided besides the name the table.
     *
     * @param entryType The type of the entry the transaction is being extracted from. Can be either a regular, SINGLE
     *                  entry, or a GROUPED entry.
     * @param rowNumber The row number of the current entry.
     * @return The extracted text.
     */
    private List<String> extractExtraText(EntryType entryType, int rowNumber) {
        List<String> miscText = new ArrayList<>();

        for (int i = rowNumber + 1; i < this.table.size(); i++) {
            List<RectangularTextContainer> row = this.table.get(i);
            RectangularTextContainer textField = row.get(1);
            RectangularTextContainer dateField = row.get(0);

            boolean continueCondition = false;
            if (entryType == EntryType.SINGLE) {
                continueCondition = dateField.getText().isBlank();
            }
            else if (entryType == EntryType.GROUPED) {
                continueCondition = dateField.getText().isBlank() && !isRowStartOfNewTransaction(textField.getText());
            }

            if (!continueCondition) {
                break;
            }

            miscText.add(textField.getText());
        }
        return miscText;
    }

    /**
     * Extracts the transaction type from the current entry.
     *
     * @param rowNumber The row number of the current entry.
     * @return The transaction Type. Can be DEBIT or CREDIT.
     */
    private TransactionType extractTransactionType(int rowNumber) {
        List<RectangularTextContainer> row = this.table.get(rowNumber);
        RectangularTextContainer dateField = row.get(0);
        RectangularTextContainer nameField = row.get(1);
        RectangularTextContainer debitField = row.get(3);
        RectangularTextContainer creditField = row.get(4);

        if (!debitField.getText().isBlank()) {
            return TransactionType.DEBIT;
        }
        else if (!creditField.getText().isBlank()) {
            return TransactionType.CREDIT;
        }
        else {
            throw new RuntimeException(String.format("No transaction type was found for: %s on " + "%s",
                                                     nameField.getText(),
                                                     dateField.getText()));
        }
    }

    /**
     * Get the contents of the Date field in the next row.
     *
     * @param rowNumber The row number of the current entry.
     * @return The contents of the Date field in the next row. Empty if the current row is the last row.
     */
    private String getDateOnNextRow(int rowNumber) {
        // If it's the last row
        if (rowNumber + 1 == this.table.size()) {
            return "";
        }
        return this.table.get(rowNumber + 1).get(0).getText();
    }

    /**
     * Uses regex to extract a currency amount from a string. The amount can have the format (1'111.11)
     *
     * @param text The String to be searched for a currency amount.
     * @return The amount, if any is found, stripped of the digit group separator (') to prevent further issues.
     */
    private String extractAmountFromText(String text) {
        Pattern pattern = Pattern.compile("(^|\\s)(\\d|')+\\.\\d\\d($|\\s)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group().replaceAll("'", "");
        }
        else {
            return "";
        }
    }

    /**
     * A testing method to print the list of transactions.
     */
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



