-- Scripts will be executable by all users, if specified.
-- As an option, scripts may also run as the given user with more access rights (in general).
ALTER TABLE T_SCRIPT ADD COLUMN executable_by_group_ids VARCHAR(10000);
ALTER TABLE T_SCRIPT ADD COLUMN executable_by_user_ids VARCHAR(10000);
ALTER TABLE T_SCRIPT ADD COLUMN execute_as_user_id INTEGER;

ALTER TABLE T_SCRIPT ADD CONSTRAINT t_script_execute_as_user_fk FOREIGN KEY (execute_as_user_id) REFERENCES t_pf_user (pk);

ALTER TABLE T_SCRIPT DROP COLUMN sudo;
