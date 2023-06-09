package donationmanager;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
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

    private final String donationCategory = "";
}
