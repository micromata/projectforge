-- Rollback: No rollback required: Script may run multiple times without causing harm.

-- Fix columns in table employee.
ALTER TABLE t_fibu_employee ALTER COLUMN austritt TYPE DATE USING austritt::DATE;
ALTER TABLE t_fibu_employee ALTER COLUMN eintritt TYPE DATE USING eintritt::DATE;

-- Forgotten alter statements:

ALTER TABLE t_orga_visitorbook_employee ALTER COLUMN visitorbook_id TYPE bigint;
ALTER TABLE t_orga_visitorbook_employee ALTER COLUMN employee_id TYPE bigint;

ALTER TABLE t_user_pref_entry ALTER COLUMN user_pref_fk TYPE bigint;
ALTER TABLE t_addressbook_address ALTER COLUMN address_id TYPE bigint;
ALTER TABLE t_addressbook_address ALTER COLUMN addressbook_id TYPE bigint;
ALTER TABLE t_employee_vacation_other_replacements ALTER COLUMN vacation_id TYPE bigint;
ALTER TABLE t_employee_vacation_other_replacements ALTER COLUMN employee_id TYPE bigint;
ALTER TABLE t_employee_vacation_substitution ALTER COLUMN substitution_id TYPE bigint;
ALTER TABLE t_group_user ALTER COLUMN user_id TYPE bigint;
ALTER TABLE t_group_user ALTER COLUMN group_id TYPE bigint;
ALTER TABLE t_group_task_access_entry ALTER COLUMN group_task_access_fk TYPE bigint;
ALTER TABLE t_plugin_poll ALTER COLUMN owner_fk TYPE bigint;
ALTER TABLE t_plugin_skillmatrix_entry ALTER COLUMN owner_fk TYPE bigint;

ALTER TABLE t_plugin_banking_account ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_banking_account_balance ALTER COLUMN banking_account_fk TYPE bigint;
ALTER TABLE t_plugin_banking_account_record ALTER COLUMN banking_account_fk TYPE bigint;
ALTER TABLE t_plugin_calendar_event_attendee ALTER COLUMN team_event_fk TYPE bigint;
ALTER TABLE t_plugin_calendar_event_attendee ALTER COLUMN user_id TYPE bigint;

-- SELECT
--     con.conname AS foreign_key_name,
--     nsp.nspname AS schema_name,
--     rel.relname AS table_name,
--     att2.attname AS column_name,
--     pg_catalog.format_type(att2.atttypid, att2.atttypmod) AS column_data_type,
--     ref.relname AS referenced_table,
--     att.attname AS referenced_column,
--     pg_catalog.format_type(att.atttypid, att.atttypmod) AS referenced_column_data_type
-- FROM
--     pg_constraint con
--     INNER JOIN pg_class rel
--         ON rel.oid = con.conrelid
--     INNER JOIN pg_namespace nsp
--         ON nsp.oid = rel.relnamespace
--     INNER JOIN pg_attribute att2
--         ON att2.attnum = con.conkey[1] AND att2.attrelid = con.conrelid
--     INNER JOIN pg_class ref
--         ON ref.oid = con.confrelid
--     INNER JOIN pg_attribute att
--         ON att.attnum = con.confkey[1] AND att.attrelid = con.confrelid
-- WHERE
--     con.contype = 'f' -- Nur Foreign Keys
--     AND pg_catalog.format_type(att2.atttypid, att2.atttypmod) != pg_catalog.format_type(att.atttypid, att.atttypmod)
-- ORDER BY
--     schema_name, table_name, foreign_key_name;
