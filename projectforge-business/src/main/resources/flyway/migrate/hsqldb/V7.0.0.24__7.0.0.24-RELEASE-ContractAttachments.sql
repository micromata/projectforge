ALTER TABLE t_contract ALTER COLUMN attachments_size RENAME TO attachments_counter;
ALTER TABLE t_contract ADD COLUMN attachments_size BIGINT;
