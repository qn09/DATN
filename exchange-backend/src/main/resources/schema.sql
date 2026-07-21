CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    password_hash TEXT,
    role TEXT NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE accounts ADD COLUMN IF NOT EXISTS password_hash TEXT;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS role TEXT NOT NULL DEFAULT 'USER';

CREATE TABLE IF NOT EXISTS wallet_balances (
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    asset TEXT NOT NULL,
    available NUMERIC(38, 18) NOT NULL DEFAULT 0,
    locked NUMERIC(38, 18) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, asset)
);

CREATE TABLE IF NOT EXISTS fiat_deposit_requests (
    id BIGSERIAL PRIMARY KEY,
    request_id TEXT NOT NULL UNIQUE,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    currency TEXT NOT NULL,
    amount NUMERIC(38, 18) NOT NULL CHECK (amount > 0),
    status TEXT NOT NULL CHECK (status IN ('NEW', 'PROCESSING', 'SUCCESS', 'FAILED')),
    gateway TEXT NOT NULL,
    gateway_reference TEXT UNIQUE,
    client_request_key TEXT NOT NULL UNIQUE,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processing_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fiat_deposit_account_id
    ON fiat_deposit_requests(account_id, id DESC);

CREATE INDEX IF NOT EXISTS idx_fiat_deposit_status
    ON fiat_deposit_requests(status, updated_at);

CREATE TABLE IF NOT EXISTS ledger_accounts (
    id BIGSERIAL PRIMARY KEY,
    owner_account_id BIGINT REFERENCES accounts(id),
    asset TEXT NOT NULL,
    account_type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ledger_account_owner CHECK (
        (account_type IN ('USER_AVAILABLE', 'USER_LOCKED') AND owner_account_id IS NOT NULL)
        OR
        (account_type IN ('SYSTEM_EXTERNAL', 'SYSTEM_MARKET') AND owner_account_id IS NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_user_account
    ON ledger_accounts(owner_account_id, asset, account_type)
    WHERE owner_account_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_system_account
    ON ledger_accounts(asset, account_type)
    WHERE owner_account_id IS NULL;

CREATE TABLE IF NOT EXISTS ledger_transactions (
    id BIGSERIAL PRIMARY KEY,
    reference_type TEXT NOT NULL,
    reference_id TEXT NOT NULL,
    idempotency_key TEXT NOT NULL UNIQUE,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES ledger_transactions(id),
    ledger_account_id BIGINT NOT NULL REFERENCES ledger_accounts(id),
    direction TEXT NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(38, 18) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_transaction
    ON ledger_entries(transaction_id);

CREATE INDEX IF NOT EXISTS idx_ledger_accounts_owner
    ON ledger_accounts(owner_account_id);

CREATE TABLE IF NOT EXISTS exchange_orders (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    symbol TEXT NOT NULL,
    side TEXT NOT NULL,
    price NUMERIC(38, 18) NOT NULL,
    original_quantity NUMERIC(38, 18) NOT NULL,
    remaining_quantity NUMERIC(38, 18) NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_exchange_orders_account_id ON exchange_orders(account_id);
CREATE INDEX IF NOT EXISTS idx_exchange_orders_symbol_status ON exchange_orders(symbol, status);

CREATE TABLE IF NOT EXISTS trades (
    id BIGSERIAL PRIMARY KEY,
    symbol TEXT NOT NULL,
    buy_order_id BIGINT REFERENCES exchange_orders(id),
    sell_order_id BIGINT REFERENCES exchange_orders(id),
    price NUMERIC(38, 18) NOT NULL,
    quantity NUMERIC(38, 18) NOT NULL,
    source TEXT NOT NULL DEFAULT 'INTERNAL',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE trades ALTER COLUMN buy_order_id DROP NOT NULL;
ALTER TABLE trades ALTER COLUMN sell_order_id DROP NOT NULL;
ALTER TABLE trades ADD COLUMN IF NOT EXISTS source TEXT NOT NULL DEFAULT 'INTERNAL';

CREATE INDEX IF NOT EXISTS idx_trades_symbol_id ON trades(symbol, id);
