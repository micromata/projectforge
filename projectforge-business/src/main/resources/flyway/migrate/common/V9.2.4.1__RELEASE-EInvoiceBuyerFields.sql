-- Buyer details of the e-invoice: order number (BT-13), department of the orderer (BT-56), cost center (BT-19).
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_order_number VARCHAR(255);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_contact_department VARCHAR(255);
ALTER TABLE t_fibu_rechnung ADD COLUMN customer_cost_center VARCHAR(255);
ALTER TABLE T_FIBU_KONTO ADD COLUMN contact_department VARCHAR(255);
ALTER TABLE T_FIBU_KONTO ADD COLUMN cost_center VARCHAR(255);

-- Rollback:
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_order_number;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_contact_department;
-- ALTER TABLE t_fibu_rechnung DROP COLUMN IF EXISTS customer_cost_center;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS contact_department;
-- ALTER TABLE T_FIBU_KONTO DROP COLUMN IF EXISTS cost_center;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.4.1';
