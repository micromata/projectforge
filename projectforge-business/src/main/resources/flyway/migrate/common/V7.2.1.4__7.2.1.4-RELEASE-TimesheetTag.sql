-- Add optional tag to each time sheet:
ALTER TABLE t_timesheet ADD COLUMN tag CHARACTER VARYING(1000);
