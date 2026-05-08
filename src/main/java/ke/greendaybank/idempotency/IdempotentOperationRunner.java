package ke.greendaybank.idempotency;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class IdempotentOperationRunner {
    private final IdempotencyService idempotency;
    private final RequestHasher hasher;

    public IdempotentOperationRunner(IdempotencyService idempotency, RequestHasher hasher) {
        this.idempotency = idempotency;
        this.hasher = hasher;
    }

    public ResponseEntity<String> run(String operation, String key, String canonicalRequest, Supplier<String> bodySupplier) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        if (key.length() < 24) {
            throw new IllegalArgumentException("Idempotency key is too short");
        }
        String requestHash = hasher.hash(canonicalRequest);
        IdempotencyService.ReplayResult replay = idempotency.beginOrReplay(key, requestHash, operation);
        if (replay.replay()) {
            return ResponseEntity.status(replay.responseCode()).body(replay.responseBody());
        }
        try {
            String responseBody = bodySupplier.get();
            idempotency.complete(key, HttpStatus.CREATED.value(), responseBody);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
        } catch (RuntimeException ex) {
            idempotency.fail(key, ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
