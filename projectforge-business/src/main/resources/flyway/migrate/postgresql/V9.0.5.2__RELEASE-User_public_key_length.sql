-- Room for armored GPG keys with subkeys and signatures, see PFUserDO.PUBLIC_KEY_MAX_LENGTH.
ALTER TABLE T_PF_USER ALTER COLUMN gpg_public_key TYPE VARCHAR(20000);
ALTER TABLE T_PF_USER ALTER COLUMN ssh_public_key TYPE VARCHAR(20000);
