package donationmanager;

import technology.tabula.RectangularTextContainer;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        List<List<RectangularTextContainer>> extractedTable = BankStatementReader.extractTable();
        //BankStatementReader.printTable(extractedTable);
        TransactionManager transactionManager = new TransactionManager(extractedTable);
        List<Transaction> transactions = TransactionManager.makeDonationList();
    }
}