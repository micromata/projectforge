-- Push token of an app (APNs device token) and the environment it belongs to (SANDBOX or PRODUCTION), see AppDeviceDO.
ALTER TABLE T_PF_USER_APP_DEVICE ADD COLUMN push_token CHARACTER VARYING(200);
ALTER TABLE T_PF_USER_APP_DEVICE ADD COLUMN push_environment CHARACTER VARYING(20);

-- Rollback (the apps register their push token again at the next start):
-- ALTER TABLE T_PF_USER_APP_DEVICE DROP COLUMN push_token;
-- ALTER TABLE T_PF_USER_APP_DEVICE DROP COLUMN push_environment;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.2.1';
