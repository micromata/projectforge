-- Table with synchronize infos (Sipgate)

CREATE TABLE T_POLL
(
    pk          INTEGER NOT NULL,
    deleted     BOOLEAN NOT NULL,
    created     TIMESTAMP WITHOUT TIME ZONE,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    title       CHARACTER VARYING(1000) NOT NULL,
    description CHARACTER VARYING(1000),
    location    CHARACTER VARYING(1000),
    owner_pk INTEGER NOT NULL,
    deadline DATE NOT NULL,
    date DATE,
    state CHARACTER VARYING(1000) NOT NULL,
    attendeeIds VARCHAR(5000),
    groupAttendeeIds VARCHAR(5000),
    full_access_user_ids CHARACTER VARYING(255),
    full_access_group_ids CHARACTER VARYING(255),
    inputFields CHARACTER Varying(100000)
);

ALTER TABLE T_POLL
    ADD CONSTRAINT t_poll_pkey PRIMARY KEY (pk);
ALTER TABLE T_POLL
    ADD CONSTRAINT fk_t_poll_pf_user FOREIGN KEY (owner_pk) REFERENCES t_pf_user (pk);

CREATE TABLE T_POLL_RESPONSE
(
    pk          INTEGER NOT NULL,
    deleted     BOOLEAN NOT NULL,
    created     TIMESTAMP WITHOUT TIME ZONE,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    poll_pk     INTEGER NOT NULL,
    owner_pk     INTEGER NOT NULL,
    responses   CHARACTER Varying(10000)
);

ALTER TABLE T_POLL_RESPONSE
    ADD CONSTRAINT t_poll_response_pkey PRIMARY KEY (pk);
ALTER TABLE T_POLL_RESPONSE
    ADD CONSTRAINT fk_t_poll_response_pf_user FOREIGN KEY (owner_pk) REFERENCES t_pf_user (pk);
ALTER TABLE T_POLL_RESPONSE
    ADD CONSTRAINT fk_t_poll_response_poll FOREIGN KEY (poll_pk) REFERENCES t_poll (pk);