package ke.greendaybank.customer;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CustomerRecord(
        UUID customerId,
        String accountNumber,
        String encryptedProfile,
        Instant createdAt,
        CustomerStatus status
) {
    public CustomerRecord {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(accountNumber, "accountNumber");
        Objects.requireNonNull(encryptedProfile, "encryptedProfile");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(status, "status");
    }
}
