-- Random token of the public url of the contact image of an address (photo of a digital business card).
ALTER TABLE t_address ADD COLUMN image_public_token VARCHAR(43);
CREATE UNIQUE INDEX idx_fk_t_address_image_public_token ON t_address (image_public_token);

-- Rollback:
-- DROP INDEX IF EXISTS idx_fk_t_address_image_public_token;
-- ALTER TABLE t_address DROP COLUMN IF EXISTS image_public_token;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.6.1';
