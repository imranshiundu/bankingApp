package ke.greendaybank.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class AuditRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String actorLabel, String action, String resourceType, String resourceId, Map<String, String> metadata) {
        String json = metadata == null || metadata.isEmpty()
                ? "{}"
                : metadata.entrySet().stream()
                    .map(e -> "\"" + escape(e.getKey()) + "\":\"" + escape(e.getValue()) + "\"")
                    .reduce("{", (a, b) -> a.equals("{") ? a + b : a + "," + b) + "}";
        jdbcTemplate.update("""
                INSERT INTO audit.events(actor_label, action, resource_type, resource_id, metadata)
                VALUES (?, ?, ?, ?, ?::jsonb)
                """, actorLabel, action, resourceType, resourceId, json);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
