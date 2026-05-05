-- KundeDO: E-Invoice fields
ALTER TABLE T_FIBU_KUNDE ADD COLUMN vat_id VARCHAR(20);
ALTER TABLE T_FIBU_KUNDE ADD COLUMN street VARCHAR(255);
ALTER TABLE T_FIBU_KUNDE ADD COLUMN zip_code VARCHAR(10);
ALTER TABLE T_FIBU_KUNDE ADD COLUMN city VARCHAR(100);
ALTER TABLE T_FIBU_KUNDE ADD COLUMN country VARCHAR(2);
ALTER TABLE T_FIBU_KUNDE ADD COLUMN leitweg_id VARCHAR(50);
ALTER TABLE T_FIBU_KUNDE ADD COLUMN e_invoice_email VARCHAR(255);

-- RechnungDO: Structured customer address and seller bank account for E-Invoice export
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_zip_code VARCHAR(10);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_city VARCHAR(100);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_country VARCHAR(2);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_vat_id VARCHAR(20);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_leitweg_id VARCHAR(50);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_e_invoice_email VARCHAR(255);
ALTER TABLE t_fibu_rechnung ADD COLUMN seller_bank_account VARCHAR(34);

-- Rollback:
-- ALTER TABLE T_FIBU_KUNDE DROP COLUMN IF EXISTS vat_id;
-- ALTER TABLE T_FIBU_KUNDE DROP COLUMN IF EXISTS street;
-- ALTER TABLE T_FIBU_KUNDE DROP COLUMN IF EXISTS zip_code;
-- ALTER TABLE T_FIBU_KUNDE DROP COLUMN IF EXISTS city;
-- ALTER TABLE T_FIBU_KUNDE DROP COLUMN IF EXISTS country;
-- ALTER TABLE T_FIBU_KUNDE DROP COLUMN IF EXISTS leitweg_id;
-- ALTER TABLE T_FIBU_KUNDE DROP COLUMN IF EXISTS e_invoice_email;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_zip_code;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_city;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_country;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_vat_id;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_leitweg_id;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_e_invoice_email;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS seller_bank_account;
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.19';
