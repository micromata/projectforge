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

-- Rollback (the substitutes of the availability entries are gone for good):
-- DROP TABLE IF EXISTS t_user_availability_substitute;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.8.2';
