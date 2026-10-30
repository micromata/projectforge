-- CVE-Radar finding workflow (PF-CVR MAT-04, MAT-05, MAT-09, MAT-13): the reason of every status (replacing the reason
-- of discarding), fixed version and date, the version at matching, and the request and approvals of a risk acceptance
-- (whether the management approves as well is fixed with the request).
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN reason CHARACTER VARYING(4000);
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN matched_version CHARACTER VARYING(100);
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN fixed_version CHARACTER VARYING(100);
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN fixed_date DATE;
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN risk_expires DATE;
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN risk_requested_by_fk BIGINT;
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN risk_approved_so_fk BIGINT;
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN risk_approved_mg_fk BIGINT;
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD COLUMN risk_needs_management BOOLEAN;

UPDATE T_PLUGIN_CVERADAR_FINDING
SET reason = discard_reason;

-- The version of the technology the existing findings were matched with (trimmed, blank as null, like the matching).
UPDATE T_PLUGIN_CVERADAR_FINDING
SET matched_version = (SELECT NULLIF(TRIM(t.technology_version), '')
                       FROM T_PLUGIN_CVERADAR_TECHNOLOGY t
                       WHERE t.pk = T_PLUGIN_CVERADAR_FINDING.technology_fk);

ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    DROP COLUMN discard_reason;

ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD CONSTRAINT fk_t_plugin_cveradar_finding_risk_requested_by FOREIGN KEY (risk_requested_by_fk) REFERENCES T_PF_USER (pk);
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD CONSTRAINT fk_t_plugin_cveradar_finding_risk_approved_so FOREIGN KEY (risk_approved_so_fk) REFERENCES T_PF_USER (pk);
ALTER TABLE T_PLUGIN_CVERADAR_FINDING
    ADD CONSTRAINT fk_t_plugin_cveradar_finding_risk_approved_mg FOREIGN KEY (risk_approved_mg_fk) REFERENCES T_PF_USER (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_finding_risk_requested_by_fk ON T_PLUGIN_CVERADAR_FINDING (risk_requested_by_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_finding_risk_approved_so_fk ON T_PLUGIN_CVERADAR_FINDING (risk_approved_so_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_finding_risk_approved_mg_fk ON T_PLUGIN_CVERADAR_FINDING (risk_approved_mg_fk);

-- Rollback:
-- ALTER TABLE T_PLUGIN_CVERADAR_FINDING ADD COLUMN discard_reason CHARACTER VARYING(4000);
-- UPDATE T_PLUGIN_CVERADAR_FINDING SET discard_reason = reason;
-- ALTER TABLE T_PLUGIN_CVERADAR_FINDING DROP COLUMN reason, DROP COLUMN matched_version, DROP COLUMN fixed_version,
--     DROP COLUMN fixed_date, DROP COLUMN risk_expires, DROP COLUMN risk_requested_by_fk,
--     DROP COLUMN risk_approved_so_fk, DROP COLUMN risk_approved_mg_fk, DROP COLUMN risk_needs_management;
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.0.5';
