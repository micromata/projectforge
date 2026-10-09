-- Notification system: the rules (with history, edited by admins and finance) and the notifications of their runs,
-- one per rule run and recipient (also the log of who acknowledged them, when). The options of the rules and the
-- delivery cascade (in the app, then mail, later SMS) are JSON, so further options need no ALTER of these columns.
CREATE TABLE T_NOTIFICATION_RULE
(
    pk                    BIGINT                      NOT NULL,
    created               TIMESTAMP WITHOUT TIME ZONE,
    deleted               BOOLEAN                     NOT NULL,
    last_update           TIMESTAMP WITHOUT TIME ZONE,
    name                  CHARACTER VARYING(255)      NOT NULL,
    description           CHARACTER VARYING(4000),
    active                BOOLEAN                     NOT NULL,
    rule_type             CHARACTER VARYING(50)       NOT NULL,
    schedule              CHARACTER VARYING(1000),
    params                CHARACTER VARYING(4000),
    recipients            CHARACTER VARYING(10000),
    delivery              CHARACTER VARYING(4000),
    severity              CHARACTER VARYING(20)       NOT NULL,
    display               CHARACTER VARYING(20)       NOT NULL,
    manual_done           BOOLEAN                     NOT NULL,
    menu_badge            CHARACTER VARYING(100),
    subject               CHARACTER VARYING(1000),
    text                  CHARACTER VARYING(100000),
    editable_by_group_ids CHARACTER VARYING(10000),
    script_fk             BIGINT,
    last_run              TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE T_NOTIFICATION_RULE
    ADD CONSTRAINT t_notification_rule_pkey PRIMARY KEY (pk);

ALTER TABLE T_NOTIFICATION_RULE
    ADD CONSTRAINT fk_t_notification_rule_script FOREIGN KEY (script_fk) REFERENCES T_SCRIPT (pk);

CREATE TABLE T_NOTIFICATION
(
    pk                 BIGINT                      NOT NULL,
    rule_fk            BIGINT,
    recipient_fk       BIGINT                      NOT NULL,
    severity           CHARACTER VARYING(20)       NOT NULL,
    display            CHARACTER VARYING(20)       NOT NULL,
    manual_done        BOOLEAN                     NOT NULL,
    menu_badge         CHARACTER VARYING(100),
    title              CHARACTER VARYING(1000),
    body               CHARACTER VARYING(100000),
    link               CHARACTER VARYING(1000),
    period_key         CHARACTER VARYING(100),
    dedup_key          CHARACTER VARYING(255)      NOT NULL,
    status             CHARACTER VARYING(20)       NOT NULL,
    created            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    in_app_since       TIMESTAMP WITHOUT TIME ZONE,
    acknowledged_at    TIMESTAMP WITHOUT TIME ZONE,
    acknowledged_by_fk BIGINT,
    done_at            TIMESTAMP WITHOUT TIME ZONE,
    resolved_at        TIMESTAMP WITHOUT TIME ZONE,
    delivery           CHARACTER VARYING(4000),
    delivery_state     CHARACTER VARYING(10000),
    next_delivery_at   TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE T_NOTIFICATION
    ADD CONSTRAINT t_notification_pkey PRIMARY KEY (pk);

ALTER TABLE T_NOTIFICATION
    ADD CONSTRAINT unique_t_notification_dedup_key UNIQUE (dedup_key);

ALTER TABLE T_NOTIFICATION
    ADD CONSTRAINT fk_t_notification_rule FOREIGN KEY (rule_fk) REFERENCES T_NOTIFICATION_RULE (pk);

ALTER TABLE T_NOTIFICATION
    ADD CONSTRAINT fk_t_notification_recipient FOREIGN KEY (recipient_fk) REFERENCES T_PF_USER (pk);

ALTER TABLE T_NOTIFICATION
    ADD CONSTRAINT fk_t_notification_acknowledged_by FOREIGN KEY (acknowledged_by_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_t_notification_recipient_status
    ON T_NOTIFICATION (recipient_fk, status);

CREATE INDEX idx_t_notification_next_delivery
    ON T_NOTIFICATION (next_delivery_at);

CREATE INDEX idx_t_notification_rule
    ON T_NOTIFICATION (rule_fk);

-- Rollback (no other table refers to these):
-- DROP TABLE T_NOTIFICATION;
-- DROP TABLE T_NOTIFICATION_RULE;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.4';
