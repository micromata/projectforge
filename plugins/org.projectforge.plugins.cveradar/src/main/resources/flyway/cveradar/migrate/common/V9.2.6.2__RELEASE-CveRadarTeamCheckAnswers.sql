-- CVE-Radar team checks (CLS-03) asking every member of the group (per_person, e.g. everyone checks the own client):
-- one answer per check and member. A check answered for the group keeps its answer in T_PLUGIN_CVERADAR_TEAM_CHECK.
ALTER TABLE T_PLUGIN_CVERADAR_TEAM_CHECK
    ADD COLUMN per_person BOOLEAN DEFAULT FALSE NOT NULL;

CREATE TABLE T_PLUGIN_CVERADAR_TEAM_CHECK_ANSWER
(
    pk            BIGINT                      NOT NULL,
    created       TIMESTAMP WITHOUT TIME ZONE,
    deleted       BOOLEAN                     NOT NULL,
    last_update   TIMESTAMP WITHOUT TIME ZONE,
    team_check_fk BIGINT                      NOT NULL,
    user_fk       BIGINT                      NOT NULL,
    answer        CHARACTER VARYING(20)       NOT NULL,
    comment       CHARACTER VARYING(4000),
    answer_date   DATE,
    CONSTRAINT t_plugin_cveradar_team_check_answer_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_team_check_answer UNIQUE (team_check_fk, user_fk)
);

ALTER TABLE T_PLUGIN_CVERADAR_TEAM_CHECK_ANSWER
    ADD CONSTRAINT fk_t_plugin_cveradar_team_check_answer_check FOREIGN KEY (team_check_fk) REFERENCES T_PLUGIN_CVERADAR_TEAM_CHECK (pk);

ALTER TABLE T_PLUGIN_CVERADAR_TEAM_CHECK_ANSWER
    ADD CONSTRAINT fk_t_plugin_cveradar_team_check_answer_user FOREIGN KEY (user_fk) REFERENCES T_PF_USER (pk);

CREATE INDEX idx_fk_t_plugin_cveradar_team_check_answer_user_fk
    ON T_PLUGIN_CVERADAR_TEAM_CHECK_ANSWER (user_fk);
