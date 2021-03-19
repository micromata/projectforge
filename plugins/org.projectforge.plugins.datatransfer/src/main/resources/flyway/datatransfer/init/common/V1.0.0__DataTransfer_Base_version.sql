-- This is the initial script for setting up the data base for this plugin.
-- For specific data base dialects, place sql scripts in the sub directory init/{vendor}

CREATE TABLE t_plugin_datatransfer_area
(
    pk                             INTEGER                NOT NULL,
    tenant_id                      INTEGER,
    created                        TIMESTAMP WITHOUT TIME ZONE,
    deleted                        BOOLEAN                NOT NULL,
    last_update                    TIMESTAMP WITHOUT TIME ZONE,
    area_name                      CHARACTER VARYING(100) NOT NULL,
    admin_ids                      CHARACTER VARYING(4000),
    access_group_ids               CHARACTER VARYING(4000),
    access_user_ids                CHARACTER VARYING(4000),
    description                    CHARACTER VARYING(4000),
    external_download_enabled      BOOLEAN,
    external_upload_enabled        BOOLEAN,
    external_access_token          CHARACTER VARYING(100),
    external_password              CHARACTER VARYING(100),
    expiry_days                    INTEGER,

    external_access_failed_counter INTEGER,
    external_access_logs           CHARACTER VARYING(10000),
    attachments_names              CHARACTER VARYING(10000),
    attachments_ids                CHARACTER VARYING(10000),
    attachments_size               INTEGER
);

ALTER TABLE t_plugin_datatransfer_area
    ADD CONSTRAINT t_plugin_datatransfer_area_pkey PRIMARY KEY (pk);

CREATE
INDEX idx_fk_t_plugin_datatransfer_area_tenant_id
  ON t_plugin_datatransfer_area (tenant_id);

ALTER TABLE t_plugin_datatransfer_area
    ADD CONSTRAINT t_plugin_datatransfer_fk_tenant FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);
