-- CVE-Radar matching (PF-CVR MAT-01 ff.): the vulnerable CPE entries of the NVD documents as an index to match against
-- (derived, rebuilt from the documents), and the findings: vulnerability x component x instance.
CREATE TABLE T_PLUGIN_CVERADAR_CPE_MATCH
(
    pk                  BIGINT                 NOT NULL,
    vulnerability_fk    BIGINT                 NOT NULL,
    cpe_part            CHARACTER VARYING(1)   NOT NULL,
    vendor              CHARACTER VARYING(255) NOT NULL,
    product             CHARACTER VARYING(255) NOT NULL,
    cpe_version         CHARACTER VARYING(100),
    version_start_incl  CHARACTER VARYING(100),
    version_start_excl  CHARACTER VARYING(100),
    version_end_incl    CHARACTER VARYING(100),
    version_end_excl    CHARACTER VARYING(100),
    criteria            CHARACTER VARYING(500) NOT NULL,
    CONSTRAINT t_plugin_cveradar_cpe_match_pkey PRIMARY KEY (pk)
);

ALTER TABLE T_PLUGIN_CVERADAR_CPE_MATCH
    ADD CONSTRAINT fk_t_plugin_cveradar_cpe_match_vulnerability FOREIGN KEY (vulnerability_fk) REFERENCES T_PLUGIN_CVERADAR_VULNERABILITY (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_cpe_match_vulnerability_fk ON T_PLUGIN_CVERADAR_CPE_MATCH (vulnerability_fk);
CREATE INDEX idx_t_plugin_cveradar_cpe_match_product ON T_PLUGIN_CVERADAR_CPE_MATCH (product);

CREATE TABLE T_PLUGIN_CVERADAR_FINDING
(
    pk               BIGINT                      NOT NULL,
    created          TIMESTAMP WITHOUT TIME ZONE,
    deleted          BOOLEAN                     NOT NULL,
    last_update      TIMESTAMP WITHOUT TIME ZONE,
    vulnerability_fk BIGINT                      NOT NULL,
    component_fk     BIGINT                      NOT NULL,
    instance_fk      BIGINT                      NOT NULL,
    technology_fk    BIGINT,
    certainty        CHARACTER VARYING(20)       NOT NULL,
    status           CHARACTER VARYING(20)       NOT NULL,
    matched          CHARACTER VARYING(1000),
    discard_reason   CHARACTER VARYING(4000),
    CONSTRAINT t_plugin_cveradar_finding_pkey PRIMARY KEY (pk)
);

ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD CONSTRAINT fk_t_plugin_cveradar_finding_vulnerability FOREIGN KEY (vulnerability_fk) REFERENCES T_PLUGIN_CVERADAR_VULNERABILITY (pk);
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD CONSTRAINT fk_t_plugin_cveradar_finding_component FOREIGN KEY (component_fk) REFERENCES T_PLUGIN_CVERADAR_COMPONENT (pk);
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD CONSTRAINT fk_t_plugin_cveradar_finding_instance FOREIGN KEY (instance_fk) REFERENCES T_PLUGIN_CVERADAR_INSTANCE (pk);
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD CONSTRAINT fk_t_plugin_cveradar_finding_technology FOREIGN KEY (technology_fk) REFERENCES T_PLUGIN_CVERADAR_TECHNOLOGY (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_finding_vulnerability_fk ON T_PLUGIN_CVERADAR_FINDING (vulnerability_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_finding_component_fk ON T_PLUGIN_CVERADAR_FINDING (component_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_finding_instance_fk ON T_PLUGIN_CVERADAR_FINDING (instance_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_finding_technology_fk ON T_PLUGIN_CVERADAR_FINDING (technology_fk);

-- Rollback:
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_FINDING;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_CPE_MATCH;
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.0.4';
