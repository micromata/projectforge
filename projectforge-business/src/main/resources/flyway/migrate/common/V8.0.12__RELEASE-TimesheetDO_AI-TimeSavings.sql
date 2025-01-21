-- Add parameters: time savings by usage of AI to t_timesheet:

ALTER TABLE t_timesheet ADD COLUMN time_saved_by_ai NUMERIC(10, 2);
ALTER TABLE t_timesheet ADD COLUMN time_saved_by_ai_unit CHAR(10);
ALTER TABLE t_timesheet ADD COLUMN time_saved_by_ai_description VARCHAR(1000);
