CREATE TABLE T_PF_USER_AUTHENTICATIONS (
  pk                                     INTEGER                      NOT NULL,
  deleted                                BOOLEAN                      NOT NULL,
  created                                TIMESTAMP WITHOUT TIME ZONE,
  last_update                            TIMESTAMP WITHOUT TIME ZONE,
  tenant_id                              INTEGER,
  user_id                                INTEGER                      NOT NULL,
  calendar_export_token                  CHARACTER VARYING(1000),
  dav_token                              CHARACTER VARYING(1000),
  rest_client_token                      CHARACTER VARYING(1000),
  stay_logged_in_key                     CHARACTER VARYING(1000)
);

ALTER TABLE T_PF_USER_AUTHENTICATIONS
  ADD CONSTRAINT t_pf_user_authentications_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_pf_user_authentications_user_id
  ON t_pf_user (pk);

-- Later: ALTER TABLE t_pf_user RENAME authentication_token TO authentication_token_old
