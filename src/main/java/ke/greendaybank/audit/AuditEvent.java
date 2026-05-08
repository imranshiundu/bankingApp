package ke.greendaybank.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        Instant occurredAt,
        String actor,
        String action,
        String resource,
        Map<String, String> metadata
) {
    public AuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(resource, "resource");
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
