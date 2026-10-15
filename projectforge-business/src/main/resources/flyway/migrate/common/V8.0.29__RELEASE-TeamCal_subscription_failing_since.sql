-- TeamCalDO: start of an uninterrupted series of failed updates of a subscribed calendar. Persisted, so
-- permanently failing subscriptions can be deactivated automatically (also across restarts).
ALTER TABLE t_calendar ADD COLUMN ext_subscription_failing_since TIMESTAMP;

-- Rollback:
-- ALTER TABLE t_calendar DROP COLUMN IF EXISTS ext_subscription_failing_since;
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.29';
