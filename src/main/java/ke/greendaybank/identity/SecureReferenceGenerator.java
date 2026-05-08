package ke.greendaybank.identity;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Component
public final class SecureReferenceGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public String customerRef() {
        return "CIF_" + token(18);
    }

    public String accountNumber() {
        // Bank-friendly but still non-sequential. Avoids revealing customer count or branch volume.
        return "GD" + digits(3) + "-" + token(10).toUpperCase();
    }

    public String transactionRef() {
        return "TX_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36).toUpperCase() + "_" + token(16).toUpperCase();
    }

    public String idempotencyKey() {
        return "IK_" + token(32);
    }

    public String auditCorrelationId() {
        return "AUD_" + token(24);
    }

    private String token(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    private String digits(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
