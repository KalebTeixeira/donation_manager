package donationmanager;

import technology.tabula.RectangularTextContainer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        FileHandler fileHandler = new FileHandler();
        File bankStatement = fileHandler.chooseFileToOpen("Choose the bank statement PDF.", FileFilter.PDF);
        BankStatementReader bankStatementReader = new BankStatementReader(bankStatement);

        List<List<RectangularTextContainer>> extractedTable = bankStatementReader.extractTable();

        TransactionManager transactionManager = new TransactionManager(extractedTable);

        List<Transaction> transactions = transactionManager.makeTransactionList();

        File allDonorsExcel = fileHandler.chooseFileToOpen("Choose the file with all known donors", FileFilter.EXCEL);
        File outputExcel = fileHandler.chooseFileToSave("Choose Where to save the output", FileFilter.EXCEL);
        DonationListMaker donationListMaker = new DonationListMaker(allDonorsExcel,
                                                                    transactions,
                                                                    outputExcel,
                                                                    new DialogueBox());

        donationListMaker.makeDonationList();
    }
}
