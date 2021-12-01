-- GPG public key needed by administrators.
ALTER TABLE T_PF_USER ADD COLUMN gpg_public_key VARCHAR(4096);
