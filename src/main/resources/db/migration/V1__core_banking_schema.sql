-- GreenDay Banking Core - PostgreSQL schema blueprint
-- No mock customer data. This file defines production-oriented structures only.
-- Target: PostgreSQL 15+

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS security;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS approvals;
CREATE SCHEMA IF NOT EXISTS compliance;

CREATE TYPE core.account_status AS ENUM ('PENDING_KYC', 'ACTIVE', 'FROZEN', 'DORMANT', 'CLOSED');
CREATE TYPE core.account_kind AS ENUM ('CUSTOMER_DEPOSIT', 'SUSPENSE', 'SETTLEMENT', 'FEE_INCOME', 'TAX_PAYABLE', 'INTERNAL_GL');
CREATE TYPE core.ledger_side AS ENUM ('DEBIT', 'CREDIT');
CREATE TYPE core.transaction_status AS ENUM ('PENDING', 'POSTED', 'REVERSED', 'REJECTED');
CREATE TYPE security.user_status AS ENUM ('ACTIVE', 'LOCKED', 'DISABLED');
CREATE TYPE approvals.approval_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');

CREATE TABLE security.roles (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name TEXT NOT NULL UNIQUE, description TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE security.permissions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code TEXT NOT NULL UNIQUE, description TEXT NOT NULL);
CREATE TABLE security.role_permissions (role_id UUID NOT NULL REFERENCES security.roles(id) ON DELETE CASCADE, permission_id UUID NOT NULL REFERENCES security.permissions(id) ON DELETE CASCADE, PRIMARY KEY (role_id, permission_id));
CREATE TABLE security.staff_users (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), staff_number TEXT NOT NULL UNIQUE, full_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, status security.user_status NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMPTZ NOT NULL DEFAULT now(), last_login_at TIMESTAMPTZ);
CREATE TABLE security.staff_user_roles (staff_user_id UUID NOT NULL REFERENCES security.staff_users(id) ON DELETE CASCADE, role_id UUID NOT NULL REFERENCES security.roles(id) ON DELETE CASCADE, PRIMARY KEY (staff_user_id, role_id));

CREATE TABLE core.customers (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), customer_ref TEXT NOT NULL UNIQUE, status core.account_status NOT NULL DEFAULT 'PENDING_KYC', created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE core.customer_encrypted_profiles (customer_id UUID PRIMARY KEY REFERENCES core.customers(id) ON DELETE CASCADE, vault_version TEXT NOT NULL, encryption_algorithm TEXT NOT NULL CHECK (encryption_algorithm = 'Twofish/CBC/PKCS7Padding'), key_reference TEXT NOT NULL, encrypted_payload TEXT NOT NULL, payload_hmac TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE core.customer_kyc_documents (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), customer_id UUID NOT NULL REFERENCES core.customers(id), document_type TEXT NOT NULL, document_reference_hash TEXT NOT NULL, storage_uri TEXT NOT NULL, verification_status TEXT NOT NULL DEFAULT 'PENDING', verified_by UUID REFERENCES security.staff_users(id), verified_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now());

CREATE TABLE core.accounts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), account_number TEXT NOT NULL UNIQUE, customer_id UUID REFERENCES core.customers(id), kind core.account_kind NOT NULL, currency CHAR(3) NOT NULL DEFAULT 'KES', status core.account_status NOT NULL DEFAULT 'ACTIVE', opened_at TIMESTAMPTZ NOT NULL DEFAULT now(), closed_at TIMESTAMPTZ, CHECK (currency ~ '^[A-Z]{3}$'));
CREATE INDEX idx_accounts_customer_id ON core.accounts(customer_id);
CREATE INDEX idx_accounts_status ON core.accounts(status);

CREATE TABLE core.transactions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), transaction_ref TEXT NOT NULL UNIQUE, external_ref TEXT UNIQUE, idempotency_key TEXT UNIQUE, status core.transaction_status NOT NULL DEFAULT 'PENDING', transaction_type TEXT NOT NULL, narration TEXT NOT NULL, requested_by UUID REFERENCES security.staff_users(id), posted_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), reversed_transaction_id UUID REFERENCES core.transactions(id));
CREATE INDEX idx_transactions_status ON core.transactions(status);
CREATE INDEX idx_transactions_created_at ON core.transactions(created_at);
CREATE INDEX idx_transactions_idempotency_key ON core.transactions(idempotency_key);

