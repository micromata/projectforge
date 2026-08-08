-- Stay-logged-in tokens: one row per device instead of one shared key per user.
-- Clean cut: the old key is dropped, so every user has to log in once again.
CREATE TABLE T_PF_USER_STAY_LOGGED_IN (
                                  pk             BIGINT                      NOT NULL,
                                  user_fk        BIGINT                      NOT NULL,
                                  token_hash     CHARACTER VARYING(64)       NOT NULL,
                                  created        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                  last_access    TIMESTAMP WITHOUT TIME ZONE,
                                  last_access_ip CHARACTER VARYING(50),
                                  user_agent     CHARACTER VARYING(255)
    );

ALTER TABLE T_PF_USER_STAY_LOGGED_IN
    ADD CONSTRAINT t_pf_user_stay_logged_in_pkey PRIMARY KEY (pk);

-- Globally unique, not unique per user: the token alone identifies the device, so nothing else in the
-- cookie has to be trusted.
ALTER TABLE T_PF_USER_STAY_LOGGED_IN
    ADD CONSTRAINT unique_t_pf_user_stay_logged_in_token UNIQUE (token_hash);

ALTER TABLE T_PF_USER_STAY_LOGGED_IN
    ADD CONSTRAINT fk_t_pf_user_stay_logged_in_user FOREIGN KEY (user_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_fk_t_pf_user_stay_logged_in_user
    ON T_PF_USER_STAY_LOGGED_IN (user_fk);

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
