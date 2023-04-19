-- Table with synchronize infos (Sipgate)

CREATE TABLE T_POLL
(
    pk          INTEGER NOT NULL,
    deleted     BOOLEAN NOT NULL,
    created     TIMESTAMP WITHOUT TIME ZONE,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    title       CHARACTER VARYING(1000),
    description CHARACTER VARYING(1000),
    location    CHARACTER VARYING(1000),
    owner_pk INTEGER NOT NULL,
    deadline DATE NOT NULL,
    date DATE,
    state CHARACTER VARYING(1000),
    inputFields CHARACTER Varying(100000)
/*
    canSeeResultUsers CHARACTER VARYING(1000),
    canEditPollUsers CHARACTER VARYING(1000),
    canVoteInPoll CHARACTER VARYING(1000),
*/
);

ALTER TABLE T_POLL
    ADD CONSTRAINT t_poll_pkey PRIMARY KEY (pk);
ALTER TABLE T_POLL
    ADD CONSTRAINT fk_t_poll_pf_user FOREIGN KEY (owner_pk) REFERENCES t_pf_user (pk);