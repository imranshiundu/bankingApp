package ke.greendaybank.approval;

import java.math.BigDecimal;

public record HighRiskMovementPayload(
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String narration,
        String idempotencyKey
) {}
