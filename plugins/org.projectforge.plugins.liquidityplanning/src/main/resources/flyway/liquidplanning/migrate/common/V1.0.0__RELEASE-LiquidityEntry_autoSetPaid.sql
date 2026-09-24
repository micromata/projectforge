-- LiquidityEntryDO: automatically treat entry as paid once the date of payment has passed.
-- For specific database dialects, place scripts in migrate/{vendor}; this ADD COLUMN works on
-- both PostgreSQL (prod) and HSQLDB (test), so it lives in migrate/common.
ALTER TABLE t_plugin_liqui_entry ADD COLUMN auto_set_paid BOOLEAN DEFAULT FALSE;

-- Rollback:
-- ALTER TABLE t_plugin_liqui_entry DROP COLUMN IF EXISTS auto_set_paid;
-- DELETE FROM t_flyway_liquidplanning_schema_version WHERE version = '1.0.0';
