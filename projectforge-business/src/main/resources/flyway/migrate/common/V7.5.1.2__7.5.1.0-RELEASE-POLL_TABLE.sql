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
);

ALTER TABLE T_POLL
    ADD CONSTRAINT t_poll_pkey PRIMARY KEY (pk);
