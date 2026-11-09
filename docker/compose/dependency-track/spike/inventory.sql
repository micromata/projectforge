-- Spike PF-CVR-DT-MIG phase 0: the effective technologies of every instance (baseline of the system plus the own
-- components of the instance) as one JSON array. PostgreSQL; run with psql -At (see ../README.md).
SELECT COALESCE(json_agg(r), '[]'::json)
FROM (SELECT i.pk                                       AS instance_id,
             s.pk                                       AS system_id,
             s.code                                     AS system_code,
             s.name                                     AS system_name,
             i.code                                     AS instance_code,
             i.name                                     AS instance_name,
             i.stage                                    AS stage,
             i.exposed                                  AS exposed,
             COALESCE(i.confidentiality, s.confidentiality) AS confidentiality,
             COALESCE(i.criticality, s.criticality)     AS criticality,
             c.code                                     AS component_code,
             t.pk                                       AS technology_id,
             t.kind                                     AS kind,
             t.vendor                                   AS vendor,
             t.product                                  AS product,
             t.technology_version                       AS version,
             t.ecosystem                                AS ecosystem,
             t.cpe                                      AS cpe,
             t.purl                                     AS purl,
             n.cpe_vendor                               AS cpe_vendor,
             n.cpe_product                              AS cpe_product,
             n.purl_type                                AS purl_type,
             n.purl_namespace                           AS purl_namespace,
             n.purl_name                                AS purl_name
      FROM t_plugin_cveradar_instance i
               JOIN t_plugin_cveradar_system s ON s.pk = i.system_fk
               JOIN t_plugin_cveradar_component c ON c.instance_fk = i.pk OR c.system_fk = s.pk
               JOIN t_plugin_cveradar_technology t ON t.component_fk = c.pk
               LEFT JOIN t_plugin_cveradar_catalog_node n ON n.pk = t.catalog_node_fk
      WHERE NOT i.deleted
        AND NOT s.deleted
        AND NOT c.deleted
        AND NOT t.deleted
      ORDER BY i.pk, t.pk) r;
