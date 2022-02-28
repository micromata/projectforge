-- Enable attachments for entity order (AuftragDO):
ALTER TABLE t_script ADD COLUMN attachments_names CHARACTER VARYING(10000);
ALTER TABLE t_script ADD COLUMN attachments_ids CHARACTER VARYING(10000);
ALTER TABLE t_script ADD COLUMN attachments_counter SMALLINT;
ALTER TABLE t_script ADD COLUMN attachments_size BIGINT;
ALTER TABLE t_script ADD COLUMN attachments_last_user_action CHARACTER VARYING(10000);
