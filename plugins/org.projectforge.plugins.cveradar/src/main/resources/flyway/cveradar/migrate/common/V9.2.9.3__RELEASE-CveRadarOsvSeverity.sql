-- CVE-Radar: the severity of a vulnerability made of OSV advisories belongs to its CVSS score (it was the rating of
-- GitHub, e.g. CRITICAL for 6.1). The ones without score (CVSS 4 only) keep the rating of the source.
UPDATE t_plugin_cveradar_vulnerability_detail
SET severity = CASE
                   WHEN cvss31_score = 0 THEN 'NONE'
                   WHEN cvss31_score < 4 THEN 'LOW'
                   WHEN cvss31_score < 7 THEN 'MEDIUM'
                   WHEN cvss31_score < 9 THEN 'HIGH'
                   ELSE 'CRITICAL'
    END
WHERE nvd_status = 'OSV'
  AND cvss31_score IS NOT NULL;
