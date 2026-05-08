package ke.greendaybank.core;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        UUID transactionId,
        Instant postedAt,
        String accountId,
        EntrySide side,
        Money amount,
        String narration
) {
    public LedgerEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(postedAt, "postedAt");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(narration, "narration");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Ledger entry amount must be positive");
        }
    }

    public Money signedAmount() {
        return side == EntrySide.CREDIT ? amount : new Money(amount.amount().negate(), amount.currency());
    }
}
