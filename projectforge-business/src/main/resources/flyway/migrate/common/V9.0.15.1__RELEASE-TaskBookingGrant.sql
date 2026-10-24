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

-- Rollback (the grants are gone for good, the time sheets booked by them stay):
-- DROP TABLE IF EXISTS t_task_booking_grant_task;
-- DROP TABLE IF EXISTS t_task_booking_grant;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.15.1';
