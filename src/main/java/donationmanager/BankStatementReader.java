package donationmanager;

import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BankStatementReader {
    /**
     * Extracts the of donations out of the bank statement PDF file. It has to extract each page
     * individually and then concatenate those tables together.
     *
     * @return the extracted table as a 2d List of Tabula RectangularTextContainer objects
     * @throws IOException from the load method of PDDocument
     */
    public static List<List<RectangularTextContainer>> extractTable() throws IOException {

        BasicExtractionAlgorithm extractionAlgorithm = new BasicExtractionAlgorithm();
        PDDocument inputPDF = PDDocument.load(new File("input.pdf"));
        ObjectExtractor extractor = new ObjectExtractor(inputPDF);

        List<List<RectangularTextContainer>> table = new ArrayList<>();

        int lastPage = inputPDF.getNumberOfPages() - 2;
        for (int pageNumber = 1; pageNumber <= lastPage; pageNumber++) {
            Table partialTable = extractTableFromPage(pageNumber,
                                                      lastPage,
                                                      extractor,
                                                      extractionAlgorithm);

            concatenateTables(table, partialTable);

        }
        return table;
    }

    /**
     * Extracts a table from a specific page of the PDF file
     *
     * @param pageNumber          Number if the page to be extracted
     * @param lastPageNumber      The final page to be extracted
     * @param extractor           Tabula class used to extract tables
     * @param extractionAlgorithm Tabula class used to extract tables
     * @return A tabula Table object
     */
    private static Table extractTableFromPage(int pageNumber,
                                              int lastPageNumber, ObjectExtractor extractor,
                                              BasicExtractionAlgorithm extractionAlgorithm) {
        Page page = extractor.extract(pageNumber);

        float top = (pageNumber == 1) ? 399.6f : 231.84f;
        float bottom = (pageNumber == lastPageNumber) ? 676.8f : 817.284f;
        float left = 71.052f;
        float right = 556.14f;

        List<Float> columnPositions = Arrays.asList(110.45f, 307.84f, 346.77f, 419.51f, 480.49f);
        return extractionAlgorithm.extract(page.getArea(top, left, bottom, right),
                                           columnPositions).get(0);
    }

    /**
     * Testing method used to print out an extracted table.
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
     * Testing method used to print out an extracted table.
     *
     * @param table The table to be printed, in a Tabula Table format
     */
    @SuppressWarnings("unused")
    public static void printTable(List<List<RectangularTextContainer>> table) {
        for (List<RectangularTextContainer> cells : table) {
            for (RectangularTextContainer cell : cells) {
                System.out.print(cell.getText() + "|");
            }
            System.out.println();
        }
        System.out.println("--------------");
    }

    /**
     * Joins two tables together.
     *
     * @param table        The object that will contain the full extracted table in the end.
     * @param partialTable The partial extracted table to be added to the full table.
     */
    private static void concatenateTables(List<List<RectangularTextContainer>> table,
                                          Table partialTable) {
        List<List<RectangularTextContainer>> rows = partialTable.getRows();

        table.addAll(rows);
    }
}
