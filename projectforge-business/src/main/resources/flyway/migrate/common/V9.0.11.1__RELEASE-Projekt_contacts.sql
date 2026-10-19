-- ProjektDO: one main contact (contact_person_fk) plus a list of additional contacts as comma separated user ids,
-- as orders have them since V8.0.35. Replaces the project manager, head of business manager and sales manager of a
-- project (see V9.0.11.2 for the migration of their values). Their columns stay unchanged in the database.
ALTER TABLE t_fibu_projekt ADD COLUMN contact_person_fk BIGINT;
ALTER TABLE t_fibu_projekt ADD COLUMN additional_contact_user_ids VARCHAR(4000);
ALTER TABLE t_fibu_projekt ADD CONSTRAINT t_fibu_projekt_contact_person_fk FOREIGN KEY (contact_person_fk) REFERENCES t_pf_user (pk);
CREATE INDEX idx_fk_t_fibu_projekt_contact_person_fk ON t_fibu_projekt (contact_person_fk);

-- Rollback:
-- DROP INDEX IF EXISTS idx_fk_t_fibu_projekt_contact_person_fk;
-- ALTER TABLE t_fibu_projekt DROP CONSTRAINT IF EXISTS t_fibu_projekt_contact_person_fk;
-- ALTER TABLE t_fibu_projekt DROP COLUMN IF EXISTS additional_contact_user_ids;
-- ALTER TABLE t_fibu_projekt DROP COLUMN IF EXISTS contact_person_fk;
-- DELETE FROM t_flyway_schema_version WHERE version IN ('9.0.11.1', '9.0.11.2');
