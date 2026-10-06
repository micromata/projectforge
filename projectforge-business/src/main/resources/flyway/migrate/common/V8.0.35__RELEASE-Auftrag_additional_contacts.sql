-- AuftragDO: one main contact (contact_person_fk) plus a list of additional contacts as comma separated user ids.
-- Replaces the project manager, head of business manager and sales manager of an order (see V8.0.36 for the
-- migration of their values). Their columns stay unchanged in the database.
ALTER TABLE t_fibu_auftrag ADD COLUMN additional_contact_user_ids VARCHAR(4000);

-- Rollback:
-- ALTER TABLE t_fibu_auftrag DROP COLUMN IF EXISTS additional_contact_user_ids;
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.35';
