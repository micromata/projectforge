-- CVE-Radar (PF-CVR INV-26): where a library read from lists was found, the uploaded file with the place inside an
-- SBOM (e.g. "image-postgres.cdx.json: /usr/local/bin/gosu"). Filled by the next upload of the component.
ALTER TABLE T_PLUGIN_CVERADAR_TECHNOLOGY
    ADD COLUMN source CHARACTER VARYING(1000);

-- Rollback:
-- ALTER TABLE T_PLUGIN_CVERADAR_TECHNOLOGY DROP COLUMN source;
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.8.3';