CREATE TABLE core.ledger_entries (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), transaction_id UUID NOT NULL REFERENCES core.transactions(id), account_id UUID NOT NULL REFERENCES core.accounts(id), side core.ledger_side NOT NULL, amount NUMERIC(20, 2) NOT NULL CHECK (amount > 0), currency CHAR(3) NOT NULL, narration TEXT NOT NULL, posted_at TIMESTAMPTZ NOT NULL DEFAULT now(), sequence_no BIGSERIAL UNIQUE, CHECK (currency ~ '^[A-Z]{3}$'));
CREATE INDEX idx_ledger_entries_transaction_id ON core.ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account_id ON core.ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_posted_at ON core.ledger_entries(posted_at);

CREATE VIEW core.account_balances AS SELECT a.id AS account_id, a.account_number, a.currency, COALESCE(SUM(CASE WHEN le.side = 'CREDIT' THEN le.amount ELSE -le.amount END), 0.00) AS balance FROM core.accounts a LEFT JOIN core.ledger_entries le ON le.account_id = a.id GROUP BY a.id, a.account_number, a.currency;

CREATE OR REPLACE FUNCTION core.assert_transaction_balanced(p_transaction_id UUID) RETURNS VOID AS $$ DECLARE imbalance_count INT; BEGIN SELECT COUNT(*) INTO imbalance_count FROM (SELECT currency, SUM(CASE WHEN side = 'DEBIT' THEN amount ELSE 0 END) AS debits, SUM(CASE WHEN side = 'CREDIT' THEN amount ELSE 0 END) AS credits FROM core.ledger_entries WHERE transaction_id = p_transaction_id GROUP BY currency HAVING SUM(CASE WHEN side = 'DEBIT' THEN amount ELSE 0 END) <> SUM(CASE WHEN side = 'CREDIT' THEN amount ELSE 0 END)) imbalances; IF imbalance_count > 0 THEN RAISE EXCEPTION 'Unbalanced transaction %', p_transaction_id; END IF; END; $$ LANGUAGE plpgsql;

CREATE TABLE approvals.approval_requests (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), resource_type TEXT NOT NULL, resource_id UUID NOT NULL, action TEXT NOT NULL, status approvals.approval_status NOT NULL DEFAULT 'PENDING', requested_by UUID NOT NULL REFERENCES security.staff_users(id), reviewed_by UUID REFERENCES security.staff_users(id), reviewed_at TIMESTAMPTZ, rejection_reason TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), CHECK (requested_by IS DISTINCT FROM reviewed_by));
CREATE INDEX idx_approval_requests_status ON approvals.approval_requests(status);
CREATE INDEX idx_approval_requests_resource ON approvals.approval_requests(resource_type, resource_id);

CREATE TABLE audit.events (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(), actor_user_id UUID REFERENCES security.staff_users(id), actor_label TEXT NOT NULL, action TEXT NOT NULL, resource_type TEXT NOT NULL, resource_id TEXT NOT NULL, ip_address INET, user_agent TEXT, metadata JSONB NOT NULL DEFAULT '{}'::jsonb);
CREATE INDEX idx_audit_events_occurred_at ON audit.events(occurred_at);
CREATE INDEX idx_audit_events_action ON audit.events(action);
CREATE INDEX idx_audit_events_resource ON audit.events(resource_type, resource_id);

CREATE TABLE compliance.aml_screening_cases (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), customer_id UUID NOT NULL REFERENCES core.customers(id), screening_provider TEXT NOT NULL, screening_reference TEXT NOT NULL, risk_score NUMERIC(5, 2), status TEXT NOT NULL DEFAULT 'PENDING', decision TEXT, reviewed_by UUID REFERENCES security.staff_users(id), reviewed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE compliance.suspicious_activity_reports (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), customer_id UUID REFERENCES core.customers(id), account_id UUID REFERENCES core.accounts(id), transaction_id UUID REFERENCES core.transactions(id), reason TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'DRAFT', filed_by UUID REFERENCES security.staff_users(id), filed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT now());

CREATE TABLE core.reconciliation_runs (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), business_date DATE NOT NULL, source_system TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'STARTED', started_at TIMESTAMPTZ NOT NULL DEFAULT now(), completed_at TIMESTAMPTZ, mismatch_count INT NOT NULL DEFAULT 0, notes TEXT);
CREATE TABLE core.reconciliation_items (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), run_id UUID NOT NULL REFERENCES core.reconciliation_runs(id) ON DELETE CASCADE, account_id UUID REFERENCES core.accounts(id), transaction_ref TEXT, expected_amount NUMERIC(20, 2), actual_amount NUMERIC(20, 2), status TEXT NOT NULL, notes TEXT);
