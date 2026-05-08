# GreenDay Banking Core Validation Checklist

This branch must be treated as untrusted until the following validation passes on a clean machine.

## Required local validation

```bash
git checkout bank-grade-ledger-twofish
docker --version
docker compose up -d
mvn -B test
mvn spring-boot:run
```

## Required API smoke test

1. `GET /api/v1/health` returns `UP`.
2. `POST /api/v1/accounts` opens two accounts.
3. `POST /api/v1/ledger/credits` credits source account with an idempotency key.
4. Repeat the same credit request with the same idempotency key and same body; it must replay, not post again.
5. Repeat same key with a different body; it must reject.
6. `POST /api/v1/ledger/movements` below KES 100,000 posts directly.
7. `POST /api/v1/ledger/movements` at or above KES 100,000 returns `APPROVAL_REQUIRED` and an `approvalId`.
8. Same actor approval must fail.
9. Different checker approval must succeed.
10. Approved operation execution must post exactly once.
11. Second execution attempt must fail.
12. Rejected operation must never execute.
13. `GET /api/v1/accounts/{accountNumber}/statement` must show posted ledger lines with running balance.

## Database invariants to inspect

```sql
SELECT * FROM core.account_balances;
SELECT transaction_id, currency,
       SUM(CASE WHEN side = 'DEBIT' THEN amount ELSE 0 END) AS debits,
       SUM(CASE WHEN side = 'CREDIT' THEN amount ELSE 0 END) AS credits
FROM core.ledger_entries
GROUP BY transaction_id, currency
HAVING SUM(CASE WHEN side = 'DEBIT' THEN amount ELSE 0 END) <> SUM(CASE WHEN side = 'CREDIT' THEN amount ELSE 0 END);
```

The second query must return zero rows.

## Known non-production gaps

- No real authentication/MFA yet.
- No HSM/KMS integration yet.
- No staff-user bootstrap workflow yet.
- No formal RBAC enforcement on endpoints yet.
- No audit log hash-chain yet.
- No backup/restore drill yet.
- No reconciliation import/export yet.
- No deployment hardening yet.
- No penetration test yet.
