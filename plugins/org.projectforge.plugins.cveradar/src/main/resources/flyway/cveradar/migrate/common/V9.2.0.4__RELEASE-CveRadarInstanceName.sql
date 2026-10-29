-- CVE-Radar inventory: an instance has a name besides its code suffix.
ALTER TABLE T_PLUGIN_CVERADAR_INSTANCE ADD COLUMN name CHARACTER VARYING(255);

-- Rollback:
-- ALTER TABLE T_PLUGIN_CVERADAR_INSTANCE DROP COLUMN name;
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.0.4';
