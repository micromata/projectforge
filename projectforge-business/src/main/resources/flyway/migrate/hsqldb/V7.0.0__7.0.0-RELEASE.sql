ALTER TABLE t_user_pref ADD COLUMN value_string VARCHAR(100000);
ALTER TABLE t_user_pref ADD COLUMN value_type VARCHAR(1000);
ALTER TABLE t_user_pref ALTER COLUMN area MODIFY varchar(255);

ALTER TABLE t_book DROP COLUMN task_id;
