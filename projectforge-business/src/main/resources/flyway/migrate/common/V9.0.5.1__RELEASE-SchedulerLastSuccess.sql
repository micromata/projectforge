-- The start of the last successful run of a job per period, e.g. for a job taking up where it succeeded last
-- (VacationTimesheetAutoBookingJob).
ALTER TABLE T_PF_SCHEDULER_JOB_STATS
    ADD COLUMN last_success_start TIMESTAMP WITHOUT TIME ZONE;
