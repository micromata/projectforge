ALTER TABLE t_plugin_banking_account ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_banking_account_balance ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_banking_account_record ALTER COLUMN pk TYPE bigint;

ALTER TABLE t_plugin_banking_account_balance ALTER COLUMN banking_account_fk TYPE bigint;
ALTER TABLE t_plugin_banking_account_record ALTER COLUMN banking_account_fk TYPE bigint;
