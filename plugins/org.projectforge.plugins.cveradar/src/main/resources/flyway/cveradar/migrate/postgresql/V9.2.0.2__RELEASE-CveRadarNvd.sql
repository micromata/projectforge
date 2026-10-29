-- CVE-Radar (dev-docs/requirements/PF-CVR_CVE-Radar_Vulnerability_Management.md, PF-CVR-IMP): the vulnerabilities imported
-- from the NVD (with history; raw_document: the gzipped NVD document) and how far the import got per source.
CREATE TABLE T_PLUGIN_CVERADAR_VULNERABILITY
(
    pk                BIGINT                      NOT NULL,
    created           TIMESTAMP WITHOUT TIME ZONE,
    deleted           BOOLEAN                     NOT NULL,
    last_update       TIMESTAMP WITHOUT TIME ZONE,
    cve_id            CHARACTER VARYING(30)       NOT NULL,
    nvd_status        CHARACTER VARYING(50),
    published         TIMESTAMP WITHOUT TIME ZONE,
    nvd_last_modified TIMESTAMP WITHOUT TIME ZONE,
    description       CHARACTER VARYING(10000),
    cvss_score        NUMERIC(3, 1),
    severity          CHARACTER VARYING(20),
    cvss31_score      NUMERIC(3, 1),
    cvss31_severity   CHARACTER VARYING(20),
    cvss31_vector     CHARACTER VARYING(255),
    cvss40_score      NUMERIC(3, 1),
    cvss40_severity   CHARACTER VARYING(20),
    cvss40_vector     CHARACTER VARYING(255),
    cwes              CHARACTER VARYING(1000),
    kev_added         TIMESTAMP WITHOUT TIME ZONE,
    raw_document      BYTEA,
    CONSTRAINT t_plugin_cveradar_vulnerability_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_vulnerability_cve_id UNIQUE (cve_id)
);

CREATE INDEX idx_t_plugin_cveradar_vulnerability_nvd_modified
    ON T_PLUGIN_CVERADAR_VULNERABILITY (nvd_last_modified);

-- The case (triage, measures, discussion) of an imported CVE.
ALTER TABLE T_PLUGIN_CVERADAR_CASE
    ADD COLUMN vulnerability_fk BIGINT;

ALTER TABLE T_PLUGIN_CVERADAR_CASE
    ADD CONSTRAINT fk_t_plugin_cveradar_case_vulnerability FOREIGN KEY (vulnerability_fk) REFERENCES T_PLUGIN_CVERADAR_VULNERABILITY (pk);

ALTER TABLE T_PLUGIN_CVERADAR_CASE
    ADD CONSTRAINT unique_t_plugin_cveradar_case_vulnerability UNIQUE (vulnerability_fk);

CREATE TABLE T_PLUGIN_CVERADAR_IMPORT_STATE
(
    source             CHARACTER VARYING(20)       NOT NULL,
    last_mod_watermark TIMESTAMP WITHOUT TIME ZONE,
    last_run           TIMESTAMP WITHOUT TIME ZONE,
    last_error         CHARACTER VARYING(4000),
    CONSTRAINT t_plugin_cveradar_import_state_pkey PRIMARY KEY (source)
);

-- Rollback:
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_IMPORT_STATE;
-- ALTER TABLE T_PLUGIN_CVERADAR_CASE DROP COLUMN vulnerability_fk;
-- DROP INDEX IF EXISTS idx_t_plugin_cveradar_vulnerability_nvd_modified;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_VULNERABILITY;
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.0.2';
