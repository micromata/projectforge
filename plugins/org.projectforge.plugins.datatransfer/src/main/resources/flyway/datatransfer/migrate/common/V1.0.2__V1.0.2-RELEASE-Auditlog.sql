-- Audit log entries for data transfer access (used for displaying and notification by mail)
CREATE TABLE t_plugin_datatransfer_audit (
                                  pk                       INTEGER              NOT NULL,
                                  timestamp                TIMESTAMP WITHOUT TIME ZONE,
                                  area_fk                  INTEGER,
                                  by_user_fk               INTEGER,
                                  by_external_user         CHARACTER VARYING(4000),
                                  filename                 CHARACTER VARYING(1000),
                                  filename_old             CHARACTER VARYING(1000),
                                  event_type               CHARACTER VARYING(20),
                                  description              CHARACTER VARYING(4000),
                                  description_old          CHARACTER VARYING(4000),
                                  notifications_sent       BOOLEAN
);

ALTER TABLE t_plugin_datatransfer_audit
    ADD CONSTRAINT t_plugin_datatransfer_audit_pk PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_datatransfer_audit_user_fk
    ON T_PF_USER (by_user_fk);

CREATE INDEX idx_t_plugin_datatransfer_audit
    ON t_plugin_datatransfer_audit (area_fk);
