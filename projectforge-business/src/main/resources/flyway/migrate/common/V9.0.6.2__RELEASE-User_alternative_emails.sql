-- PFUserDO: further e-mail addresses of a user (comma separated): alternative (still used) and former ones
-- (e.g. after a change of name). Used e.g. to match the users of Lanes & Planes.
ALTER TABLE t_pf_user ADD COLUMN alternative_emails VARCHAR(1000);
ALTER TABLE t_pf_user ADD COLUMN former_emails VARCHAR(1000);

-- Rollback:
-- ALTER TABLE t_pf_user DROP COLUMN IF EXISTS alternative_emails;
-- ALTER TABLE t_pf_user DROP COLUMN IF EXISTS former_emails;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.6.2';
