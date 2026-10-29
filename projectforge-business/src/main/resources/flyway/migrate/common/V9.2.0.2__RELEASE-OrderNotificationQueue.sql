-- Order changes waiting for the daily digest mail of users who chose the daily summary of the order notifications
-- (user preference area orderNotification). One row per recipient and order, deleted after the digest is sent.
CREATE TABLE T_ORDER_NOTIFICATION_QUEUE
(
    pk           BIGINT                      NOT NULL,
    recipient_fk BIGINT                      NOT NULL,
    order_fk     BIGINT                      NOT NULL,
    order_number INTEGER,
    order_title  CHARACTER VARYING(1000),
    operation    CHARACTER VARYING(20)       NOT NULL,
    link         CHARACTER VARYING(1000),
    created      TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

ALTER TABLE T_ORDER_NOTIFICATION_QUEUE
    ADD CONSTRAINT t_order_notification_queue_pkey PRIMARY KEY (pk);

ALTER TABLE T_ORDER_NOTIFICATION_QUEUE
    ADD CONSTRAINT unique_t_order_notification_queue UNIQUE (recipient_fk, order_fk);

ALTER TABLE T_ORDER_NOTIFICATION_QUEUE
    ADD CONSTRAINT fk_t_order_notification_queue_recipient FOREIGN KEY (recipient_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_t_order_notification_queue_recipient
    ON T_ORDER_NOTIFICATION_QUEUE (recipient_fk);

-- Rollback (no other table refers to this):
-- DROP TABLE T_ORDER_NOTIFICATION_QUEUE;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.0.2';
