-- Aggregated log messages (errors and classified log events) for the support error digest and the admin dashboard:
-- one row per problem (fingerprint) and its occurrences per hour. Written in batches by LogAggregationService,
-- cleaned up by its daily privacy protection job. No user names are stored, only the number of distinct users.
CREATE TABLE T_PF_LOG_GROUP (
                                pk                BIGINT                      NOT NULL,
                                fingerprint       CHARACTER VARYING(64)       NOT NULL,
                                code              CHARACTER VARYING(255)      NOT NULL,
                                category          CHARACTER VARYING(20)       NOT NULL,
                                log_level         CHARACTER VARYING(10)       NOT NULL,
                                location          CHARACTER VARYING(255),
                                exception_class   CHARACTER VARYING(255),
                                sample_message    CHARACTER VARYING(4000),
                                sample_stacktrace CHARACTER VARYING(10000),
                                sample_request    CHARACTER VARYING(1000),
                                first_seen        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                last_seen         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                total_count       BIGINT                      NOT NULL,
                                status            CHARACTER VARYING(20)       NOT NULL,
                                muted_until       TIMESTAMP WITHOUT TIME ZONE,
                                override_notify   CHARACTER VARYING(20),
                                last_notified     TIMESTAMP WITHOUT TIME ZONE,
                                reopened_at       TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE T_PF_LOG_GROUP
    ADD CONSTRAINT t_pf_log_group_pkey PRIMARY KEY (pk);

ALTER TABLE T_PF_LOG_GROUP
    ADD CONSTRAINT unique_t_pf_log_group_fingerprint UNIQUE (fingerprint);

CREATE INDEX idx_t_pf_log_group_last_seen
    ON T_PF_LOG_GROUP (last_seen);

CREATE TABLE T_PF_LOG_BUCKET (
                                 pk             BIGINT                      NOT NULL,
                                 group_fk       BIGINT                      NOT NULL,
                                 bucket_start   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                 occurrences    INTEGER                     NOT NULL,
                                 distinct_users INTEGER                     NOT NULL
);

ALTER TABLE T_PF_LOG_BUCKET
    ADD CONSTRAINT t_pf_log_bucket_pkey PRIMARY KEY (pk);

ALTER TABLE T_PF_LOG_BUCKET
    ADD CONSTRAINT unique_t_pf_log_bucket_group_start UNIQUE (group_fk, bucket_start);

ALTER TABLE T_PF_LOG_BUCKET
    ADD CONSTRAINT fk_t_pf_log_bucket_group FOREIGN KEY (group_fk) REFERENCES T_PF_LOG_GROUP (pk);

CREATE INDEX idx_t_pf_log_bucket_start
    ON T_PF_LOG_BUCKET (bucket_start);

-- Rollback (no other table refers to these, the content is only statistics):
-- DROP TABLE T_PF_LOG_BUCKET;
-- DROP TABLE T_PF_LOG_GROUP;
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.32';
