-- Webauthn tokens
CREATE TABLE T_USER_WEBAUTHN (
                                  pk                       INTEGER                  NOT NULL,
                                  created                  TIMESTAMP WITHOUT TIME ZONE,
                                  last_update              TIMESTAMP WITHOUT TIME ZONE,
                                  owner_fk                 INTEGER,
                                  credential_id            CHARACTER VARYING(4000),
                                  display_name             CHARACTER VARYING(1000),
                                  attested_credential_data CHARACTER VARYING(10000),
                                  attestation_statement    CHARACTER VARYING(10000),
                                  sign_count               INTEGER
    );

ALTER TABLE T_USER_WEBAUTHN
    ADD CONSTRAINT t_user_webauthn_pkey PRIMARY KEY (pk);

ALTER TABLE T_USER_WEBAUTHN
    ADD CONSTRAINT unique_t_user_webauthn_uid_credential UNIQUE (owner_fk, credential_id);

CREATE INDEX idx_fk_t_user_webauthn_fk
    ON T_USER_WEBAUTHN (owner_fk);

CREATE INDEX idx_t_user_webauthn_user_credential
    ON T_USER_WEBAUTHN (owner_fk, credential_id);
