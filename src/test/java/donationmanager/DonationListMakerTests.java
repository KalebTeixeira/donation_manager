package donationmanager;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class DonationListMakerTests {
    public DialogueBox dialogueBox = new DialogueBox();

    @Test
    public void test_output_correct_donation_list() throws IOException {
        // given
        File allDonorsFile = new File("test-files/all-donors-test.xlsx");
        File outputFile = new File("test-files/output-test.xlsx");

        String transaction1Name = "Gilbert und Liselotte Aebischer-Pfander";
        String transaction1Amount = "10";
        TransactionType transaction1Type = TransactionType.CREDIT;
        String transaction1Date = "";
        List<String> transaction1Text = new ArrayList<>();

        String transaction2Name = "Belastung";
        String transaction2Amount = "10000";
        TransactionType transaction2Type = TransactionType.DEBIT;
        String transaction2Date = "";
        List<String> transaction2Text = new ArrayList<>();

        String transaction3Name = "This name is not present in the list of all donors";
        String transaction3Amount = "10'000.50";
        TransactionType transaction3Type = TransactionType.CREDIT;
        String transaction3Date = "";
        List<String> transaction3Text = new ArrayList<>();

        String transaction4Name = "Affentranger-Imbach Elisabeth";
        String transaction4Amount = "1000";
        TransactionType transaction4Type = TransactionType.CREDIT;
        String transaction4Date = "";
        List<String> transaction4Text = new ArrayList<>();

        List<Transaction> transactions = List.of(new Transaction(transaction1Name,
                                                                 transaction1Amount,
                                                                 transaction1Type,
                                                                 transaction1Date,
                                                                 transaction1Text),
                                                 new Transaction(transaction2Name,
                                                                 transaction2Amount,
                                                                 transaction2Type,
                                                                 transaction2Date,
                                                                 transaction2Text),
                                                 new Transaction(transaction3Name,
                                                                 transaction3Amount,
                                                                 transaction3Type,
                                                                 transaction3Date,
                                                                 transaction3Text),
                                                 new Transaction(transaction4Name,
                                                                 transaction4Amount,
                                                                 transaction4Type,
                                                                 transaction4Date,
                                                                 transaction4Text));

        DialogueBox dialogueBox = spy(new DialogueBox());
        DonationListMaker donationListMaker = new DonationListMaker(allDonorsFile,
                                                                    transactions,
                                                                    outputFile,
                                                                    dialogueBox);

        doReturn(JOptionPane.YES_OPTION).when(dialogueBox).showNameComparisonDialogue(any(), any());

        // when
        donationListMaker.makeDonationList();

        // then
        XSSFSheet outputSheet = new XSSFWorkbook(new FileInputStream(outputFile)).getSheetAt(0);

        // Check if header is present
        assertEquals("Code /", outputSheet.getRow(0).getCell(0).getStringCellValue());

        XSSFRow row1 = outputSheet.getRow(1);
        XSSFRow row2 = outputSheet.getRow(2);
        XSSFRow row3 = outputSheet.getRow(3);
        XSSFRow row4 = outputSheet.getRow(4);

        assertEquals(transaction1Name, row1.getCell(3).getStringCellValue());
        assertEquals(transaction1Amount, row1.getCell(6).getStringCellValue());

        assertEquals(transaction2Name, row2.getCell(3).getStringCellValue());
        assertEquals(transaction2Amount, row2.getCell(7).getStringCellValue());

        // Should be empty because the name is not present in the list of all donors
        assertNull(row3);

        assertEquals(transaction4Name, row4.getCell(3).getStringCellValue());
        assertEquals(transaction4Amount, row4.getCell(6).getStringCellValue());
    }

    @Test
    public void test_incorrect_donors_file_format() {
        // given
        File allDonorsFile = new File("test-files/test.txt");
        File outputFile = new File("test-files/output-test.xlsx");
        List<Transaction> transactions = new ArrayList<>();

        // when then
        Exception exception = assertThrows(IllegalArgumentException.class,
                                           () -> new DonationListMaker(allDonorsFile,
                                                                       transactions,
                                                                       outputFile,
                                                                       dialogueBox));
        assertEquals("No valid entries or contents found, this is not a valid OOXML (Office Open XML) file",
                     exception.getMessage());
    }

    @Test
    void test_incorrect_donors_excel_file() {
        // given
        File allDonorsFile = new File("test-files/test.xlsx");
        File outputFile = new File("test-files/output-test.xlsx");
        List<Transaction> transactions = new ArrayList<>();

        // when then
        Exception exception = assertThrows(IllegalArgumentException.class,
                                           () -> new DonationListMaker(allDonorsFile,
                                                                       transactions,
                                                                       outputFile,
                                                                       dialogueBox));
        assertEquals("The Excel file with all known donors is not valid", exception.getMessage());
    }

    @Test
    void test_incorrect_output_file_format() {
        // given
        File allDonorsFile = new File("test-files/all-donors-test.xlsx");
        File outputFile = new File("test-files/output-test.txt");
        List<Transaction> transactions = new ArrayList<>();


        // when then
        Exception exception = assertThrows(IllegalArgumentException.class,
                                           () -> new DonationListMaker(allDonorsFile,
                                                                       transactions,
                                                                       outputFile,
                                                                       dialogueBox));
        assertEquals("The output file does not have the correct file format (must be an Excel file)",
                     exception.getMessage());
    }
}

