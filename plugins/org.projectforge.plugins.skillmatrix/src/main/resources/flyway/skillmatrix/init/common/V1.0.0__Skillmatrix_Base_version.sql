-- This is the initial script for setting up the data base for this plugin.
-- For specific data base dialects, place sql scripts in the sub directory init/{vendor}

CREATE TABLE t_plugin_skillmatrix_entry (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  skill       CHARACTER VARYING(255) NOT NULL,
  tenant_id   INTEGER,
  owner_fk    INTEGER,
  rating      INTEGER,
  interest    INTEGER,
  comment     CHARACTER VARYING(4000)
);

ALTER TABLE t_plugin_skillmatrix_entry
  ADD CONSTRAINT t_plugin_skillmatrix_skill_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_skillmatrix_entry_owner_fk
  ON t_plugin_skillmatrix_entry (owner_fk);

CREATE INDEX idx_fk_t_plugin_skillmatrix_skill_tenant_id
  ON t_plugin_skillmatrix_entry (tenant_id);

ALTER TABLE t_plugin_skillmatrix_entry
  ADD CONSTRAINT t_plugin_skillmatrix_entry_fk_tenant FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_skillmatrix_entry
  ADD CONSTRAINT t_plugin_skillmatrix_entry_fk_owner FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);
