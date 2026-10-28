-- Notification rules: an own plain text with variables for the delivery by SMS (otherwise subject and link), and
-- the rendered one of each notification.
ALTER TABLE t_notification_rule ADD COLUMN sms_text VARCHAR(1000);
ALTER TABLE t_notification ADD COLUMN sms_text VARCHAR(1000);

-- Rollback:
-- ALTER TABLE t_notification DROP COLUMN IF EXISTS sms_text;
-- ALTER TABLE t_notification_rule DROP COLUMN IF EXISTS sms_text;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.15.2';
