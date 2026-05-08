package ke.greendaybank.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ke.greendaybank.approval.HighRiskMovementPayload;
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
    private final ObjectMapper objectMapper;

    public PendingOperationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID createMovement(String fromAccount, String toAccount, BigDecimal amount, String narration, String idempotencyKey, String actor) {
        HighRiskMovementPayload payload = new HighRiskMovementPayload(fromAccount, toAccount, amount, narration, idempotencyKey);
        return jdbcTemplate.queryForObject("""
                INSERT INTO approvals.pending_operations(operation_type, request_payload, requested_by_label, idempotency_key)
                VALUES ('HIGH_RISK_MOVEMENT', ?::jsonb, ?, ?)
                ON CONFLICT (operation_type, idempotency_key)
                DO UPDATE SET updated_at = approvals.pending_operations.created_at
                RETURNING id
                """, UUID.class, writeJson(payload), actor, idempotencyKey);
    }

    public PendingMovement getPendingMovement(UUID id) {
        return jdbcTemplate.queryForObject("""
                SELECT id, status::text, request_payload::text, requested_by_label, created_at
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

    public HighRiskMovementPayload movementPayload(UUID id) {
        PendingMovement pending = getPendingMovement(id);
        try {
            return objectMapper.readValue(pending.requestPayload(), HighRiskMovementPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored approval payload is not readable", e);
        }
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
                SELECT id, status::text, request_payload::text, requested_by_label, created_at
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

    private String writeJson(HighRiskMovementPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Approval payload could not be serialized", e);
        }
    }

    public record PendingMovement(UUID id, String status, String requestPayload, String requestedByLabel, OffsetDateTime createdAt) {}
}
