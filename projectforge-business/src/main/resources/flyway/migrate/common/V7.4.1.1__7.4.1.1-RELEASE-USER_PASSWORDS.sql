-- Vacation forms of employees supports now others substitutes (replacements) in addition to the main substitue (replacement)

CREATE TABLE T_PF_USER_PASSWORD
(
    pk                                     INTEGER                      NOT NULL,
    deleted                                BOOLEAN                      NOT NULL,
    created                                TIMESTAMP WITHOUT TIME ZONE,
    last_update                            TIMESTAMP WITHOUT TIME ZONE,
    user_id                                INTEGER                      NOT NULL,
    password_hash                          CHARACTER VARYING(255),
    password_salt                          CHARACTER VARYING(40)
);

ALTER TABLE T_PF_USER_PASSWORD
    ADD CONSTRAINT t_pf_user_password_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_pf_user_password_user_id
    ON t_pf_user (pk);

ALTER TABLE T_PF_USER_PASSWORD
    ADD CONSTRAINT idx_fk_t_user_password_user_fk FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

-- Add missed foreign key constraint:
ALTER TABLE T_PF_USER_AUTHENTICATIONS
    ADD CONSTRAINT idx_fk_t_user_authentications_user_fk FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);
