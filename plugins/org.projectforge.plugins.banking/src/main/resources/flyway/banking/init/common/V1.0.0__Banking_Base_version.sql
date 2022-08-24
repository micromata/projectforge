-- This is the initial script for setting up the data base for this plugin.
-- For specific data base dialects, place sql scripts in the sub directory init/{vendor}

-- Drop old tables of former plugin (never in use):
DROP TABLE t_plugin_bank_account_record;
DROP TABLE t_plugin_bank_account_balance;
DROP TABLE t_plugin_bank_account;

CREATE TABLE t_plugin_banking_account
(
    pk                        INTEGER                      NOT NULL,
    created                   TIMESTAMP WITHOUT TIME ZONE,
    deleted                   BOOLEAN                      NOT NULL,
    last_update               TIMESTAMP WITHOUT TIME ZONE,
    name                      CHARACTER VARYING(1000)      NOT NULL,
    description               CHARACTER VARYING(4000),
    iban                      CHARACTER VARYING(1000),
    bic                       CHARACTER VARYING(1000),
    bank                      CHARACTER VARYING(1000)      NOT NULL,
    full_access_group_ids     CHARACTER VARYING(255),
    full_access_user_ids      CHARACTER VARYING(255),
    minimal_access_group_ids  CHARACTER VARYING(255),
    minimal_access_user_ids   CHARACTER VARYING(255),
    readonly_access_group_ids CHARACTER VARYING(255),
    readonly_access_user_ids  CHARACTER VARYING(255),
    title                     CHARACTER VARYING(1000),
    import_settings           CHARACTER VARYING(10000)
);

ALTER TABLE t_plugin_banking_account
    ADD CONSTRAINT t_plugin_banking_account_pkey PRIMARY KEY (pk);

CREATE TABLE t_plugin_banking_account_record
(
    pk                        INTEGER                      NOT NULL,
    created                   TIMESTAMP WITHOUT TIME ZONE,
    deleted                   BOOLEAN                      NOT NULL,
    last_update               TIMESTAMP WITHOUT TIME ZONE,
    date                      DATE                         NOT NULL,
    value_date                DATE                         NOT NULL,
    amount                    NUMERIC(18, 5) NOT NULL,
    subject                   CHARACTER VARYING(4000),
    comment                   CHARACTER VARYING(4000),
    currency                  CHARACTER VARYING(4000),
    debtee_id                 CHARACTER VARYING(4000),
    mandate_reference         CHARACTER VARYING(4000),
    customer_reference        CHARACTER VARYING(4000),
    collection_reference      CHARACTER VARYING(4000),
    info                      CHARACTER VARYING(4000),
    receiver_sender           CHARACTER VARYING(4000),
    type                      CHARACTER VARYING(4000),
    iban                      CHARACTER VARYING(1000),
    bic                       CHARACTER VARYING(1000),
    checksum                  CHARACTER VARYING(255),
    bank                      CHARACTER VARYING(1000),
    banking_account_fk        INTEGER                     NOT NULL
);

ALTER TABLE t_plugin_banking_account_record
    ADD CONSTRAINT t_plugin_banking_account_record_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_banking_account_record_fk_account
    ON t_plugin_banking_account_record (banking_account_fk);

ALTER TABLE t_plugin_banking_account_record
    ADD CONSTRAINT t_plugin_banking_account_record_fk_account FOREIGN KEY (banking_account_fk) REFERENCES t_plugin_banking_account (pk);
