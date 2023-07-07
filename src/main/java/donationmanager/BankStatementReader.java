package donationmanager;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.*;
import technology.tabula.extractors.BasicExtractionAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class BankStatementReader {
    private final File bankStatement;

    /**
     * Extracts the of donations out of the bank statement PDF file. It has to extract each page individually and then
     * concatenate those tables together.
     *
     * @return the extracted table as a 2d List of Tabula RectangularTextContainer objects
     * @throws IOException if loading the bank statement fails
     */
    public List<List<RectangularTextContainer>> extractTable() throws IOException {

        BasicExtractionAlgorithm extractionAlgorithm = new BasicExtractionAlgorithm();

        PDDocument inputPDF = PDDocument.load(this.bankStatement);
        ObjectExtractor extractor = new ObjectExtractor(inputPDF);

        validateFile(extractor);

        List<List<RectangularTextContainer>> table = new ArrayList<>();

        int lastPage = inputPDF.getNumberOfPages();
        for (int pageNumber = 1; pageNumber <= lastPage; pageNumber++) {
            List<List<RectangularTextContainer>> partialTable = extractTableFromPage(pageNumber,
                                                                                     lastPage,
                                                                                     extractor,
                                                                                     extractionAlgorithm);

            table.addAll(partialTable);

        }
        inputPDF.close();

        printTable(table);
        return table;
    }


    /**
     * Extracts a table from a specific page of the PDF file. If no table is found in the page, it returns an empty
     * list.
     *
     * @param pageNumber          Number if the page to be extracted
     * @param lastPageNumber      The final page to be extracted
     * @param extractor           Tabula class used to extract tables
     * @param extractionAlgorithm Tabula class used to extract tables
     * @return A 2D list of RectangularTextContainers
     */
    private static List<List<RectangularTextContainer>> extractTableFromPage(int pageNumber,
                                                                             int lastPageNumber,
                                                                             ObjectExtractor extractor,
                                                                             BasicExtractionAlgorithm extractionAlgorithm) {
        Page page = extractor.extract(pageNumber);

        float top = (pageNumber == 1) ? 399.6f : 231.84f;
        float bottom = (pageNumber == lastPageNumber) ? 676.8f : 817.284f;
        float left = 71.052f;
        float right = 500f;

        List<Float> columnPositions = Arrays.asList(110.45f, 307.84f, 346.77f, 419.51f);
        Table table = extractionAlgorithm.extract(page.getArea(top, left, bottom, right), columnPositions).get(0);

        return trim(table);
    }

    /**
     * Debugging method used to print out an extracted table.
     *
     * @param table The table to be printed, in a 2D list of Tabula RectangularTextContainer format
     */
    @SuppressWarnings("unused")
    public static void printTable(Table table) {
        List<List<RectangularTextContainer>> rows = table.getRows();

        for (List<RectangularTextContainer> cells : rows) {
            for (RectangularTextContainer cell : cells) {
                System.out.print(cell.getText() + "|");
            }
            System.out.println();
        }
        System.out.println("--------------");
    }

    /**
     * Debugging method used to print out an extracted table.
     *
     * @param table The table to be printed, in a Tabula Table format
     */
    @SuppressWarnings("unused")
    public static void printTable(List<List<RectangularTextContainer>> table) {
        for (int i = 0; i < table.size(); i++) {
            List<RectangularTextContainer> cells = table.get(i);
            for (RectangularTextContainer cell : cells) {
                System.out.print(cell.getText() + "|");
            }
            System.out.println(" " + (i + 1));
        }
        System.out.println("--------------");
    }

    /**
     * Removes useless rows from the end of the table, starting with the row that reads "Saldo per..."
     *
     * @param table The table to be trimmed
     * @return the trimmed table as a 2d list of RectangularTextContainers
     */
    private static List<List<RectangularTextContainer>> trim(Table table) {
        List<List<RectangularTextContainer>> iterableTable = table.getRows();
        for (int i = iterableTable.size() - 1; i >= 0; i--) {
            if (iterableTable.get(i).get(1).getText().contains("Saldo per")) {
                iterableTable.subList(i, iterableTable.size()).clear();
                return iterableTable;
            }
        }
        return iterableTable;
    }

    /**
     * Checks if the file is a valid bank statement. It does this by reading the first 13 characters of the file. In
     * every AEK bank statement, this will be "AEK BANK 1826".
     *
     * @param extractor Contains information about the file to be validated
     * @throws IllegalArgumentException if the file is not valid
     */
    private void validateFile(ObjectExtractor extractor) {
        StringBuilder AEKString = new StringBuilder();
        List<TextElement> textInFirstPage = extractor.extract(1).getText();
        try {
            for (int i = 0; i < 13; i++) {
                AEKString.append(textInFirstPage.get(i).getText());
            }
        }
        catch (IndexOutOfBoundsException exception) {
            throwInvalidStatementException();
        }
        if (!AEKString.toString().equals("AEK BANK 1826")) {
            System.out.print(AEKString);
            throwInvalidStatementException();
        }
    }

    private void throwInvalidStatementException() {
        throw new IllegalArgumentException("The selected file is not a valid bank statement");
    }
}
