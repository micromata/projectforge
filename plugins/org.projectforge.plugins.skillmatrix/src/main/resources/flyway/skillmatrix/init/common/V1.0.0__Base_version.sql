-- This is the initial script for setting up the data base for this plugin.
-- For specific data base dialects, place sql scripts in the sub directory init/{vendor}

CREATE TABLE t_plugin_skillmatrix_skill (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  skill       CHARACTER VARYING(1000),
  comment     CHARACTER VARYING(4000),
  tenant_id   INTEGER,
  owner_fk    INTEGER
);

ALTER TABLE t_plugin_skillmatrix_skill
  ADD CONSTRAINT t_plugin_skillmatrix_skill_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_skillmatrix_skill_owner_fk
  ON t_plugin_skillmatrix_skill (owner_fk);

CREATE INDEX idx_fk_t_plugin_skillmatrix_skill_tenant_id
  ON t_plugin_skillmatrix_skill (tenant_id);

ALTER TABLE t_plugin_skillmatrix_skill
  ADD CONSTRAINT t_plugin_skillmatrix_skill_fk_tenant FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_skillmatrix_skill
  ADD CONSTRAINT t_plugin_skillmatrix_skill_fk_owner FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);
