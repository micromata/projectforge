-- The lengths of PollDO (the columns were shorter than the entity allows).
ALTER TABLE t_poll ALTER COLUMN description VARCHAR(10000);
ALTER TABLE t_poll ALTER COLUMN full_access_user_ids VARCHAR(4000);
ALTER TABLE t_poll ALTER COLUMN full_access_group_ids VARCHAR(4000);
