-- Encryption at rest of the files in the file system storage (projectforge.files.fileSystemEncryptionKey). Existing
-- files aren't encrypted.
ALTER TABLE pf_files.t_attachment ADD COLUMN encryption_type CHARACTER VARYING(10) DEFAULT 'NONE' NOT NULL;
