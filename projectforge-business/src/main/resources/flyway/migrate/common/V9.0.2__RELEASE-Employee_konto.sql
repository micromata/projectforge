-- EmployeeDO: the (creditor) account of the employee, e.g. for travel expenses (sent to Lanes & Planes as
-- creditor_account).
ALTER TABLE t_fibu_employee ADD COLUMN konto_id BIGINT;

ALTER TABLE t_fibu_employee
    ADD CONSTRAINT fk_t_fibu_employee_konto FOREIGN KEY (konto_id) REFERENCES t_fibu_konto (pk);

CREATE INDEX idx_fk_t_fibu_employee_konto_id
    ON t_fibu_employee (konto_id);

-- Rollback:
-- DROP INDEX IF EXISTS idx_fk_t_fibu_employee_konto_id;
-- ALTER TABLE t_fibu_employee DROP CONSTRAINT IF EXISTS fk_t_fibu_employee_konto;
-- ALTER TABLE t_fibu_employee DROP COLUMN IF EXISTS konto_id;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.2';
