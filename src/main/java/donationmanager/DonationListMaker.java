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

public class DonationListMaker {

    public static void makeDonationList(List<Transaction> transactions) throws IOException {
        Workbook allDonorsExcel = new XSSFWorkbook(new FileInputStream("ACTIVE DONORS  2021.xlsx"));
        Sheet allDonorsSheet = allDonorsExcel.getSheetAt(0);

        Workbook outputExcel = new XSSFWorkbook();
        Sheet outputSheet = outputExcel.createSheet();

        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);

            int matchRow = findNameMatch(transaction.getName(), allDonorsSheet);
            if (matchRow != -1) {
                insertRow(allDonorsSheet, outputSheet, matchRow, i);
            }
        }

        FileOutputStream outputStream = new FileOutputStream("output.xlsx");
        outputExcel.write(outputStream);
        outputExcel.close();
    }

    private static int findNameMatch(String name, Sheet allDonorsSheet) {
        // The first element of this list is the match score (101 for perfect match).
        // The rest are row indexes of the matches.
        List<Integer> highestMatchList = findHighestMatchList(name, allDonorsSheet);
        int highestScore = highestMatchList.get(0);

        boolean perfectMatch = highestScore == 101;
        if (perfectMatch) {
            if (highestMatchList.size() > 2) {
                handleMultipleMatches(highestMatchList);
            }
            return highestMatchList.get(1);
        }

        if (highestScore > 70) {
            return showNameComparisonDialogue(allDonorsSheet, name, highestMatchList);
        }

        return -1;
    }

    private static List<Integer> findHighestMatchList(String donorListName, Sheet allDonorsSheet) {
        int highestScore = 0;
        List<Integer> matchedRows = new ArrayList<>();

        for (int i = 1; i < allDonorsSheet.getLastRowNum(); i++) {
            Row row = allDonorsSheet.getRow(i);
            if (row == null) {
                continue;
            }

            Cell nameCell = row.getCell(3);
            if (nameCell == null) {
                continue;
            }
            String bankStatementName = nameCell.getStringCellValue().trim();

            int score;
            if (donorListName.equals(bankStatementName)) {
                // 101 means perfect match
                score = 101;
            }
            else {
                score = (FuzzySearch.tokenSortRatio(donorListName, bankStatementName));
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
                default:
                    outputCell.setCellValue(matchCell.getStringCellValue());
            }
        }
    }
}

