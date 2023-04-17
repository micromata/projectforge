-- Table with synchronize infos (Sipgate)

ALTER TABLE T_POLL
    DROP COLUMN accessUser;

ALTER TABLE T_POLL
    ADD full_access_user_ids CHARACTER VARYING(255);
