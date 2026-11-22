-- A birthday of an address may be given without the year (the year of birthday is then only a placeholder).
ALTER TABLE t_address ADD COLUMN birthday_year_unknown BOOLEAN DEFAULT false NOT NULL;

-- Rollback:
-- ALTER TABLE t_address DROP COLUMN IF EXISTS birthday_year_unknown;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.13.2';
