package donationmanager;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class Transaction {
    @NonNull
    private String name;

    @NonNull
    private String amount;

    @NonNull
    private TransactionType transactionType;

    @NonNull
    private String date;

    @NonNull
    private List<String> miscText;

    private String donationCategory;
}
