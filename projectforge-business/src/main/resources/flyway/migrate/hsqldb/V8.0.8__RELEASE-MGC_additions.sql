-- Fix columns in table employee.
ALTER TABLE t_fibu_employee ADD COLUMN new_eintritt DATE;
UPDATE t_fibu_employee SET new_eintritt = CAST(eintritt AS DATE);
ALTER TABLE t_fibu_employee DROP COLUMN eintritt;
ALTER TABLE t_fibu_employee ALTER COLUMN new_eintritt RENAME TO eintritt;

ALTER TABLE t_fibu_employee ADD COLUMN new_austritt DATE;
UPDATE t_fibu_employee SET new_austritt = CAST(austritt AS DATE);
ALTER TABLE t_fibu_employee DROP COLUMN austritt;
ALTER TABLE t_fibu_employee ALTER COLUMN new_austritt RENAME TO austritt;
