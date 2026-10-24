-- Ad hoc booking grants: a user may book own time sheets on some tasks for a period, without group access to them.
CREATE TABLE t_task_booking_grant
(
    pk                 BIGINT                      NOT NULL,
    created            TIMESTAMP WITHOUT TIME ZONE,
    deleted            BOOLEAN                     NOT NULL,
    last_update        TIMESTAMP WITHOUT TIME ZONE,
    user_id            BIGINT                      NOT NULL,
    valid_from         DATE                        NOT NULL,
    valid_until        DATE                        NOT NULL,
    comment            VARCHAR(4000),
    acceptance_required BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT t_task_booking_grant_pkey PRIMARY KEY (pk)
);

CREATE INDEX idx_fk_t_task_booking_grant_user_id
    ON t_task_booking_grant (user_id);

ALTER TABLE t_task_booking_grant
    ADD CONSTRAINT fk_t_task_booking_grant_user_id
        FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

-- The tasks of a grant, each with the cost units it may be booked on (comma separated ids, null for all).
CREATE TABLE t_task_booking_grant_task
(
    pk          BIGINT                      NOT NULL,
    created     TIMESTAMP WITHOUT TIME ZONE,
    deleted     BOOLEAN                     NOT NULL,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    grant_id    BIGINT                      NOT NULL,
    task_id     BIGINT                      NOT NULL,
    kost2_ids   VARCHAR(4000),
    CONSTRAINT t_task_booking_grant_task_pkey PRIMARY KEY (pk),
    CONSTRAINT uk_t_task_booking_grant_task UNIQUE (grant_id, task_id)
);

CREATE INDEX idx_fk_t_task_booking_grant_task_grant_id
    ON t_task_booking_grant_task (grant_id);

CREATE INDEX idx_fk_t_task_booking_grant_task_task_id
    ON t_task_booking_grant_task (task_id);

ALTER TABLE t_task_booking_grant_task
    ADD CONSTRAINT fk_t_task_booking_grant_task_grant_id
        FOREIGN KEY (grant_id) REFERENCES t_task_booking_grant (pk);

ALTER TABLE t_task_booking_grant_task
    ADD CONSTRAINT fk_t_task_booking_grant_task_task_id
        FOREIGN KEY (task_id) REFERENCES t_task (pk);

-- A time sheet booked by a grant only (the user has no group access to its task) refers to it. If the grant was
-- proposed by an employee without the right to grant, the time sheet is to be accepted by the project management.
ALTER TABLE t_timesheet
    ADD COLUMN booking_grant_id BIGINT;

ALTER TABLE t_timesheet
    ADD COLUMN acceptance_pending BOOLEAN DEFAULT FALSE NOT NULL;

CREATE INDEX idx_fk_t_timesheet_booking_grant_id
    ON t_timesheet (booking_grant_id);

CREATE INDEX idx_t_timesheet_acceptance_pending
    ON t_timesheet (acceptance_pending);

ALTER TABLE t_timesheet
    ADD CONSTRAINT fk_t_timesheet_booking_grant_id
        FOREIGN KEY (booking_grant_id) REFERENCES t_task_booking_grant (pk);

-- Rollback (the grants are gone for good, the time sheets booked by them stay):
-- ALTER TABLE t_timesheet DROP CONSTRAINT IF EXISTS fk_t_timesheet_booking_grant_id;
-- DROP INDEX IF EXISTS idx_fk_t_timesheet_booking_grant_id;
-- DROP INDEX IF EXISTS idx_t_timesheet_acceptance_pending;
-- ALTER TABLE t_timesheet DROP COLUMN IF EXISTS booking_grant_id;
-- ALTER TABLE t_timesheet DROP COLUMN IF EXISTS acceptance_pending;
-- DROP TABLE IF EXISTS t_task_booking_grant_task;
-- DROP TABLE IF EXISTS t_task_booking_grant;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.15.1';
