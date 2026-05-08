package ke.greendaybank.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AuditLog {
    private final List<AuditEvent> events = Collections.synchronizedList(new ArrayList<>());
    private final Clock clock;

    public AuditLog(Clock clock) {
        this.clock = clock;
    }

    public AuditEvent record(String actor, String action, String resource, Map<String, String> metadata) {
        AuditEvent event = new AuditEvent(UUID.randomUUID(), Instant.now(clock), actor, action, resource, metadata);
        events.add(event);
        return event;
    }

    public List<AuditEvent> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }
}
