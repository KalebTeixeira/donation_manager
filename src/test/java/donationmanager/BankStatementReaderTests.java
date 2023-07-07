package donationmanager;

import org.junit.jupiter.api.Test;
import technology.tabula.RectangularTextContainer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BankStatementReaderTests {
    @Test
    public void test_extract_table_from_bank_statement() throws IOException {
        File bankStatement = new File("test-bank-statement.pdf");
        BankStatementReader bankStatementReader = new BankStatementReader(bankStatement);

        List<List<RectangularTextContainer>> table = bankStatementReader.extractTable();

        final int tableSizeExpected = 726;
        final int rowSizeExpected = 5;
        final String firstRowExpected = "03.04.23";
        final String lastRowExpected = "PATENSCHAFT AETHIOPIEN";


        assertEquals(tableSizeExpected, table.size());
        assertEquals(rowSizeExpected, table.get(0).size());

        assertEquals(firstRowExpected, table.get(0).get(0).getText());
        assertEquals(lastRowExpected, table.get(tableSizeExpected - 1).get(1).getText());
    }

    @Test
    public void test_wrong_bank_statement_file_format() {
        File file = new File("test.png");

        BankStatementReader bankStatementReader = new BankStatementReader(file);
        Exception exception = assertThrows(IOException.class, bankStatementReader::extractTable);

        String messageExpected = "Error: End-of-File, expected line";

        assertEquals(messageExpected, exception.getMessage());


    }

    @Test
    public void test_inexistent_bank_statement() {
        File file = new File("IDontExist.pdf");

        BankStatementReader bankStatementReader = new BankStatementReader(file);
        Exception exception = assertThrows(IOException.class, bankStatementReader::extractTable);

        String messageExpected = "The system cannot find the file specified";

        assertTrue(exception.getMessage().contains(messageExpected));
    }

    @Test
    public void test_incorrect_bank_statement() {
        File file = new File("test.pdf");

        BankStatementReader bankStatementReader = new BankStatementReader(file);
        Exception exception = assertThrows(IllegalArgumentException.class, bankStatementReader::extractTable);

        String messageExpected = "The file is not a valid bank statement";

        assertTrue(exception.getMessage().contains(messageExpected));
    }

}
