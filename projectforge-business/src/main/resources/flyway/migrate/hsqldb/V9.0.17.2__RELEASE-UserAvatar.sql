-- Avatars of the users: a square picture and a small preview of it (chat, user menu), at most one per user.
CREATE TABLE T_PF_USER_AVATAR
(
    pk            BIGINT                      NOT NULL,
    last_update   TIMESTAMP WITHOUT TIME ZONE,
    user_fk       BIGINT                      NOT NULL,
    image         BLOB,
    image_preview BLOB,
    image_type    CHARACTER VARYING(5),
    CONSTRAINT t_pf_user_avatar_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_pf_user_avatar_user UNIQUE (user_fk)
);

ALTER TABLE T_PF_USER_AVATAR
    ADD CONSTRAINT fk_t_pf_user_avatar_user FOREIGN KEY (user_fk) REFERENCES T_PF_USER (pk);
