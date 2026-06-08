-- Rename Keycloak-specific columns to generic IdP columns to support multiple identity providers.
ALTER TABLE T_PF_USER RENAME COLUMN keycloak_id TO idp_external_id;
ALTER TABLE T_PF_USER RENAME COLUMN last_keycloak_password_sync TO last_idp_password_sync;
