package ke.greendaybank.repository;

import ke.greendaybank.identity.SecureReferenceGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public class LedgerRepository {
    private static final String SETTLEMENT_ACCOUNT = "GD-GL-SETTLEMENT-KES";

    private final JdbcTemplate jdbcTemplate;
    private final SecureReferenceGenerator references;

    public LedgerRepository(JdbcTemplate jdbcTemplate, SecureReferenceGenerator references) {
        this.jdbcTemplate = jdbcTemplate;
        this.references = references;
    }

    @Transactional
    public String postCredit(String accountNumber, BigDecimal amount, String idempotencyKey, String narration) {
        ensureSettlementAccount();
        String transactionRef = references.transactionRef();
        UUID transactionId = createPendingTransaction(transactionRef, idempotencyKey, "ACCOUNT_CREDIT", narration);
        postLine(transactionId, accountId(SETTLEMENT_ACCOUNT), "DEBIT", amount, narration);
        postLine(transactionId, accountId(accountNumber), "CREDIT", amount, narration);
        markPosted(transactionId);
        return transactionRef;
    }

    @Transactional
    public String postMovement(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String idempotencyKey, String narration) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }
        if (balance(fromAccountNumber).compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        String transactionRef = references.transactionRef();
        UUID transactionId = createPendingTransaction(transactionRef, idempotencyKey, "ACCOUNT_MOVEMENT", narration);
        postLine(transactionId, accountId(fromAccountNumber), "DEBIT", amount, narration);
        postLine(transactionId, accountId(toAccountNumber), "CREDIT", amount, narration);
        markPosted(transactionId);
        return transactionRef;
    }

    public BigDecimal balance(String accountNumber) {
        BigDecimal value = jdbcTemplate.queryForObject("SELECT balance FROM core.account_balances WHERE account_number = ?", BigDecimal.class, accountNumber);
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }

    private UUID createPendingTransaction(String transactionRef, String idempotencyKey, String type, String narration) {
        return jdbcTemplate.queryForObject("INSERT INTO core.transactions(transaction_ref, idempotency_key, transaction_type, narration, status) VALUES (?, ?, ?, ?, 'PENDING') RETURNING id", UUID.class, transactionRef, idempotencyKey, type, narration);
    }

    private void postLine(UUID transactionId, UUID accountId, String side, BigDecimal amount, String narration) {
        jdbcTemplate.update("INSERT INTO core.ledger_entries(transaction_id, account_id, side, amount, currency, narration) VALUES (?, ?, ?::core.ledger_side, ?, 'KES', ?)", transactionId, accountId, side, amount, narration);
    }

    private void markPosted(UUID transactionId) {
        jdbcTemplate.update("SELECT core.assert_transaction_balanced(?)", transactionId);
        jdbcTemplate.update("UPDATE core.transactions SET status = 'POSTED', posted_at = now() WHERE id = ?", transactionId);
    }

    private UUID accountId(String accountNumber) {
        UUID id = jdbcTemplate.queryForObject("SELECT id FROM core.accounts WHERE account_number = ?", UUID.class, accountNumber);
        if (id == null) {
            throw new IllegalArgumentException("Unknown account");
        }
        return id;
    }

    private void ensureSettlementAccount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM core.accounts WHERE account_number = ?", Integer.class, SETTLEMENT_ACCOUNT);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO core.accounts(account_number, kind, currency, status) VALUES (?, 'SETTLEMENT', 'KES', 'ACTIVE')", SETTLEMENT_ACCOUNT);
        }
    }
}
