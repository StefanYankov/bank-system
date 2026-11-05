CREATE SEQUENCE account_number_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 99999999
    CACHE 1
    NO CYCLE;

CREATE TABLE accounts
(
    id                UUID                        NOT NULL,
    version           BIGINT                      NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by        VARCHAR(255)                NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_by        VARCHAR(255)                NOT NULL,
    iban              VARCHAR(22)                 NOT NULL,
    owner_customer_id UUID                        NOT NULL,
    account_type      VARCHAR(16)                 NOT NULL,
    currency          VARCHAR(3)                  NOT NULL,
    balance           NUMERIC(19, 2)              NOT NULL,
    status            VARCHAR(16)                 NOT NULL,
    CONSTRAINT pk_accounts
        PRIMARY KEY (id),
    CONSTRAINT uc_accounts_iban
        UNIQUE (iban),
    CONSTRAINT ck_accounts_account_type
        CHECK (account_type IN ('CURRENT', 'SAVINGS')),
    CONSTRAINT ck_accounts_currency
        CHECK (currency IN ('EUR')),
    CONSTRAINT ck_accounts_status
        CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT ck_accounts_balance_not_negative
        CHECK (balance >= 0)
);

CREATE INDEX ix_accounts_owner_customer_id
    ON accounts (owner_customer_id);

CREATE TABLE account_transactions
(
    id                UUID                        NOT NULL,
    version           BIGINT                      NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by        VARCHAR(255)                NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_by        VARCHAR(255)                NOT NULL,
    account_id        UUID                        NOT NULL,
    transaction_type  VARCHAR(16)                 NOT NULL,
    amount            NUMERIC(19, 2)              NOT NULL,
    currency          VARCHAR(3)                  NOT NULL,
    resulting_balance NUMERIC(19, 2)              NOT NULL,
    reference         VARCHAR(140)                NOT NULL,
    CONSTRAINT pk_account_transactions
        PRIMARY KEY (id),
    CONSTRAINT fk_account_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT ck_account_transactions_transaction_type
        CHECK (transaction_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_account_transactions_currency
        CHECK (currency IN ('EUR')),
    CONSTRAINT ck_account_transactions_amount_positive
        CHECK (amount > 0),
    CONSTRAINT ck_account_transactions_resulting_balance_not_negative
        CHECK (resulting_balance >= 0)
);

CREATE INDEX ix_account_transactions_account_id_created_at
    ON account_transactions (account_id, created_at);
