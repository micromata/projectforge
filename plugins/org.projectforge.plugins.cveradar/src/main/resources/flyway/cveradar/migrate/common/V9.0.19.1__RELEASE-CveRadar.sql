-- CVE-Radar R0 (dev-docs/requirements/PF-CVR_CVE-Radar_Vulnerability_Management.md): cases (vulnerabilities and incidents)
-- with their fix criteria, team checks, the confirmations of the employees, and the devices with the findings of the
-- vulnerability scanner and the versions/checks of the client agent (filled by the syncs only).
CREATE TABLE T_PLUGIN_CVERADAR_CASE
(
    pk                       BIGINT                      NOT NULL,
    created                  TIMESTAMP WITHOUT TIME ZONE,
    deleted                  BOOLEAN                     NOT NULL,
    last_update              TIMESTAMP WITHOUT TIME ZONE,
    code                     CHARACTER VARYING(50)       NOT NULL,
    kind                     CHARACTER VARYING(20)       NOT NULL,
    title                    CHARACTER VARYING(1000)     NOT NULL,
    description              CHARACTER VARYING(20000),
    status                   CHARACTER VARYING(20)       NOT NULL,
    priority                 CHARACTER VARYING(5),
    assignee_fk              BIGINT,
    tech_measures            CHARACTER VARYING(20000),
    non_tech_measures        CHARACTER VARYING(20000),
    client_advisory          BOOLEAN                     NOT NULL,
    min_client_agent_version CHARACTER VARYING(50),
    client_agent_check_id    CHARACTER VARYING(100),
    scan_plugin_ids          CHARACTER VARYING(1000),
    cve_ids                  CHARACTER VARYING(1000),
    close_reason             CHARACTER VARYING(4000),
    CONSTRAINT t_plugin_cveradar_case_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_case_code UNIQUE (code)
);

ALTER TABLE T_PLUGIN_CVERADAR_CASE
    ADD CONSTRAINT fk_t_plugin_cveradar_case_assignee FOREIGN KEY (assignee_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_fk_t_plugin_cveradar_case_assignee_fk
    ON T_PLUGIN_CVERADAR_CASE (assignee_fk);

CREATE TABLE T_PLUGIN_CVERADAR_TEAM_CHECK
(
    pk             BIGINT                      NOT NULL,
    created        TIMESTAMP WITHOUT TIME ZONE,
    deleted        BOOLEAN                     NOT NULL,
    last_update    TIMESTAMP WITHOUT TIME ZONE,
    case_fk        BIGINT                      NOT NULL,
    group_fk       BIGINT                      NOT NULL,
    subject        CHARACTER VARYING(255)      NOT NULL,
    answer         CHARACTER VARYING(20),
    comment        CHARACTER VARYING(4000),
    answer_date    DATE,
    answered_by_fk BIGINT,
    CONSTRAINT t_plugin_cveradar_team_check_pkey PRIMARY KEY (pk)
);

ALTER TABLE T_PLUGIN_CVERADAR_TEAM_CHECK
    ADD CONSTRAINT fk_t_plugin_cveradar_team_check_case FOREIGN KEY (case_fk) REFERENCES T_PLUGIN_CVERADAR_CASE (pk);

ALTER TABLE T_PLUGIN_CVERADAR_TEAM_CHECK
    ADD CONSTRAINT fk_t_plugin_cveradar_team_check_group FOREIGN KEY (group_fk) REFERENCES T_GROUP (pk);

ALTER TABLE T_PLUGIN_CVERADAR_TEAM_CHECK
    ADD CONSTRAINT fk_t_plugin_cveradar_team_check_answered_by FOREIGN KEY (answered_by_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_fk_t_plugin_cveradar_team_check_case_fk
    ON T_PLUGIN_CVERADAR_TEAM_CHECK (case_fk);

CREATE INDEX idx_fk_t_plugin_cveradar_team_check_group_fk
    ON T_PLUGIN_CVERADAR_TEAM_CHECK (group_fk);

CREATE TABLE T_PLUGIN_CVERADAR_CONFIRMATION
(
    pk          BIGINT                      NOT NULL,
    case_fk     BIGINT                      NOT NULL,
    user_fk     BIGINT                      NOT NULL,
    answer      CHARACTER VARYING(20)       NOT NULL,
    comment     CHARACTER VARYING(4000),
    last_update TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT t_plugin_cveradar_confirmation_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_confirmation UNIQUE (case_fk, user_fk)
);

ALTER TABLE T_PLUGIN_CVERADAR_CONFIRMATION
    ADD CONSTRAINT fk_t_plugin_cveradar_confirmation_case FOREIGN KEY (case_fk) REFERENCES T_PLUGIN_CVERADAR_CASE (pk);

ALTER TABLE T_PLUGIN_CVERADAR_CONFIRMATION
    ADD CONSTRAINT fk_t_plugin_cveradar_confirmation_user FOREIGN KEY (user_fk) REFERENCES T_PF_USER (pk);

-- Devices: personal data, deleted after projectforge.plugin.cveradar.deviceRetentionDays without report.
CREATE TABLE T_PLUGIN_CVERADAR_DEVICE
(
    pk               BIGINT                      NOT NULL,
    created          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    serial_number    CHARACTER VARYING(100),
    username         CHARACTER VARYING(255),
    user_fk          BIGINT,
    hostname         CHARACTER VARYING(255),
    scan_asset_id    CHARACTER VARYING(50),
    scan_asset_name  CHARACTER VARYING(255),
    last_seen        TIMESTAMP WITHOUT TIME ZONE,
    last_scan_sync   TIMESTAMP WITHOUT TIME ZONE,
    agent_version    CHARACTER VARYING(50),
    agent_version_at TIMESTAMP WITHOUT TIME ZONE,
    agent_checks     CHARACTER VARYING(4000),
    CONSTRAINT t_plugin_cveradar_device_pkey PRIMARY KEY (pk)
);

ALTER TABLE T_PLUGIN_CVERADAR_DEVICE
    ADD CONSTRAINT fk_t_plugin_cveradar_device_user FOREIGN KEY (user_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_t_plugin_cveradar_device_serial
    ON T_PLUGIN_CVERADAR_DEVICE (serial_number);

CREATE INDEX idx_t_plugin_cveradar_device_user_fk
    ON T_PLUGIN_CVERADAR_DEVICE (user_fk);

CREATE TABLE T_PLUGIN_CVERADAR_SCAN_FINDING
(
    pk          BIGINT                      NOT NULL,
    device_fk   BIGINT                      NOT NULL,
    plugin_id   CHARACTER VARYING(50)       NOT NULL,
    plugin_name CHARACTER VARYING(1000),
    severity    INTEGER,
    cve_ids     CHARACTER VARYING(4000),
    first_seen  TIMESTAMP WITHOUT TIME ZONE,
    last_seen   TIMESTAMP WITHOUT TIME ZONE,
    status      CHARACTER VARYING(20)       NOT NULL,
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT t_plugin_cveradar_scan_finding_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_scan_finding UNIQUE (device_fk, plugin_id)
);

ALTER TABLE T_PLUGIN_CVERADAR_SCAN_FINDING
    ADD CONSTRAINT fk_t_plugin_cveradar_scan_finding_device FOREIGN KEY (device_fk) REFERENCES T_PLUGIN_CVERADAR_DEVICE (pk);
