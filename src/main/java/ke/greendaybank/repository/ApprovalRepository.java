package ke.greendaybank.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ApprovalRepository {
    private final JdbcTemplate jdbcTemplate;

    public ApprovalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID request(String resourceType, UUID resourceId, String action, UUID requestedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO approvals.approval_requests(resource_type, resource_id, action, requested_by)
                VALUES (?, ?, ?, ?) RETURNING id
                """, UUID.class, resourceType, resourceId, action, requestedBy);
    }

    public void approve(UUID approvalId, UUID reviewedBy) {
        jdbcTemplate.update("""
                UPDATE approvals.approval_requests
                SET status = 'APPROVED', reviewed_by = ?, reviewed_at = now()
                WHERE id = ? AND status = 'PENDING' AND requested_by IS DISTINCT FROM ?
                """, reviewedBy, approvalId, reviewedBy);
    }

    public void reject(UUID approvalId, UUID reviewedBy, String reason) {
        jdbcTemplate.update("""
                UPDATE approvals.approval_requests
                SET status = 'REJECTED', reviewed_by = ?, reviewed_at = now(), rejection_reason = ?
                WHERE id = ? AND status = 'PENDING' AND requested_by IS DISTINCT FROM ?
                """, reviewedBy, reason, approvalId, reviewedBy);
    }

    public List<ApprovalItem> pending(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jdbcTemplate.query("""
                SELECT id, resource_type, resource_id, action, status::text, requested_by, created_at
                FROM approvals.approval_requests
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                LIMIT ?
                """, (rs, rowNum) -> new ApprovalItem(
                rs.getObject("id", UUID.class),
                rs.getString("resource_type"),
                rs.getObject("resource_id", UUID.class),
                rs.getString("action"),
                rs.getString("status"),
                rs.getObject("requested_by", UUID.class),
                rs.getObject("created_at", OffsetDateTime.class)
        ), safeLimit);
    }

    public record ApprovalItem(UUID id, String resourceType, UUID resourceId, String action, String status, UUID requestedBy, OffsetDateTime createdAt) {}
}
