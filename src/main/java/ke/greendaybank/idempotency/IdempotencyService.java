package ke.greendaybank.idempotency;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {
    private final JdbcTemplate jdbcTemplate;

    public IdempotencyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ReplayResult beginOrReplay(String key, String requestHash, String operation) {
        return jdbcTemplate.query("""
                SELECT request_hash, status::text, response_code, response_body::text
                FROM api.idempotency_records
                WHERE idempotency_key = ? AND operation = ?
                FOR UPDATE
                """, rs -> {
            if (!rs.next()) {
                jdbcTemplate.update("""
                        INSERT INTO api.idempotency_records(idempotency_key, request_hash, operation, status, locked_until)
                        VALUES (?, ?, ?, 'PROCESSING', now() + interval '2 minutes')
                        """, key, requestHash, operation);
                return ReplayResult.fresh();
            }

            String existingHash = rs.getString("request_hash");
            String status = rs.getString("status");
            Integer responseCode = (Integer) rs.getObject("response_code");
            String responseBody = rs.getString("response_body");

            if (!existingHash.equals(requestHash)) {
                throw new IllegalArgumentException("Idempotency key reused with a different request body");
            }
            if ("COMPLETED".equals(status) && responseBody != null) {
                return ReplayResult.replay(responseCode == null ? 200 : responseCode, responseBody);
            }
            if ("PROCESSING".equals(status)) {
                throw new IllegalStateException("Idempotent request is still processing");
            }
            return ReplayResult.fresh();
        }, key, operation);
    }

    @Transactional
    public void complete(String key, int responseCode, String responseBody) {
        jdbcTemplate.update("""
                UPDATE api.idempotency_records
                SET status = 'COMPLETED', response_code = ?, response_body = ?::jsonb, updated_at = now(), locked_until = NULL
                WHERE idempotency_key = ?
                """, responseCode, responseBody, key);
    }

    @Transactional
    public void fail(String key, String failureCode) {
        jdbcTemplate.update("""
                UPDATE api.idempotency_records
                SET status = 'FAILED', failure_code = ?, updated_at = now(), locked_until = NULL
                WHERE idempotency_key = ?
                """, failureCode, key);
    }

    public record ReplayResult(boolean replay, int responseCode, String responseBody) {
        static ReplayResult fresh() {
            return new ReplayResult(false, 0, null);
        }

        static ReplayResult replay(int code, String body) {
            return new ReplayResult(true, code, body);
        }
    }
}
