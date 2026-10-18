-- Whether an address campaign is active: the address values of an inactive campaign can't be changed any more.
-- Every existing campaign stays active.
ALTER TABLE T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL;

-- Rollback:
-- ALTER TABLE T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN DROP COLUMN active;
-- DELETE FROM t_flyway_marketing_schema_version WHERE version = '9.0.8.1';
