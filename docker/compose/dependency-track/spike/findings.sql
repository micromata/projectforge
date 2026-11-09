-- Spike PF-CVR-DT-MIG phase 0: the findings of the own matching, for the comparison with Dependency-Track.
-- PostgreSQL; run with psql -At (see ../README.md).
SELECT COALESCE(json_agg(r), '[]'::json)
FROM (SELECT f.instance_fk   AS instance_id,
             f.technology_fk AS technology_id,
             v.cve_id        AS cve_id,
             f.certainty     AS certainty,
             f.status        AS status,
             f.matched       AS matched
      FROM t_plugin_cveradar_finding f
               JOIN t_plugin_cveradar_vulnerability v ON v.pk = f.vulnerability_fk
      WHERE NOT f.deleted
      ORDER BY f.instance_fk, v.cve_id) r;
