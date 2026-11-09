-- JSON: the preview of the first link of a chat message (title, description, thumbnail in the file store).
ALTER TABLE t_chat_message ADD COLUMN link_preview CHARACTER VARYING(4000);

-- Rollback:
-- ALTER TABLE t_chat_message DROP COLUMN IF EXISTS link_preview;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.8.1';
