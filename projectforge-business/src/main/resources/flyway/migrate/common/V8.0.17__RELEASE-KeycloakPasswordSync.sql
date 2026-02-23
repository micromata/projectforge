-- Add Keycloak password sync tracking column to T_PF_USER.
-- Null means the user's password has never been synced to Keycloak.
ALTER TABLE T_PF_USER ADD COLUMN last_keycloak_password_sync TIMESTAMP;

-- Add Keycloak user ID column to T_PF_USER.
-- Stores the Keycloak UUID so users can be looked up by ID instead of username.
-- Null for users that have not yet been synced with Keycloak.
ALTER TABLE T_PF_USER ADD COLUMN keycloak_id VARCHAR(100);
