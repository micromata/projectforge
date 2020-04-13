-- Favorite phone numbers are not supported anymore, so drop the columns.

ALTER TABLE T_CONTRACT ADD COLUMN attachment_names CHARACTER VARYING(10000);
ALTER TABLE T_CONTRACT ADD COLUMN attachment_ids CHARACTER VARYING(10000);
ALTER TABLE T_CONTRACT ADD COLUMN number_of_attachments SMALLINT;
