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
    attendeesIds VARCHAR(5000),
    groupAttendeesIds VARCHAR(5000),
    full_access_user_ids CHARACTER VARYING(255),
    full_access_group_ids CHARACTER VARYING(255),
    inputFields CHARACTER Varying(1000)
);

ALTER TABLE T_POLL
    ADD CONSTRAINT t_poll_pkey PRIMARY KEY (pk);
ALTER TABLE T_POLL
    ADD CONSTRAINT fk_t_poll_pf_user FOREIGN KEY (owner_pk) REFERENCES t_pf_user (pk);