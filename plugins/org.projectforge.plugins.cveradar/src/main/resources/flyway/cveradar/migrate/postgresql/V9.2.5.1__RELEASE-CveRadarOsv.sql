-- CVE-Radar (PF-CVR IMP-13, MAT-01): the advisories of OSV.dev for the libraries of the ecosystems (GHSA, PYSEC, ...;
-- raw_document: the gzipped OSV document) and their affected versions per package as an index to match against
-- (derived, replaced with every import of the advisory). An advisory becomes a vulnerability once it matches the
-- inventory (vulnerability_fk); the key of that vulnerability is its CVE id, otherwise its GHSA or OSV id.
CREATE TABLE T_PLUGIN_CVERADAR_OSV_ADVISORY
(
    pk                BIGINT                      NOT NULL,
    osv_id            CHARACTER VARYING(100)      NOT NULL,
    vulnerability_key CHARACTER VARYING(30)       NOT NULL,
    vulnerability_fk  BIGINT,
    modified          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    published         TIMESTAMP WITHOUT TIME ZONE,
    withdrawn         TIMESTAMP WITHOUT TIME ZONE,
    aliases           CHARACTER VARYING(1000),
    summary           CHARACTER VARYING(1000),
    severity          CHARACTER VARYING(20),
    cvss31_vector     CHARACTER VARYING(255),
    cvss40_vector     CHARACTER VARYING(255),
    cwes              CHARACTER VARYING(1000),
    malicious         BOOLEAN                     NOT NULL,
    raw_document      BYTEA,
    CONSTRAINT t_plugin_cveradar_osv_advisory_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_osv_advisory_osv_id UNIQUE (osv_id)
);

ALTER TABLE T_PLUGIN_CVERADAR_OSV_ADVISORY
    ADD CONSTRAINT fk_t_plugin_cveradar_osv_advisory_vulnerability FOREIGN KEY (vulnerability_fk) REFERENCES T_PLUGIN_CVERADAR_VULNERABILITY (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_osv_advisory_vulnerability_fk ON T_PLUGIN_CVERADAR_OSV_ADVISORY (vulnerability_fk);
CREATE INDEX idx_t_plugin_cveradar_osv_advisory_vulnerability_key ON T_PLUGIN_CVERADAR_OSV_ADVISORY (vulnerability_key);

CREATE TABLE T_PLUGIN_CVERADAR_PURL_MATCH
(
    pk             BIGINT                 NOT NULL,
    advisory_fk    BIGINT                 NOT NULL,
    ecosystem      CHARACTER VARYING(50)  NOT NULL,
    package_name   CHARACTER VARYING(500) NOT NULL,
    short_name     CHARACTER VARYING(255),
    introduced     CHARACTER VARYING(100),
    fixed          CHARACTER VARYING(100),
    last_affected  CHARACTER VARYING(100),
    exact_version  CHARACTER VARYING(100),
    CONSTRAINT t_plugin_cveradar_purl_match_pkey PRIMARY KEY (pk)
);

ALTER TABLE T_PLUGIN_CVERADAR_PURL_MATCH
    ADD CONSTRAINT fk_t_plugin_cveradar_purl_match_advisory FOREIGN KEY (advisory_fk) REFERENCES T_PLUGIN_CVERADAR_OSV_ADVISORY (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_purl_match_advisory_fk ON T_PLUGIN_CVERADAR_PURL_MATCH (advisory_fk);
CREATE INDEX idx_t_plugin_cveradar_purl_match_package_name ON T_PLUGIN_CVERADAR_PURL_MATCH (package_name);
CREATE INDEX idx_t_plugin_cveradar_purl_match_short_name ON T_PLUGIN_CVERADAR_PURL_MATCH (short_name);

-- Rollback:
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_PURL_MATCH;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_OSV_ADVISORY;
-- DELETE FROM T_PLUGIN_CVERADAR_IMPORT_STATE WHERE source LIKE 'OSV%';
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.5.1';
