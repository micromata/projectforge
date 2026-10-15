-- Stay-logged-in tokens: one row per device instead of one shared key per user.
-- Clean cut: the old key is dropped, so every user has to log in once again.

-- The shared key per user is gone: it made a targeted logout impossible (one key for all devices) and
-- was stored reversibly encrypted, while the new token is only stored as a SHA-256 hash.
ALTER TABLE T_PF_USER_AUTHENTICATIONS DROP COLUMN stay_logged_in_key;
ALTER TABLE T_PF_USER_AUTHENTICATIONS DROP COLUMN stay_logged_in_key_creation_date;

-- Rollback:
-- The dropped columns can be re-created, but their content is gone for good - a rollback therefore means
-- that every user has to log in once again (just as this migration does). That is acceptable, because the
-- key was a login credential only, no user data. To go back to the previous release:
-- 1. Re-add the columns (the previous code expects them to exist, empty is fine):
-- ALTER TABLE T_PF_USER_AUTHENTICATIONS ADD COLUMN stay_logged_in_key CHARACTER VARYING(1000);
-- ALTER TABLE T_PF_USER_AUTHENTICATIONS ADD COLUMN stay_logged_in_key_creation_date TIMESTAMP WITHOUT TIME ZONE;
-- 2. Drop the new table and this migration's entry:
-- DROP TABLE T_PF_USER_STAY_LOGGED_IN;
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.21';
-- 3. Nothing else is needed: the previous code re-creates a key per user lazily on the next login
--    (UserAuthenticationsDao.ensureAuthentications), and every stay-logged-in cookie in the wild is
--    invalid anyway.
