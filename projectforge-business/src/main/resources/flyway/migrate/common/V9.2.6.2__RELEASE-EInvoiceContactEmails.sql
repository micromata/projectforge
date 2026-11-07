-- Contact emails of the e-invoice: buyer contact (BT-58, formerly shared with BT-49), seller contact (BT-43).
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_contact_email VARCHAR(255);
ALTER TABLE t_fibu_rechnung ADD COLUMN seller_contact_email VARCHAR(255);
ALTER TABLE T_FIBU_KONTO ADD COLUMN contact_email VARCHAR(255);

-- Rollback:
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_contact_email;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS seller_contact_email;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS contact_email;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.6.2';
