-- This is the initial script for setting up the data base for this plugin.
-- For specific data base dialects, place sql scripts in the sub directory init/{vendor}

CREATE TABLE t_plugin_inventory_item
(
    pk              INTEGER                NOT NULL,
    created         TIMESTAMP WITHOUT TIME ZONE,
    deleted         BOOLEAN                NOT NULL,
    last_update     TIMESTAMP WITHOUT TIME ZONE,
    item            CHARACTER VARYING(255) NOT NULL,
    owner_ids       CHARACTER VARYING(10000),
    external_owners CHARACTER VARYING(10000),
    comment         CHARACTER VARYING(4000)
);

ALTER TABLE t_plugin_inventory_item
    ADD CONSTRAINT t_plugin_inventory_item_pkey PRIMARY KEY (pk);
