-- Statistics of the scheduled jobs for the system dashboard (tab scheduler): one row per job and day, aggregated into
-- one row per job and month after the retention of the days (SchedulerJobStatsService, daily privacy protection job).
-- Only failed and slow runs are stored as single runs. No user names are stored.
CREATE TABLE T_PF_SCHEDULER_JOB_STATS (
                                          pk                 BIGINT                      NOT NULL,
                                          job_id             CHARACTER VARYING(100)      NOT NULL,
                                          period_type        CHARACTER VARYING(10)       NOT NULL,
                                          period_start       DATE                        NOT NULL,
                                          run_count          INTEGER                     NOT NULL,
                                          success_count      INTEGER                     NOT NULL,
                                          error_count        INTEGER                     NOT NULL,
                                          skipped_count      INTEGER                     NOT NULL,
                                          manual_count       INTEGER                     NOT NULL,
                                          slow_count         INTEGER                     NOT NULL,
                                          min_duration_ms    BIGINT,
                                          max_duration_ms    BIGINT,
                                          sum_duration_ms    BIGINT                      NOT NULL,
                                          last_run_start     TIMESTAMP WITHOUT TIME ZONE,
                                          last_status        CHARACTER VARYING(20),
                                          last_duration_ms   BIGINT,
                                          last_error_message CHARACTER VARYING(4000),
                                          last_error_time    TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE T_PF_SCHEDULER_JOB_STATS
    ADD CONSTRAINT t_pf_scheduler_job_stats_pkey PRIMARY KEY (pk);

ALTER TABLE T_PF_SCHEDULER_JOB_STATS
    ADD CONSTRAINT unique_t_pf_scheduler_job_stats_period UNIQUE (job_id, period_type, period_start);

CREATE INDEX idx_t_pf_scheduler_job_stats_period
    ON T_PF_SCHEDULER_JOB_STATS (period_type, period_start);

CREATE TABLE T_PF_SCHEDULER_JOB_RUN (
                                        pk            BIGINT                      NOT NULL,
                                        job_id        CHARACTER VARYING(100)      NOT NULL,
                                        start_time    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                        duration_ms   BIGINT                      NOT NULL,
                                        status        CHARACTER VARYING(20)       NOT NULL,
                                        slow          BOOLEAN                     NOT NULL,
                                        run_trigger   CHARACTER VARYING(10)       NOT NULL,
                                        error_message CHARACTER VARYING(4000),
                                        stack_excerpt CHARACTER VARYING(10000)
);

ALTER TABLE T_PF_SCHEDULER_JOB_RUN
    ADD CONSTRAINT t_pf_scheduler_job_run_pkey PRIMARY KEY (pk);

CREATE INDEX idx_t_pf_scheduler_job_run_job_start
    ON T_PF_SCHEDULER_JOB_RUN (job_id, start_time);

CREATE INDEX idx_t_pf_scheduler_job_run_start
    ON T_PF_SCHEDULER_JOB_RUN (start_time);

-- Rollback (no other table refers to these, the content is only statistics):
-- DROP TABLE T_PF_SCHEDULER_JOB_RUN;
-- DROP TABLE T_PF_SCHEDULER_JOB_STATS;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.4.2';
