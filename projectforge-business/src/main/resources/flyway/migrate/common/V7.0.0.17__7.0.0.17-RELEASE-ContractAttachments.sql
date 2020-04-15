-- Favorite phone numbers are not supported anymore, so drop the columns.

ALTER TABLE T_CONTRACT ADD COLUMN attachments_names CHARACTER VARYING(10000);
ALTER TABLE T_CONTRACT ADD COLUMN attachments_ids CHARACTER VARYING(10000);
ALTER TABLE T_CONTRACT ADD COLUMN attachments_size SMALLINT;
ALTER TABLE T_CONTRACT ADD COLUMN attachments_last_user_action CHARACTER VARYING(10000);
