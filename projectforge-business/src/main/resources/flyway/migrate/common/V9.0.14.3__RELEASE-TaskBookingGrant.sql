-- Ad hoc booking grants: a user may book own time sheets on one task for a period, without group access to it.
CREATE TABLE t_task_booking_grant
(
    pk                    BIGINT                      NOT NULL,
    created               TIMESTAMP WITHOUT TIME ZONE,
    deleted               BOOLEAN                     NOT NULL,
    last_update           TIMESTAMP WITHOUT TIME ZONE,
    task_id               BIGINT                      NOT NULL,
    user_id               BIGINT                      NOT NULL,
    valid_from            DATE                        NOT NULL,
    valid_until           DATE                        NOT NULL,
    kost2_id              BIGINT,
    comment               VARCHAR(4000),
    granted_by_user_id    BIGINT                      NOT NULL,
    accepted_at           TIMESTAMP WITHOUT TIME ZONE,
    invitation_token_hash VARCHAR(64),
    invitation_expires_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT t_task_booking_grant_pkey PRIMARY KEY (pk),
    CONSTRAINT uk_t_task_booking_grant_invitation_token_hash UNIQUE (invitation_token_hash)
);

CREATE INDEX idx_fk_t_task_booking_grant_task_id
    ON t_task_booking_grant (task_id);

CREATE INDEX idx_fk_t_task_booking_grant_user_id
    ON t_task_booking_grant (user_id);

ALTER TABLE t_task_booking_grant
    ADD CONSTRAINT fk_t_task_booking_grant_task_id
        FOREIGN KEY (task_id) REFERENCES t_task (pk);

ALTER TABLE t_task_booking_grant
    ADD CONSTRAINT fk_t_task_booking_grant_user_id
        FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_task_booking_grant
    ADD CONSTRAINT fk_t_task_booking_grant_kost2_id
        FOREIGN KEY (kost2_id) REFERENCES t_fibu_kost2 (pk);

ALTER TABLE t_task_booking_grant
    ADD CONSTRAINT fk_t_task_booking_grant_granted_by_user_id
        FOREIGN KEY (granted_by_user_id) REFERENCES t_pf_user (pk);

-- Rollback (the grants are gone for good, the time sheets booked by them stay):
-- DROP TABLE IF EXISTS t_task_booking_grant;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.14.3';
