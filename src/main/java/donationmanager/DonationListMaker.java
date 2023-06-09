package donationmanager;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DonationListMaker {

    /**
     * Fills a newly created Excel output file the current donors.
     *
     * @param transactions A list of transaction objects (entries read from the bank statement).
     * @throws Exception
     */
    public static void makeDonationList(List<Transaction> transactions) throws IOException {

        Workbook allDonorsExcel =
                new XSSFWorkbook(new FileInputStream(new FileHandler().openFile()));
        Sheet allDonorsSheet = allDonorsExcel.getSheet("ACTIVE DONORS  2022");

        Workbook outputExcel = new XSSFWorkbook();
        Sheet outputSheet = outputExcel.createSheet();

        // Copy over the header row
        insertRow(allDonorsSheet, outputSheet, 0, 0);

        for (int i = 1; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);

            Optional<Integer> matchRow = findNameMatch(transaction.getName(), allDonorsSheet);
            if (matchRow.isPresent()) {
                insertRow(allDonorsSheet, outputSheet, matchRow.get(), i);
            }
        }

        FileOutputStream outputStream = new FileOutputStream(new FileHandler().saveFile());
        outputExcel.write(outputStream);
        outputExcel.close();
    }

    /**
     * Compares a given name with the list of a all donors and attempts to find the best match if
     * any. Can finds a perfect match (identical strings), or can ask the user for confirmation if
     * the fuzzy matcher natches above 70%.
     *
     * @param name           The current name to be compared with the list of all known donors.
     * @param allDonorsSheet The list of all known donors.
     * @return An optional integer containing the row on the list of all known donors with the best
     * match. Empty if no good match is found.
     */
    private static Optional<Integer> findNameMatch(String name, Sheet allDonorsSheet) {
        // The first element of this list is the match score (101 for perfect match).
        // The rest are row indexes of the matches.
        Optional<Integer> result = Optional.empty();
        List<Integer> highestMatchList = findHighestMatchList(name, allDonorsSheet);
        int highestScore = highestMatchList.get(0);

        boolean perfectMatch = highestScore == 101;
        if (perfectMatch) {
            /*if (highestMatchList.size() > 2) {
                handleMultipleMatches(highestMatchList);
            }*/
            // get(1) because the highest score is in index 0. Actual row numbers start at
            // index 1.
            result = Optional.of(highestMatchList.get(1));
        }

        /*if (highestScore > 70) {
            result = Optional.of(showNameComparisonDialogue(allDonorsSheet,
                                                            name,
                                                            highestMatchList));
        }*/

        return result;
    }

    /**
     * This method compares the names on the bank statement to the names on the list of all known
     * donors and finds the best match(es) using fuzzy string comparison.
     *
     * @param currentDonorName   The Name of the current donor that we are going to try to find a
     *                           match for.
     * @param allKnownDonorsList The Excel list of all known donors.
     * @return A list of Integers. The first item is the best score found. The next items and the
     * indices of the rows of the best matched names on the list of all known donors.
     */
    private static List<Integer> findHighestMatchList(String currentDonorName,
                                                      Sheet allKnownDonorsList) {
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

            int score;
            if (currentDonorName.equals(knownDonorsName)) {
                // 101 means perfect match
                score = 101;
            }
            else {
                score = (FuzzySearch.tokenSortRatio(currentDonorName, knownDonorsName));
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
     * If two matches are found with the exact same score, for example two known donors with perfect
     * matches, this method warns the user about this, then just picks the first match.
     *
     * @param highestMatchScoreList List of rows with the highest match scores.
     */
    private static void handleMultipleMatches(List<Integer> highestMatchScoreList) {
        StringBuilder message = new StringBuilder("Multiple matches were found: Rows ");

        for (int i = 1; i < highestMatchScoreList.size(); i++) {
            if (i != 1) {
                message.append(",");
            }
            int physicalRowNumber = highestMatchScoreList.get(i) + 1;
            message.append(" ").append(physicalRowNumber);
        }
        message.append(". Using first match.");

        JOptionPane.showMessageDialog(null,
                                      message.toString(),
                                      "Multiple matches",
                                      JOptionPane.WARNING_MESSAGE);
    }

    /**
     * When the fuzzy matcher finds a match above 70% similarity, this method asks the user for
     * confirmation that both name are the same using a simple dialogue box.
     *
     * @param allDonorsSheet   The Excel list of all known donors.
     * @param name             The current donor name being compared.
     * @param highestMatchList List of rows with the highest match scores.
     * @return returns the row number of the matching name if the user confirms equality. Returns -1
     * if the user denies any possible matches.
     */
    private static int showNameComparisonDialogue(Sheet allDonorsSheet,
                                                  String name,
                                                  List<Integer> highestMatchList) {

        for (int i = 1; i < highestMatchList.size(); i++) {
            int matchedRow = highestMatchList.get(i);
            String matchedName = allDonorsSheet.getRow(matchedRow).getCell(3).getStringCellValue();

            String message = String.format("Is %s = %s?", name, matchedName);
            int response = JOptionPane.showConfirmDialog(null,
                                                         message,
                                                         "",
                                                         JOptionPane.YES_NO_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                return matchedRow;
            }
        }
        return -1;
    }

    /**
     * Copies a row from the list of all donors into the newly created Excel file.
     *
     * @param allDonorsSheet  The list of all known donors.
     * @param outputSheet     The newly created output Excel sheet.
     * @param matchRowNumber  The number of the row to be copied.
     * @param outputRowNumber The number of the row to be copies into.
     */
    private static void insertRow(Sheet allDonorsSheet,
                                  Sheet outputSheet,
                                  int matchRowNumber,
                                  int outputRowNumber) {

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
}

