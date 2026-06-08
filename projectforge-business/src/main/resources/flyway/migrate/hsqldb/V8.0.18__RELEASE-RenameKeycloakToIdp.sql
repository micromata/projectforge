-- Rename Keycloak-specific columns to generic IdP columns to support multiple identity providers.
ALTER TABLE T_PF_USER ALTER COLUMN keycloak_id RENAME TO idp_external_id;
ALTER TABLE T_PF_USER ALTER COLUMN last_keycloak_password_sync RENAME TO last_idp_password_sync;
