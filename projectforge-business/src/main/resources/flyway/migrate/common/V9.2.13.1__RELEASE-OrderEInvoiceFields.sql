-- Fields of an order the invoices of it are prefilled with: customer order number (BT-13), contract number (BT-12),
-- cost center (BT-19), buyer contact point (BT-56, BT-58); the same order number and cost center per position;
-- service period (BT-73, BT-74) of a payment schedule entry.
ALTER TABLE t_fibu_auftrag ADD COLUMN customer_order_number VARCHAR(255);
ALTER TABLE t_fibu_auftrag ADD COLUMN contract_number VARCHAR(255);
ALTER TABLE t_fibu_auftrag ADD COLUMN customer_cost_center VARCHAR(255);
ALTER TABLE t_fibu_auftrag ADD COLUMN customer_contact_person VARCHAR(255);
ALTER TABLE t_fibu_auftrag ADD COLUMN customer_contact_department VARCHAR(255);
ALTER TABLE t_fibu_auftrag ADD COLUMN customer_contact_email VARCHAR(255);
ALTER TABLE t_fibu_auftrag_position ADD COLUMN customer_order_number VARCHAR(255);
ALTER TABLE t_fibu_auftrag_position ADD COLUMN customer_cost_center VARCHAR(255);
ALTER TABLE t_fibu_payment_schedule ADD COLUMN period_of_performance_begin DATE;
ALTER TABLE t_fibu_payment_schedule ADD COLUMN period_of_performance_end DATE;

-- Rollback:
-- ALTER TABLE t_fibu_payment_schedule DROP COLUMN IF EXISTS period_of_performance_end;
-- ALTER TABLE t_fibu_payment_schedule DROP COLUMN IF EXISTS period_of_performance_begin;
-- ALTER TABLE t_fibu_auftrag_position DROP COLUMN IF EXISTS customer_cost_center;
-- ALTER TABLE t_fibu_auftrag_position DROP COLUMN IF EXISTS customer_order_number;
-- ALTER TABLE t_fibu_auftrag DROP COLUMN IF EXISTS customer_contact_email;
-- ALTER TABLE t_fibu_auftrag DROP COLUMN IF EXISTS customer_contact_department;
-- ALTER TABLE t_fibu_auftrag DROP COLUMN IF EXISTS customer_contact_person;
-- ALTER TABLE t_fibu_auftrag DROP COLUMN IF EXISTS customer_cost_center;
-- ALTER TABLE t_fibu_auftrag DROP COLUMN IF EXISTS contract_number;
-- ALTER TABLE t_fibu_auftrag DROP COLUMN IF EXISTS customer_order_number;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.13.1';
