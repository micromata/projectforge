-- LiquidityEntryDO.paid becomes a three-state override: true = forced paid, false = forced unpaid,
-- null = "automatic" (paid status then follows auto_set_paid + date_of_payment, see effectivePaid).
-- The column was NOT NULL; drop that so the "automatic" (null) state can be stored (PostgreSQL syntax).
ALTER TABLE t_plugin_liqui_entry ALTER COLUMN paid DROP NOT NULL;

-- Rollback:
-- UPDATE t_plugin_liqui_entry SET paid = FALSE WHERE paid IS NULL;
-- ALTER TABLE t_plugin_liqui_entry ALTER COLUMN paid SET NOT NULL;
-- DELETE FROM t_flyway_liquidplanning_schema_version WHERE version = '1.0.1';
