# GreenDay Banking Core

GreenDay Banking Core is a Java/Spring Boot banking-core prototype for Kenyan financial institutions. It is being upgraded from a console banking demo into a ledger-first banking foundation with encrypted customer records, auditable transactions, strict money movement invariants, and PostgreSQL-backed persistence.

This repository is not yet production-certified banking software. It is a serious technical baseline for bank review, pilot scoping, and architecture validation.

## Current bank-grade upgrades

- Spring Boot 3 backend runtime.
- PostgreSQL schema with Flyway migration.
- Ledger-first transaction model.
- Immutable ledger entries.
- Double-entry transfer posting.
- KES-first money type using `BigDecimal` and `HALF_EVEN` rounding.
- Customer profile model separated from financial ledger.
- Twofish encrypted customer vault using Bouncy Castle.
- HMAC-SHA256 integrity protection for encrypted records.
- PBKDF2 key derivation.
- Database-backed audit event repository.
- Database-backed customer repository.
- Database-backed ledger posting repository.
- High-entropy customer, account, transaction, idempotency, and audit references.
- Safe API exception handling.
- Docker Compose PostgreSQL runtime.
- JUnit tests for encryption, tamper detection, transfers, insufficient funds, and currency mismatch.

## Run locally

```bash
docker compose up -d
mvn spring-boot:run
```

Required production override:

```bash
export GREENDAY_VAULT_PASSPHRASE='use-a-real-secret-from-a-bank-kms-or-hsm'
export GREENDAY_VAULT_KEY_REFERENCE='bank-kms-key-reference'
```

## API surface

### Health

```bash
curl http://localhost:8080/api/v1/health
```

### Open account

The private profile payload is intentionally a map. It is encrypted before storage. Do not send real customer data to a dev laptop.

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{
    "actor": "operations-user",
    "privateProfile": {
      "displayName": "Pilot Customer",
      "identityRef": "internal-identity-reference",
      "contactRef": "internal-contact-reference",
      "emailRef": "internal-email-reference",
      "taxRef": "internal-tax-reference"
    }
  }'
```

### Credit an account

```bash
curl -X POST http://localhost:8080/api/v1/ledger/credits \
  -H 'Content-Type: application/json' \
  -d '{
    "accountNumber": "ACCOUNT_FROM_OPEN_ACCOUNT_RESPONSE",
    "amount": 1000.00,
    "narration": "Opening credit",
    "idempotencyKey": "IK_CLIENT_GENERATED_HIGH_ENTROPY_KEY_0001",
    "actor": "operations-user"
  }'
```

### Move funds between accounts

```bash
curl -X POST http://localhost:8080/api/v1/ledger/movements \
  -H 'Content-Type: application/json' \
  -d '{
    "fromAccountNumber": "SOURCE_ACCOUNT",
    "toAccountNumber": "DESTINATION_ACCOUNT",
    "amount": 250.00,
    "narration": "Customer transfer",
    "idempotencyKey": "IK_CLIENT_GENERATED_HIGH_ENTROPY_KEY_0002",
    "actor": "operations-user"
  }'
```

### Check balance

```bash
curl http://localhost:8080/api/v1/accounts/ACCOUNT_NUMBER/balance
```

## Real use cases

### 1. Customer onboarding
A bank officer captures private customer profile material. The sensitive profile payload is encrypted using Twofish before storage. The ledger account can exist without exposing private profile material to normal transaction queries.

### 2. Deposit or controlled account credit
Cash, agency, teller, settlement, or integration credits can be posted as balanced ledger movements from a settlement/general-ledger account into a customer account.

### 3. Customer-to-customer movement
A movement debits the sender and credits the receiver in one balanced transaction. If the sender has insufficient funds, the whole transaction is rejected before posting.

### 4. Audit review
Operational events such as onboarding, profile storage, credits, and movements are written to the audit log. This gives bank reviewers a starting point for traceability.

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

Production recommendation: replace passphrase custody with bank-grade HSM/KMS-backed key management. Do not keep vault passphrases in source code, logs, GitHub Actions, local shell history, or committed `.env` files.

## Current limitations before bank pilot

The system still needs these before any real-money pilot:

- Strong authentication and MFA.
- Role-based access control.
- Maker-checker workflows.
- HSM/KMS integration.
- Full idempotent response replay for duplicate keys.
- Transaction reversal workflow.
- Daily reconciliation reports.
- Regulatory reporting/export layer.
- Observability, alerting, rate limits, and fraud rules.
- Formal security review and penetration test.
- Data Protection Act compliance review.
- CBK-aligned ICT risk assessment.

## One-week bank-demo path

Day 1: ledger, encryption, audit trail, tests.
Day 2: REST API for onboarding, balance, account credit, account movement.
Day 3: PostgreSQL persistence hardening and integration tests.
Day 4: RBAC, maker-checker, idempotency replay, audit export.
Day 5: web dashboard for teller/admin/customer demo.
Day 6: deployment, controlled demo data, threat model, architecture diagrams.
Day 7: bank pitch pack, live demo script, risk register, pilot proposal.
