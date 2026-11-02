-- The installed apps (ProjectForge mobile client) of the users, one row per app: the hash of its token, the device
-- shown to the owner and the time of the last second factor (see AppDeviceDO).
CREATE TABLE T_PF_USER_APP_DEVICE (
                                  pk             BIGINT                      NOT NULL,
                                  user_fk        BIGINT                      NOT NULL,
                                  token_hash     CHARACTER VARYING(64)       NOT NULL,
                                  device_name    CHARACTER VARYING(100),
                                  platform       CHARACTER VARYING(20),
                                  app_version    CHARACTER VARYING(50),
                                  created        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                  last_access    TIMESTAMP WITHOUT TIME ZONE,
                                  last_access_ip CHARACTER VARYING(50),
                                  last_2fa       TIMESTAMP WITHOUT TIME ZONE
    );

ALTER TABLE T_PF_USER_APP_DEVICE
    ADD CONSTRAINT t_pf_user_app_device_pkey PRIMARY KEY (pk);

-- Globally unique: the token alone identifies the device.
ALTER TABLE T_PF_USER_APP_DEVICE
    ADD CONSTRAINT unique_t_pf_user_app_device_token UNIQUE (token_hash);

ALTER TABLE T_PF_USER_APP_DEVICE
    ADD CONSTRAINT fk_t_pf_user_app_device_user FOREIGN KEY (user_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_fk_t_pf_user_app_device_user
    ON T_PF_USER_APP_DEVICE (user_fk);

-- Rollback (the apps have to log in again afterwards, nothing else is lost):
-- DROP TABLE T_PF_USER_APP_DEVICE;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.1.1';
