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

@SuppressWarnings("rawtypes")
public class BankStatementReader {
    public static List<List<RectangularTextContainer>> extractTable() throws IOException {

        BasicExtractionAlgorithm extractionAlgorithm = new BasicExtractionAlgorithm();
        PDDocument inputPDF = PDDocument.load(new File("input.pdf"));
        ObjectExtractor extractor = new ObjectExtractor(inputPDF);

        List<List<RectangularTextContainer>> table = new ArrayList<>();

        int lastPage = inputPDF.getNumberOfPages() - 2;
        for (int pageNumber = 1; pageNumber <= lastPage; pageNumber++) {
            Table partialTable = extractPartialTable(pageNumber,
                                                     lastPage,
                                                     extractor,
                                                     extractionAlgorithm);

            concatenateTables(table, partialTable);

        }
        return table;
    }

    private static Table extractPartialTable(int pageNumber,
                                             int lastPage, ObjectExtractor extractor,
                                             BasicExtractionAlgorithm extractionAlgorithm) {
        Page page = extractor.extract(pageNumber);

        float top = (pageNumber == 1) ? 399.6f : 231.84f;
        float bottom = (pageNumber == lastPage) ? 676.8f : 817.284f;
        float left = 71.052f;
        float right = 556.14f;

        List<Float> columnPositions = Arrays.asList(110.45f, 307.84f, 346.77f, 419.51f, 480.49f);
        return extractionAlgorithm.extract(page.getArea(top, left, bottom, right),
                                           columnPositions).get(0);
    }

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

    private static void concatenateTables(List<List<RectangularTextContainer>> table,
                                          Table partialTable) {
        List<List<RectangularTextContainer>> rows = partialTable.getRows();

        table.addAll(rows);
    }
}
