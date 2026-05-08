# GreenDay Banking Core

GreenDay Banking Core is a Java banking-core prototype for Kenyan financial institutions. It is being upgraded from a console banking demo into a ledger-first banking foundation with encrypted customer records, auditable transactions, and strict money movement invariants.

This repository is not yet production-certified banking software. It is a serious technical baseline for bank review, pilot scoping, and architecture validation.

## Current bank-grade upgrades

- Maven Java 21 project structure.
- Ledger-first transaction model.
- Immutable ledger entries.
- Double-entry transfer posting.
- KES-first money type using `BigDecimal` and `HALF_EVEN` rounding.
- Customer profile model separated from financial ledger.
- Twofish encrypted customer vault using Bouncy Castle.
- HMAC-SHA256 integrity protection for encrypted records.
- PBKDF2 key derivation.
- Append-only in-memory audit event model.
- JUnit tests for encryption, tamper detection, transfers, insufficient funds, and currency mismatch.

## Real use cases

### 1. Customer onboarding
A bank officer captures customer details such as full name, phone number, national ID, email, and KRA PIN. The sensitive profile payload is encrypted using Twofish before storage. The ledger account can exist without exposing PII to normal transaction queries.

### 2. Deposit posting
Cash, agency, teller, or integration deposits can be posted as balanced ledger movements from a suspense/general-ledger account into a customer account.

### 3. Customer-to-customer transfer
A transfer debits the sender and credits the receiver in one balanced transaction. If the sender has insufficient funds, the whole transaction is rejected before state mutation.

### 4. Audit review
Operational events such as onboarding, customer profile reads, deposits, and transfers are written to the audit log. This gives bank reviewers a starting point for traceability.

### 5. Encrypted record retrieval
Authorized bank systems can decrypt customer profiles with the vault passphrase. Wrong keys and tampered payloads are rejected.

## Twofish design

The vault uses:

- `Twofish/CBC/PKCS7Padding`
- 256-bit encryption key material
- random 16-byte salt per encrypted record
- random 16-byte IV per encrypted record
- `PBKDF2WithHmacSHA256` with 210,000 iterations
- `HmacSHA256` integrity check over version, salt, IV, and ciphertext

Production recommendation: replace passphrase custody with bank-grade HSM/KMS-backed key management. Do not keep vault passphrases in source code, logs, GitHub Actions, local shell history, or `.env` committed files.

## Run tests

```bash
mvn test
```

## Current limitations before bank pilot

The system still needs these before any real-money pilot:

- Persistent database with migrations.
- Role-based access control.
- Maker-checker workflows.
- Proper authentication and MFA.
- HSM/KMS integration.
- API layer with request signing and idempotency keys.
- Transaction reversal workflow.
- Daily reconciliation reports.
- Regulatory reporting/export layer.
- Observability, alerting, rate limits, and fraud rules.
- Formal security review and penetration test.
- Data Protection Act compliance review.
- CBK-aligned ICT risk assessment.

## One-week bank-demo path

Day 1: ledger, encryption, audit trail, tests.
Day 2: REST API for onboarding, balance, deposit, transfer, statement.
Day 3: database persistence with PostgreSQL.
Day 4: RBAC, maker-checker, idempotency, audit export.
Day 5: web dashboard for teller/admin/customer demo.
Day 6: deployment, seed demo data, threat model, architecture diagrams.
Day 7: bank pitch pack, live demo script, risk register, pilot proposal.
