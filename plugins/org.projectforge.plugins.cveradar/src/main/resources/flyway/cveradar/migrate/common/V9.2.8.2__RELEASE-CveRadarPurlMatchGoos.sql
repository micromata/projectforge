-- CVE-Radar (PF-CVR MAT-14): the operating systems an entry of the purl index is built for (Go advisories,
-- ecosystem_specific.imports[].goos, e.g. "windows"), comma separated; null for all.
ALTER TABLE T_PLUGIN_CVERADAR_PURL_MATCH
    ADD COLUMN goos CHARACTER VARYING(255);

-- The stored Go advisories get it with their next import: they count as modified, and the import of the ecosystem
-- starts over (without a watermark it reads all of them).
UPDATE T_PLUGIN_CVERADAR_OSV_ADVISORY SET modified = TIMESTAMP '1970-01-01 00:00:00' WHERE osv_id LIKE 'GO-%';
DELETE FROM T_PLUGIN_CVERADAR_IMPORT_STATE WHERE source = 'OSV:Go';

-- Rollback:
-- ALTER TABLE T_PLUGIN_CVERADAR_PURL_MATCH DROP COLUMN goos;
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.8.2';
