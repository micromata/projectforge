-- Kost2 reallocation: a run reallocates accounting records (and the time sheets behind the salary records) from a
-- collective cost unit to a target project. The original kost2 of a record is kept, the reallocated assignment is
-- stored as shares.
CREATE TABLE t_fibu_kost2_reallocation
(
    pk          BIGINT                      NOT NULL,
    created     TIMESTAMP WITHOUT TIME ZONE,
    deleted     BOOLEAN                     NOT NULL,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    description VARCHAR(1000)               NOT NULL,
    projekt_fk  BIGINT                      NOT NULL,
    task_fk     BIGINT,
    from_year   INTEGER                     NOT NULL,
    from_month  INTEGER                     NOT NULL,
    to_year     INTEGER                     NOT NULL,
    to_month    INTEGER                     NOT NULL,
    status      VARCHAR(20)                 NOT NULL,
    applied_at  TIMESTAMP WITHOUT TIME ZONE,
    comment     VARCHAR(4000),
    CONSTRAINT t_fibu_kost2_reallocation_pkey PRIMARY KEY (pk)
);

CREATE INDEX idx_fk_t_fibu_kost2_reallocation_projekt_fk
    ON t_fibu_kost2_reallocation (projekt_fk);

ALTER TABLE t_fibu_kost2_reallocation
    ADD CONSTRAINT fk_t_fibu_kost2_reallocation_projekt_fk
        FOREIGN KEY (projekt_fk) REFERENCES t_fibu_projekt (pk);

ALTER TABLE t_fibu_kost2_reallocation
    ADD CONSTRAINT fk_t_fibu_kost2_reallocation_task_fk
        FOREIGN KEY (task_fk) REFERENCES t_task (pk);

-- The rules A -> B of a run, optionally restricted to an account range.
CREATE TABLE t_fibu_kost2_reallocation_rule
(
    pk              BIGINT                      NOT NULL,
    created         TIMESTAMP WITHOUT TIME ZONE,
    deleted         BOOLEAN                     NOT NULL,
    last_update     TIMESTAMP WITHOUT TIME ZONE,
    run_fk          BIGINT                      NOT NULL,
    source_kost2_fk BIGINT                      NOT NULL,
    target_kost2_fk BIGINT                      NOT NULL,
    konto_from      INTEGER,
    konto_to        INTEGER,
    CONSTRAINT t_fibu_kost2_reallocation_rule_pkey PRIMARY KEY (pk)
);

CREATE INDEX idx_fk_t_fibu_kost2_reallocation_rule_run_fk
    ON t_fibu_kost2_reallocation_rule (run_fk);

ALTER TABLE t_fibu_kost2_reallocation_rule
    ADD CONSTRAINT fk_t_fibu_kost2_reallocation_rule_run_fk
        FOREIGN KEY (run_fk) REFERENCES t_fibu_kost2_reallocation (pk);

ALTER TABLE t_fibu_kost2_reallocation_rule
    ADD CONSTRAINT fk_t_fibu_kost2_reallocation_rule_source_kost2_fk
        FOREIGN KEY (source_kost2_fk) REFERENCES t_fibu_kost2 (pk);

ALTER TABLE t_fibu_kost2_reallocation_rule
    ADD CONSTRAINT fk_t_fibu_kost2_reallocation_rule_target_kost2_fk
        FOREIGN KEY (target_kost2_fk) REFERENCES t_fibu_kost2 (pk);

-- The log of the time sheets moved by a run (for the revert and the re-apply).
CREATE TABLE t_fibu_kost2_reallocation_timesheet
(
    pk           BIGINT                      NOT NULL,
    created      TIMESTAMP WITHOUT TIME ZONE,
    deleted      BOOLEAN                     NOT NULL,
    last_update  TIMESTAMP WITHOUT TIME ZONE,
    run_fk       BIGINT                      NOT NULL,
    timesheet_fk BIGINT                      NOT NULL,
    user_fk      BIGINT                      NOT NULL,
    year         INTEGER                     NOT NULL,
    month        INTEGER                     NOT NULL,
    old_task_fk  BIGINT                      NOT NULL,
    new_task_fk  BIGINT                      NOT NULL,
    old_kost2_fk BIGINT,
    new_kost2_fk BIGINT,
    millis       BIGINT                      NOT NULL,
    CONSTRAINT t_fibu_kost2_reallocation_timesheet_pkey PRIMARY KEY (pk)
);

