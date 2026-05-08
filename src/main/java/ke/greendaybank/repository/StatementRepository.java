package ke.greendaybank.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class StatementRepository {
    private final JdbcTemplate jdbcTemplate;

    public StatementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<StatementLine> latest(String accountNumber, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jdbcTemplate.query("""
                SELECT sequence_no, posted_at, transaction_ref, transaction_type, side::text, amount, currency, narration, running_balance
                FROM core.account_statement_lines
                WHERE account_number = ?
                ORDER BY sequence_no DESC
                LIMIT ?
                """, (rs, rowNum) -> new StatementLine(
                rs.getLong("sequence_no"),
                rs.getObject("posted_at", OffsetDateTime.class),
                rs.getString("transaction_ref"),
                rs.getString("transaction_type"),
                rs.getString("side"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("narration"),
                rs.getBigDecimal("running_balance")
        ), accountNumber, safeLimit);
    }

    public record StatementLine(
            long sequenceNo,
            OffsetDateTime postedAt,
            String transactionRef,
            String transactionType,
            String side,
            BigDecimal amount,
            String currency,
            String narration,
            BigDecimal runningBalance
    ) {}
}
