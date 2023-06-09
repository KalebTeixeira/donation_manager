package donationmanager;

import technology.tabula.RectangularTextContainer;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        List<List<RectangularTextContainer>> extractedTable = BankStatementReader.extractTable();

        BankStatementReader.printTable(extractedTable);

        TransactionManager transactionManager = new TransactionManager(extractedTable);
        List<Transaction> transactions = transactionManager.makeTransactionList();

        DonationListMaker.makeDonationList(transactions);
    }
}
