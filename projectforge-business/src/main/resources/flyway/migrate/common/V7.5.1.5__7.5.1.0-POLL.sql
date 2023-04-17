-- Table with synchronize infos (Sipgate)
ALTER TABLE T_POLL
    ADD minimal_access_user_ids   CHARACTER VARYING(255);
ALTER TABLE T_POLL
    ADD readonly_access_group_ids CHARACTER VARYING(255);
ALTER TABLE T_POLL
    ADD readonly_access_user_ids  CHARACTER VARYING(255);