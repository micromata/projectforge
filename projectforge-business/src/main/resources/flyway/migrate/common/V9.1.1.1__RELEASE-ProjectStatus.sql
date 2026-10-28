-- The project status is reduced to acquisition, active, paused and ended (null: not set).
-- Build phase, productive and maintenance become active, on hold becomes paused, the misspelled ACQUISISTION is
-- corrected. The history keeps the former values (displayed with their former texts, see HistoryValueService).
UPDATE t_fibu_projekt
SET status = 'ACQUISITION'
WHERE status = 'ACQUISISTION';

UPDATE t_fibu_projekt
SET status = 'ACTIVE'
WHERE status IN ('BUILD', 'PRODUCTIVE', 'MAINTENANCE');

UPDATE t_fibu_projekt
SET status = 'PAUSED'
WHERE status = 'ON_HOLD';

UPDATE t_fibu_projekt
SET status = NULL
WHERE status = 'NONE';

-- Rollback (the former build phase, productive and maintenance can't be told apart, all of them become build phase):
-- UPDATE t_fibu_projekt SET status = 'ACQUISISTION' WHERE status = 'ACQUISITION';
-- UPDATE t_fibu_projekt SET status = 'BUILD' WHERE status = 'ACTIVE';
-- UPDATE t_fibu_projekt SET status = 'ON_HOLD' WHERE status = 'PAUSED';
-- DELETE FROM t_flyway_schema_version WHERE version = '9.1.1.1';
