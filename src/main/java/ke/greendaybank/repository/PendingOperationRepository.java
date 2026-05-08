package ke.greendaybank.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class PendingOperationRepository {
    private final JdbcTemplate jdbcTemplate;

    public PendingOperationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID createMovement(String fromAccount, String toAccount, BigDecimal amount, String narration, String idempotencyKey, String actor) {
        String payload = "{"
                + "\"fromAccount\":\"" + escape(fromAccount) + "\","
                + "\"toAccount\":\"" + escape(toAccount) + "\","
                + "\"amount\":\"" + amount.toPlainString() + "\","
                + "\"narration\":\"" + escape(narration) + "\","
                + "\"idempotencyKey\":\"" + escape(idempotencyKey) + "\""
                + "}";
        return jdbcTemplate.queryForObject("""
                INSERT INTO approvals.pending_operations(operation_type, request_payload, requested_by_label)
                VALUES ('HIGH_RISK_MOVEMENT', ?::jsonb, ?) RETURNING id
                """, UUID.class, payload, actor);
    }

    public PendingMovement getPendingMovement(UUID id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, status::text, request_payload, requested_by_label, created_at
                FROM approvals.pending_operations
                WHERE id = ? AND operation_type = 'HIGH_RISK_MOVEMENT'
                """, (rs, rowNum) -> new PendingMovement(
                rs.getObject("id", UUID.class),
                rs.getString("status"),
                rs.getString("request_payload"),
                rs.getString("requested_by_label"),
                rs.getObject("created_at", OffsetDateTime.class)
        ), id);
    }

    @Transactional
    public void markApproved(UUID id, String reviewer, String reason) {
        int updated = jdbcTemplate.update("""
                UPDATE approvals.pending_operations
                SET status = 'APPROVED', reviewed_by_label = ?, review_reason = ?, reviewed_at = now()
                WHERE id = ? AND status = 'PENDING_APPROVAL' AND requested_by_label <> ?
                """, reviewer, reason, id, reviewer);
        if (updated == 0) {
            throw new IllegalStateException("Approval request could not be approved");
        }
    }

    @Transactional
    public void markRejected(UUID id, String reviewer, String reason) {
        int updated = jdbcTemplate.update("""
                UPDATE approvals.pending_operations
                SET status = 'REJECTED', reviewed_by_label = ?, review_reason = ?, reviewed_at = now()
                WHERE id = ? AND status = 'PENDING_APPROVAL' AND requested_by_label <> ?
                """, reviewer, reason, id, reviewer);
        if (updated == 0) {
            throw new IllegalStateException("Approval request could not be rejected");
        }
    }

    @Transactional
    public void markExecuted(UUID id, String resultRef) {
        int updated = jdbcTemplate.update("""
                UPDATE approvals.pending_operations
                SET status = 'EXECUTED', executed_at = now(), result_ref = ?
                WHERE id = ? AND status = 'APPROVED'
                """, resultRef, id);
        if (updated == 0) {
            throw new IllegalStateException("Approved operation could not be executed");
        }
    }

    public List<PendingMovement> pending(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jdbcTemplate.query("""
                SELECT id, status::text, request_payload, requested_by_label, created_at
                FROM approvals.pending_operations
                WHERE operation_type = 'HIGH_RISK_MOVEMENT' AND status = 'PENDING_APPROVAL'
                ORDER BY created_at ASC
                LIMIT ?
                """, (rs, rowNum) -> new PendingMovement(
                rs.getObject("id", UUID.class),
                rs.getString("status"),
                rs.getString("request_payload"),
                rs.getString("requested_by_label"),
                rs.getObject("created_at", OffsetDateTime.class)
        ), safeLimit);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record PendingMovement(UUID id, String status, String requestPayload, String requestedByLabel, OffsetDateTime createdAt) {}
}
