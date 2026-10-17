-- Availability periods of users (employees and freelancers without an employee record).
CREATE TABLE t_user_availability
(
    pk                  BIGINT                      NOT NULL,
    created             TIMESTAMP WITHOUT TIME ZONE,
    deleted             BOOLEAN                     NOT NULL,
    last_update         TIMESTAMP WITHOUT TIME ZONE,
    user_id             BIGINT                      NOT NULL,
    start_date          DATE                        NOT NULL,
    end_date            DATE                        NOT NULL,
    type                VARCHAR(100),
    availability_status VARCHAR(30),
    location            VARCHAR(30),
    comment             VARCHAR(4000),
    internal_comment    VARCHAR(4000),
    CONSTRAINT t_user_availability_pkey PRIMARY KEY (pk)
);

CREATE INDEX idx_fk_t_user_availability_user_id
    ON t_user_availability (user_id);

ALTER TABLE t_user_availability
    ADD CONSTRAINT fk_t_user_availability_user_id
        FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

-- The optional substitutes of an availability entry (users, freelancers included).
CREATE TABLE t_user_availability_substitute
(
    availability_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    CONSTRAINT t_user_availability_substitute_pkey PRIMARY KEY (availability_id, user_id)
);

CREATE INDEX idx_fk_t_user_availability_substitute_availability_id
    ON t_user_availability_substitute (availability_id);

CREATE INDEX idx_fk_t_user_availability_substitute_user_id
    ON t_user_availability_substitute (user_id);

ALTER TABLE t_user_availability_substitute
    ADD CONSTRAINT fk_t_user_availability_substitute_availability_id
        FOREIGN KEY (availability_id) REFERENCES t_user_availability (pk);

ALTER TABLE t_user_availability_substitute
    ADD CONSTRAINT fk_t_user_availability_substitute_user_id
        FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

-- Rollback (the availability entries and their substitutes are gone for good):
-- DROP TABLE IF EXISTS t_user_availability_substitute;
-- DROP TABLE IF EXISTS t_user_availability;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.8.1';