CREATE INDEX idx_fk_t_fibu_kost2_reallocation_timesheet_run_fk
    ON t_fibu_kost2_reallocation_timesheet (run_fk);

CREATE INDEX idx_fk_t_fibu_kost2_reallocation_timesheet_timesheet_fk
    ON t_fibu_kost2_reallocation_timesheet (timesheet_fk);

ALTER TABLE t_fibu_kost2_reallocation_timesheet
    ADD CONSTRAINT fk_t_fibu_kost2_reallocation_timesheet_run_fk
        FOREIGN KEY (run_fk) REFERENCES t_fibu_kost2_reallocation (pk);

ALTER TABLE t_fibu_kost2_reallocation_timesheet
    ADD CONSTRAINT fk_t_fibu_kost2_reallocation_timesheet_timesheet_fk
        FOREIGN KEY (timesheet_fk) REFERENCES t_timesheet (pk);

ALTER TABLE t_fibu_kost2_reallocation_timesheet
    ADD CONSTRAINT fk_t_fibu_kost2_reallocation_timesheet_user_fk
        FOREIGN KEY (user_fk) REFERENCES t_pf_user (pk);

-- The kost2 shares of an accounting record. The active shares (not deleted and not superseded) sum up to the amount
-- of the record; a record without active shares counts on its own kost2.
CREATE TABLE t_fibu_buchungssatz_kost2_share
(
    pk                   BIGINT                      NOT NULL,
    created              TIMESTAMP WITHOUT TIME ZONE,
    deleted              BOOLEAN                     NOT NULL,
    last_update          TIMESTAMP WITHOUT TIME ZONE,
    buchungssatz_fk      BIGINT                      NOT NULL,
    kost2_fk             BIGINT                      NOT NULL,
    betrag               NUMERIC(18, 2)              NOT NULL,
    source               VARCHAR(20)                 NOT NULL,
    run_fk               BIGINT,
    superseded_by_run_fk BIGINT,
    CONSTRAINT t_fibu_buchungssatz_kost2_share_pkey PRIMARY KEY (pk)
);

CREATE INDEX idx_fk_t_fibu_buchungssatz_kost2_share_buchungssatz_fk
    ON t_fibu_buchungssatz_kost2_share (buchungssatz_fk);

CREATE INDEX idx_fk_t_fibu_buchungssatz_kost2_share_kost2_fk
    ON t_fibu_buchungssatz_kost2_share (kost2_fk);

CREATE INDEX idx_fk_t_fibu_buchungssatz_kost2_share_run_fk
    ON t_fibu_buchungssatz_kost2_share (run_fk);

ALTER TABLE t_fibu_buchungssatz_kost2_share
    ADD CONSTRAINT fk_t_fibu_buchungssatz_kost2_share_buchungssatz_fk
        FOREIGN KEY (buchungssatz_fk) REFERENCES t_fibu_buchungssatz (pk);

ALTER TABLE t_fibu_buchungssatz_kost2_share
    ADD CONSTRAINT fk_t_fibu_buchungssatz_kost2_share_kost2_fk
        FOREIGN KEY (kost2_fk) REFERENCES t_fibu_kost2 (pk);

ALTER TABLE t_fibu_buchungssatz_kost2_share
    ADD CONSTRAINT fk_t_fibu_buchungssatz_kost2_share_run_fk
        FOREIGN KEY (run_fk) REFERENCES t_fibu_kost2_reallocation (pk);

ALTER TABLE t_fibu_buchungssatz_kost2_share
    ADD CONSTRAINT fk_t_fibu_buchungssatz_kost2_share_superseded_by_run_fk
        FOREIGN KEY (superseded_by_run_fk) REFERENCES t_fibu_kost2_reallocation (pk);

-- Rollback (the original kost2 of the records is untouched, moved time sheets stay moved):
-- DROP TABLE IF EXISTS t_fibu_buchungssatz_kost2_share;
-- DROP TABLE IF EXISTS t_fibu_kost2_reallocation_timesheet;
-- DROP TABLE IF EXISTS t_fibu_kost2_reallocation_rule;
-- DROP TABLE IF EXISTS t_fibu_kost2_reallocation;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.0.2';
