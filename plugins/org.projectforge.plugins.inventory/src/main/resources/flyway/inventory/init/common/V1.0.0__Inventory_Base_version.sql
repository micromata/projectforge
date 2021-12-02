-- This is the initial script for setting up the data base for this plugin.
-- For specific data base dialects, place sql scripts in the sub directory init/{vendor}

CREATE TABLE t_plugin_inventory_entry
(
    pk             INTEGER                NOT NULL,
    created        TIMESTAMP WITHOUT TIME ZONE,
    deleted        BOOLEAN                NOT NULL,
    last_update    TIMESTAMP WITHOUT TIME ZONE,
    item           CHARACTER VARYING(255) NOT NULL,
    owner_fk       INTEGER,
    external_owner CHARACTER VARYING(1000),
    comment        CHARACTER VARYING(4000)
);

ALTER TABLE t_plugin_inventory_entry
    ADD CONSTRAINT t_plugin_inventory_entry_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_inventory_entry_owner_fk
    ON t_plugin_inventory_entry (owner_fk);

ALTER TABLE t_plugin_inventory_entry
    ADD CONSTRAINT t_plugin_inventory_entry_fk_owner FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);
