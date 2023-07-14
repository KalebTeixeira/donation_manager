package donationmanager;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DonationListMaker {
    private final Sheet allDonorsSheet;
    private final List<Transaction> transactions;
    private final File outputFile;
    private final Workbook outputExcel = new XSSFWorkbook();
    private final Sheet outputSheet;
    private final DialogueBox dialogueBox;

    public DonationListMaker(File allDonorsFile,
                             List<Transaction> transactions,
                             File outputFile,
                             DialogueBox dialogueBox) {

        this.transactions = transactions;
        this.outputFile = outputFile;
        this.dialogueBox = dialogueBox;

        // Check if all donors file is an Excel file
        Workbook allDonorsExcel;
        try {
            allDonorsExcel = new XSSFWorkbook(new FileInputStream(allDonorsFile));
        }
        catch (IOException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }

        this.allDonorsSheet = allDonorsExcel.getSheet("ACTIVE DONORS  2022") != null ? allDonorsExcel.getSheet(
                "ACTIVE DONORS  2022") : allDonorsExcel.getSheetAt(0);

        Row firstRow = allDonorsSheet.getRow(0);
        Cell firstCell;

        // Validate all donors file
        if (firstRow == null || (firstCell = firstRow.getCell(0)) == null ||
            firstCell.getCellType() != CellType.STRING || !firstCell.getStringCellValue().equals("Code /")) {

            throw new IllegalArgumentException("The Excel file with all known donors is not valid");
        }

        this.outputSheet = outputExcel.createSheet();

        if (!FilenameUtils.isExtension(outputFile.getName(), FileFilter.EXCEL.extensions)) {
            throw new IllegalArgumentException(
                    "The output file does not have the correct file format (must be an Excel file)");
        }
    }

    /**
     * Fills a newly created Excel output file the current donors.
     *
     * @throws IOException If the Excel file with all known donors can't be opened.
     */
    public void makeDonationList() throws IOException {
        // Copy over the header row
        insertRow(allDonorsSheet, outputSheet, 0, 0);

        for (int i = 0; i < this.transactions.size(); i++) {
            Transaction transaction = this.transactions.get(i);

            if (transaction.getTransactionType() == TransactionType.DEBIT) {
                Row outputRow = outputSheet.createRow(i + 1);
                insertDebitRow(transaction, outputRow);
                continue;
            }

            Optional<Integer> matchRow = findNameMatch(transaction, allDonorsSheet);
            if (matchRow.isPresent()) {
                insertRow(allDonorsSheet, outputSheet, matchRow.get(), i + 1);
                replaceAmount(outputSheet.getRow(i + 1), transaction.getAmount(), transaction.getTransactionType());
            }
        }

        FileOutputStream outputStream = new FileOutputStream(this.outputFile);
        outputExcel.write(outputStream);
        outputExcel.close();
    }

    /**
     * Compares a given name with the list of all donors and attempts to find the best match if any. Can find a perfect
     * match (identical strings), or can ask the user for confirmation if the fuzzy matcher natches above 70%.
     *
     * @param transaction    The current transaction to be compared with the list of all known donors.
     * @param allDonorsSheet The list of all known donors.
     * @return An optional integer containing the row on the list of all known donors with the best match. Empty if no
     * good match is found.
     */
    private Optional<Integer> findNameMatch(Transaction transaction, Sheet allDonorsSheet) {
        String name = transaction.getName();

        Optional<Integer> result = Optional.empty();
        // The first element of this list is the match score (101 for perfect match).
        // The rest are row indexes of the matches.
        List<Integer> highestMatchList = findHighestMatchList(name, allDonorsSheet);
        int highestScore = highestMatchList.get(0);

        boolean perfectMatch = highestScore == 101;
        if (perfectMatch) {
            if (highestMatchList.size() > 2) {
                result = Optional.of(handleMultipleMatches(highestMatchList, transaction.getAmount()));
            }
            // get(1) because the highest score is in index 0. Actual row numbers start at
            // index 1.
            else {
                result = Optional.of(highestMatchList.get(1));
            }
        }

        else if (highestScore > 70) {
            for (int i = 1; i < highestMatchList.size(); i++) {
                int matchedRow = highestMatchList.get(i);
                String matchedName = allDonorsSheet.getRow(matchedRow).getCell(3).getStringCellValue();

                int response = this.dialogueBox.showNameComparisonDialogue(name, matchedName);

                if (response == JOptionPane.YES_OPTION) {
                    return Optional.of(matchedRow);
                }
            }
        }

        return result;
    }

    /**
     * This method compares the names on the bank statement to the names on the list of all known donors and finds the
     * best match(es) using fuzzy string comparison.
     *
     * @param currentDonorName   The Name of the current donor that we are going to try to find a match for.
     * @param allKnownDonorsList The Excel list of all known donors.
     * @return A list of Integers. The first item is the best score found. The next items and the indices of the rows of
     * the best matched names on the list of all known donors.
     */
    private List<Integer> findHighestMatchList(String currentDonorName, Sheet allKnownDonorsList) {
        int highestScore = 0;
        List<Integer> matchedRows = new ArrayList<>();

        for (int i = 1; i < allKnownDonorsList.getLastRowNum(); i++) {
            Row row = allKnownDonorsList.getRow(i);

            if (row == null) {
                continue;
            }
            Cell nameCell = row.getCell(3);
            if (nameCell == null) {
                continue;
            }

            String knownDonorsName = nameCell.getStringCellValue().trim();
            currentDonorName = currentDonorName.trim();

            int score;
            if (currentDonorName.equals(knownDonorsName)) {
                // 101 means perfect match
                score = 101;
            }
            else {
                score = (FuzzySearch.tokenSortRatio(currentDonorName, knownDonorsName));
                if (score > 90) {
                    continue;
                }
            }

            if (score == highestScore) {
                matchedRows.add(i);
            }
            if (score > highestScore) {
                matchedRows.clear();
                highestScore = score;
                matchedRows.add(i);
            }
        }
        matchedRows.add(0, highestScore);
        return matchedRows;
    }

    /**
     * If multiple matches are found for a given name, this method looks for the best match by comparing the donation
     * amounts. If none are found with the same amount, the method just returns the index if the first match.
     *
     * @param highestMatchScoreList List of rows with the highest match scores
     * @return The match where the donated amount is the same or the first index in the match list if none are found
     */
    private int handleMultipleMatches(List<Integer> highestMatchScoreList, String donatedAmount) {
        for (int i = 1; i < highestMatchScoreList.size(); i++) {
            int rowIndex = highestMatchScoreList.get(i);
            Row row = this.allDonorsSheet.getRow(rowIndex);
            double savedAmount = row.getCell(6).getNumericCellValue();
            if (donatedAmount.equals(String.valueOf(savedAmount))) {
                return rowIndex;
            }
        }
        // Return the first index if no match is found with the same amount
        return highestMatchScoreList.get(1);
    }

    /**
     * Copies a row from the list of all donors into the newly created Excel file.
     *
     * @param allDonorsSheet  The list of all known donors.
     * @param outputSheet     The newly created output Excel sheet.
     * @param matchRowNumber  The number of the row to be copied.
     * @param outputRowNumber The number of the row to be copies into.
     */
    private void insertRow(Sheet allDonorsSheet, Sheet outputSheet, int matchRowNumber, int outputRowNumber) {

        Row matchRow = allDonorsSheet.getRow(matchRowNumber);
        Row outputRow = outputSheet.createRow(outputRowNumber);

        for (int i = 0; i < matchRow.getLastCellNum(); i++) {
            Cell matchCell = matchRow.getCell(i);
            Cell outputCell = outputRow.createCell(i);

            if (matchCell == null) {
                continue;
            }

            switch (matchCell.getCellType()) {

                case NUMERIC:
                    outputCell.setCellValue(matchCell.getNumericCellValue());
                    break;
                case STRING:
                    outputCell.setCellValue(matchCell.getStringCellValue());
                    break;
            }
        }
    }

    /**
     * Replaces the amount copied from the list of all known donors with the actual amount donated/debited
     *
     * @param outputRow       The row in the output file where the amount has to be replaced
     * @param amount          The actual amount in the transaction
     * @param transactionType The type of transaction: CREDIT or DEBIT
     */
    private void replaceAmount(Row outputRow, String amount, TransactionType transactionType) {
        switch (transactionType) {
            case CREDIT:
                Cell creditCell = outputRow.getCell(6);
                creditCell.setCellValue(amount);

                // Loop over donation purposes to replace the amount there as well
                for (int i = 7; i < outputRow.getLastCellNum(); i++) {
                    if (outputRow.getCell(i).getCellType() != CellType.BLANK) {
                        outputRow.getCell(i).setCellValue(amount);
                    }
                }
                break;

            case DEBIT:
                Cell debitCell = outputRow.getCell(7);
                debitCell.setCellValue(amount);
                break;
        }
    }

    /**
     * Inserts a new row into the output file in the case of a debit transaction
     *
     * @param transaction The transaction inserted into the output file
     * @param outputRow   The row where the transaction should be inserted
     */
    private void insertDebitRow(Transaction transaction, Row outputRow) {
        Cell nameCell = outputRow.createCell(3, CellType.STRING);
        Cell amountCell = outputRow.createCell(7, CellType.NUMERIC);

        nameCell.setCellValue(transaction.getName());
        amountCell.setCellValue(transaction.getAmount());
    }
}