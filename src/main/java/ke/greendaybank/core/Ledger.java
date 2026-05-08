package ke.greendaybank.core;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Ledger {
    private final List<LedgerEntry> entries = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Currency> accountCurrencies = new ConcurrentHashMap<>();
    private final Clock clock;

    public Ledger(Clock clock) {
        this.clock = clock;
    }

    public void registerAccount(String accountId, Currency currency) {
        accountCurrencies.putIfAbsent(accountId, currency);
    }

    public UUID postTransfer(String fromAccountId, String toAccountId, Money amount, String narration) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        requireAccount(fromAccountId, amount.currency());
        requireAccount(toAccountId, amount.currency());
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        Money sourceBalance = balanceOf(fromAccountId, amount.currency());
        if (sourceBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        UUID transactionId = UUID.randomUUID();
        Instant now = Instant.now(clock);
        List<LedgerEntry> transactionEntries = List.of(
                new LedgerEntry(UUID.randomUUID(), transactionId, now, fromAccountId, EntrySide.DEBIT, amount, narration),
                new LedgerEntry(UUID.randomUUID(), transactionId, now, toAccountId, EntrySide.CREDIT, amount, narration)
        );
        assertBalanced(transactionEntries);
        entries.addAll(transactionEntries);
        return transactionId;
    }

    public UUID postExternalCredit(String toAccountId, Money amount, String suspenseAccountId, String narration) {
        requireAccount(suspenseAccountId, amount.currency());
        return postTransfer(suspenseAccountId, toAccountId, amount, narration);
    }

    public Money balanceOf(String accountId, Currency currency) {
        requireAccount(accountId, currency);
        Money balance = Money.zero(currency);
        synchronized (entries) {
            for (LedgerEntry entry : entries) {
                if (entry.accountId().equals(accountId)) {
                    balance = balance.plus(entry.signedAmount());
                }
            }
        }
        return balance;
    }

    public List<LedgerEntry> entries() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }

    private void requireAccount(String accountId, Currency currency) {
        Currency registered = accountCurrencies.get(accountId);
        if (registered == null) {
            throw new IllegalArgumentException("Unknown ledger account: " + accountId);
        }
        if (!registered.equals(currency)) {
            throw new IllegalArgumentException("Currency mismatch for account " + accountId);
        }
    }

    private void assertBalanced(List<LedgerEntry> transactionEntries) {
        if (transactionEntries.size() < 2) {
            throw new IllegalStateException("A financial transaction requires at least two ledger entries");
        }
        Currency currency = transactionEntries.getFirst().amount().currency();
        Money total = Money.zero(currency);
        for (LedgerEntry entry : transactionEntries) {
            total = total.plus(entry.signedAmount());
        }
        if (total.amount().signum() != 0) {
            throw new IllegalStateException("Unbalanced transaction rejected");
        }
    }
}
