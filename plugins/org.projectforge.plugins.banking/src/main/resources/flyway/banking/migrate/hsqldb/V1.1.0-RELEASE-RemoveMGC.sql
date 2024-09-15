ALTER TABLE t_plugin_banking_account ALTER COLUMN pk bigint;
ALTER TABLE t_plugin_banking_account_balance ALTER COLUMN pk bigint;
ALTER TABLE t_plugin_banking_account_record ALTER COLUMN pk bigint;

ALTER TABLE t_plugin_banking_account_balance ALTER COLUMN banking_account_fk bigint;
ALTER TABLE t_plugin_banking_account_record ALTER COLUMN banking_account_fk bigint;
