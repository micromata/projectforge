-- Add Keycloak password sync tracking column to T_PF_USER.
-- Null means the user's password has never been synced to Keycloak.
ALTER TABLE T_PF_USER ADD COLUMN last_keycloak_password_sync TIMESTAMP;
