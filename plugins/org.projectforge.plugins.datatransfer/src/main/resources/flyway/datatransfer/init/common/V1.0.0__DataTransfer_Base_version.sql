-- This is the initial script for setting up the data base for this plugin.
-- For specific data base dialects, place sql scripts in the sub directory init/{vendor}

CREATE TABLE t_plugin_datatransfer_file (
      pk                    INTEGER NOT NULL,
      tenant_id             INTEGER,
      created               TIMESTAMP WITHOUT TIME ZONE,
      deleted               BOOLEAN NOT NULL,
      last_update           TIMESTAMP WITHOUT TIME ZONE,
      owner_fk              INTEGER,
      owner_group_fk        INTEGER,
      filename              CHARACTER VARYING(100) NOT NULL,
      comment               CHARACTER VARYING(4000),
      access_token          CHARACTER VARYING(100),
      password              CHARACTER VARYING(100),
  access_failed_counter INTEGER,
  valid_until           TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE t_plugin_datatransfer_file
  ADD CONSTRAINT t_plugin_datatransfer_file_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_datatransfer_file_owner_fk
  ON t_plugin_datatransfer_file (owner_fk);

CREATE INDEX idx_fk_t_plugin_datatransfer_file_owner_group_fk
  ON t_plugin_datatransfer_file (owner_group_fk);

CREATE INDEX idx_fk_t_plugin_datatransfer_file_tenant_id
  ON t_plugin_datatransfer_file (tenant_id);

ALTER TABLE t_plugin_datatransfer_file
  ADD CONSTRAINT t_plugin_datatransfer_file_fk_tenant FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_datatransfer_file
  ADD CONSTRAINT t_plugin_datatransfer_file_fk_owner FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_datatransfer_file
    ADD CONSTRAINT t_plugin_datatransfer_file_fk_owner_group FOREIGN KEY (owner_group_fk) REFERENCES t_group (pk);
