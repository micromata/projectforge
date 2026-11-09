-- CVE-Radar: the rating of the EUVD, kept apart from the one of the NVD for the ratings of the sources in the file.
-- Filled by the EUVD import whenever it delivers an entry; here for the vulnerabilities made of an EUVD entry, whose
-- rating is the EUVD's. The others get it once the EUVD delivers their entry again.
ALTER TABLE T_PLUGIN_CVERADAR_VULNERABILITY_DETAIL
    ADD COLUMN euvd_score NUMERIC(3, 1);
ALTER TABLE T_PLUGIN_CVERADAR_VULNERABILITY_DETAIL
    ADD COLUMN euvd_vector CHARACTER VARYING(255);

UPDATE T_PLUGIN_CVERADAR_VULNERABILITY_DETAIL
SET euvd_score  = cvss_score,
    euvd_vector = COALESCE(cvss40_vector, cvss31_vector)
WHERE nvd_status = 'EUVD'
  AND nvd_last_modified IS NULL
  AND cvss_score IS NOT NULL;

-- Rollback:
-- ALTER TABLE T_PLUGIN_CVERADAR_VULNERABILITY_DETAIL DROP COLUMN euvd_score;
-- ALTER TABLE T_PLUGIN_CVERADAR_VULNERABILITY_DETAIL DROP COLUMN euvd_vector;
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.9.4';
