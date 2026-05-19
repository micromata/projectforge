-- KontoDO: E-Invoice fields
ALTER TABLE T_FIBU_KONTO ADD COLUMN vat_id VARCHAR(20);
ALTER TABLE T_FIBU_KONTO ADD COLUMN street VARCHAR(255);
ALTER TABLE T_FIBU_KONTO ADD COLUMN zip_code VARCHAR(10);
ALTER TABLE T_FIBU_KONTO ADD COLUMN city VARCHAR(100);
ALTER TABLE T_FIBU_KONTO ADD COLUMN country VARCHAR(2);
ALTER TABLE T_FIBU_KONTO ADD COLUMN leitweg_id VARCHAR(50);
ALTER TABLE T_FIBU_KONTO ADD COLUMN e_invoice_email VARCHAR(255);
ALTER TABLE T_FIBU_KONTO ADD COLUMN contact_person VARCHAR(255);
ALTER TABLE T_FIBU_KONTO ADD COLUMN seller_bank_account_name VARCHAR(100);

-- RechnungDO: Structured customer address and seller bank account for E-Invoice export
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_contact_person VARCHAR(255);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_zip_code VARCHAR(10);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_city VARCHAR(100);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_country VARCHAR(2);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_vat_id VARCHAR(20);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_leitweg_id VARCHAR(50);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_e_invoice_email VARCHAR(255);
ALTER TABLE t_fibu_rechnung ADD COLUMN seller_bank_account VARCHAR(34);

-- RechnungDO: JCR attachment columns
ALTER TABLE t_fibu_rechnung ADD COLUMN attachments_names VARCHAR(10000);
ALTER TABLE t_fibu_rechnung ADD COLUMN attachments_ids VARCHAR(10000);
ALTER TABLE t_fibu_rechnung ADD COLUMN attachments_counter INTEGER;
ALTER TABLE t_fibu_rechnung ADD COLUMN attachments_size BIGINT;
ALTER TABLE t_fibu_rechnung ADD COLUMN attachments_last_user_action VARCHAR(10000);

-- Rollback:
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS vat_id;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS street;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS zip_code;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS city;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS country;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS leitweg_id;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS e_invoice_email;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS contact_person;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS seller_bank_account_name;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_contact_person;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_zip_code;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_city;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_country;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_vat_id;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_leitweg_id;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_e_invoice_email;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS seller_bank_account;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS attachments_names;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS attachments_ids;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS attachments_counter;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS attachments_size;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS attachments_last_user_action;
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.19';
