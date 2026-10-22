-- Polls: a template has no deadline (it is never answered), so the column is optional. The deadline templates got as
-- a placeholder (the day they were saved) is removed.
ALTER TABLE t_poll ALTER COLUMN deadline SET NULL;
UPDATE t_poll
SET deadline = NULL
WHERE template = TRUE;
