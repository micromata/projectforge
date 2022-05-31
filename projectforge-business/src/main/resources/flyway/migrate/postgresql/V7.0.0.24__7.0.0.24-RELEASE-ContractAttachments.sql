ALTER TABLE t_contract RENAME COLUMN attachments_size TO attachments_counter;
ALTER TABLE t_contract ADD COLUMN attachments_size BIGINT;
