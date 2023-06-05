-- Table with synchronize infos (Sipgate)

CREATE TABLE T_SIPGATE_CONTACT_SYNC
(
    sipgate_contact_id            CHARACTER VARYING(1000)      NOT NULL,
    address_id                    INTEGER                      NOT NULL,
    last_sync                     TIMESTAMP WITHOUT TIME ZONE,
    remote_status                 CHARACTER VARYING(20)        NOT NULL,
    sync_info                     CHARACTER VARYING(10000)
);

ALTER TABLE T_SIPGATE_CONTACT_SYNC
    ADD CONSTRAINT t_sipgate_contact_sync_pkey PRIMARY KEY (sipgate_contact_id);

CREATE INDEX idx_fk_t_sipgate_contact_sync_contact_id
    ON T_SIPGATE_CONTACT_SYNC (sipgate_contact_id);

CREATE INDEX idx_fk_t_sipgate_contact_sync_address_id
    ON T_SIPGATE_CONTACT_SYNC (address_id);

ALTER TABLE T_SIPGATE_CONTACT_SYNC
    ADD CONSTRAINT ifk_t_sipgate_contact_sync_address_fk FOREIGN KEY (address_id) REFERENCES t_address (pk);
