-- What the daily digest of the order notifications shows besides number and title: customer and project, status, net
-- sum and the names of the changed properties (copied when the change is queued).
ALTER TABLE T_ORDER_NOTIFICATION_QUEUE
    ADD COLUMN order_customer_project CHARACTER VARYING(1000),
    ADD COLUMN order_status           CHARACTER VARYING(30),
    ADD COLUMN order_amount           NUMERIC(18, 2),
    ADD COLUMN changes                CHARACTER VARYING(2000);

-- Rollback:
-- ALTER TABLE T_ORDER_NOTIFICATION_QUEUE DROP COLUMN order_customer_project, DROP COLUMN order_status,
--     DROP COLUMN order_amount, DROP COLUMN changes;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.11.1';
