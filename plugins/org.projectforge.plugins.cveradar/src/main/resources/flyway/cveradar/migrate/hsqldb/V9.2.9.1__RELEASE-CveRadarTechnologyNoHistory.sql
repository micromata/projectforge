-- CVE-Radar: the technologies of a component (also the libraries of uploaded lists) are no longer historised; their
-- history entries go.
DELETE
FROM t_pf_history_attr
WHERE master_fk IN (SELECT pk FROM t_pf_history WHERE entity_name = 'org.projectforge.plugins.cveradar.model.CvrTechnologyDO');

DELETE
FROM t_pf_history
WHERE entity_name = 'org.projectforge.plugins.cveradar.model.CvrTechnologyDO';
