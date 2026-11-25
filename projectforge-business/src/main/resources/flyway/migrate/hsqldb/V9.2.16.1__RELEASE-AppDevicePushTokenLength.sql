-- FCM registration tokens of Android apps are case sensitive text of up to ~200 characters, longer than an APNs token.
ALTER TABLE T_PF_USER_APP_DEVICE ALTER COLUMN push_token CHARACTER VARYING(512);

-- Rollback (tokens longer than 200 characters have to be cleared first):
-- UPDATE T_PF_USER_APP_DEVICE SET push_token = NULL WHERE LENGTH(push_token) > 200;
-- ALTER TABLE T_PF_USER_APP_DEVICE ALTER COLUMN push_token CHARACTER VARYING(200);
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.16.1';
