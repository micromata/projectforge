-- CVE-Radar inventory (PF-CVR INV-01 ff.): areas as a tree of any depth, below them systems with instances and
-- components (the baseline of a system, or the own ones of an instance) and their technologies, which refer to the
-- technology catalogue (INV-28: ecosystems, products, libraries as a graph).
CREATE TABLE T_PLUGIN_CVERADAR_AREA
(
    pk          BIGINT                      NOT NULL,
    created     TIMESTAMP WITHOUT TIME ZONE,
    deleted     BOOLEAN                     NOT NULL,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    parent_fk   BIGINT,
    code        CHARACTER VARYING(50)       NOT NULL,
    name        CHARACTER VARYING(255)      NOT NULL,
    description CHARACTER VARYING(4000),
    group_fk    BIGINT,
    CONSTRAINT t_plugin_cveradar_area_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_area_code UNIQUE (code)
);

ALTER TABLE T_PLUGIN_CVERADAR_AREA
    ADD CONSTRAINT fk_t_plugin_cveradar_area_parent FOREIGN KEY (parent_fk) REFERENCES T_PLUGIN_CVERADAR_AREA (pk);
ALTER TABLE T_PLUGIN_CVERADAR_AREA
    ADD CONSTRAINT fk_t_plugin_cveradar_area_group FOREIGN KEY (group_fk) REFERENCES T_GROUP (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_area_parent_fk ON T_PLUGIN_CVERADAR_AREA (parent_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_area_group_fk ON T_PLUGIN_CVERADAR_AREA (group_fk);

CREATE TABLE T_PLUGIN_CVERADAR_SYSTEM
(
    pk              BIGINT                      NOT NULL,
    created         TIMESTAMP WITHOUT TIME ZONE,
    deleted         BOOLEAN                     NOT NULL,
    last_update     TIMESTAMP WITHOUT TIME ZONE,
    area_fk         BIGINT                      NOT NULL,
    code            CHARACTER VARYING(50)       NOT NULL,
    name            CHARACTER VARYING(255)      NOT NULL,
    description     CHARACTER VARYING(4000),
    type            CHARACTER VARYING(20)       NOT NULL,
    criticality     CHARACTER VARYING(20)       NOT NULL,
    confidentiality CHARACTER VARYING(30)       NOT NULL,
    group_fk        BIGINT,
    CONSTRAINT t_plugin_cveradar_system_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_system_code UNIQUE (code)
);

ALTER TABLE T_PLUGIN_CVERADAR_SYSTEM
    ADD CONSTRAINT fk_t_plugin_cveradar_system_area FOREIGN KEY (area_fk) REFERENCES T_PLUGIN_CVERADAR_AREA (pk);
ALTER TABLE T_PLUGIN_CVERADAR_SYSTEM
    ADD CONSTRAINT fk_t_plugin_cveradar_system_group FOREIGN KEY (group_fk) REFERENCES T_GROUP (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_system_area_fk ON T_PLUGIN_CVERADAR_SYSTEM (area_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_system_group_fk ON T_PLUGIN_CVERADAR_SYSTEM (group_fk);

CREATE TABLE T_PLUGIN_CVERADAR_INSTANCE
(
    pk              BIGINT                      NOT NULL,
    created         TIMESTAMP WITHOUT TIME ZONE,
    deleted         BOOLEAN                     NOT NULL,
    last_update     TIMESTAMP WITHOUT TIME ZONE,
    system_fk       BIGINT                      NOT NULL,
    code            CHARACTER VARYING(50),
    name            CHARACTER VARYING(255),
    site            CHARACTER VARYING(100),
    stage           CHARACTER VARYING(100),
    tenant          CHARACTER VARYING(255),
    exposed         BOOLEAN                     NOT NULL,
    contact_fk      BIGINT,
    confidentiality CHARACTER VARYING(30),
    criticality     CHARACTER VARYING(20),
    CONSTRAINT t_plugin_cveradar_instance_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_instance_system_code UNIQUE (system_fk, code)
);

ALTER TABLE T_PLUGIN_CVERADAR_INSTANCE
    ADD CONSTRAINT fk_t_plugin_cveradar_instance_system FOREIGN KEY (system_fk) REFERENCES T_PLUGIN_CVERADAR_SYSTEM (pk);
ALTER TABLE T_PLUGIN_CVERADAR_INSTANCE
    ADD CONSTRAINT fk_t_plugin_cveradar_instance_contact FOREIGN KEY (contact_fk) REFERENCES T_PF_USER (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_instance_system_fk ON T_PLUGIN_CVERADAR_INSTANCE (system_fk);

CREATE TABLE T_PLUGIN_CVERADAR_COMPONENT
(
    pk          BIGINT                      NOT NULL,
    created     TIMESTAMP WITHOUT TIME ZONE,
    deleted     BOOLEAN                     NOT NULL,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    system_fk   BIGINT,
    instance_fk BIGINT,
    code        CHARACTER VARYING(50)       NOT NULL,
    name        CHARACTER VARYING(255)      NOT NULL,
    CONSTRAINT t_plugin_cveradar_component_pkey PRIMARY KEY (pk)
);

ALTER TABLE T_PLUGIN_CVERADAR_COMPONENT
    ADD CONSTRAINT fk_t_plugin_cveradar_component_system FOREIGN KEY (system_fk) REFERENCES T_PLUGIN_CVERADAR_SYSTEM (pk);
ALTER TABLE T_PLUGIN_CVERADAR_COMPONENT
    ADD CONSTRAINT fk_t_plugin_cveradar_component_instance FOREIGN KEY (instance_fk) REFERENCES T_PLUGIN_CVERADAR_INSTANCE (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_component_system_fk ON T_PLUGIN_CVERADAR_COMPONENT (system_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_component_instance_fk ON T_PLUGIN_CVERADAR_COMPONENT (instance_fk);

CREATE TABLE T_PLUGIN_CVERADAR_CATALOG_NODE
(
    pk             BIGINT                      NOT NULL,
    created        TIMESTAMP WITHOUT TIME ZONE,
    deleted        BOOLEAN                     NOT NULL,
    last_update    TIMESTAMP WITHOUT TIME ZONE,
    kind           CHARACTER VARYING(20)       NOT NULL,
    name           CHARACTER VARYING(255)      NOT NULL,
    aliases        CHARACTER VARYING(4000),
    description    CHARACTER VARYING(4000),
    ecosystem_key  CHARACTER VARYING(50),
    cpe_vendor     CHARACTER VARYING(100),
    cpe_product    CHARACTER VARYING(100),
    purl_type      CHARACTER VARYING(50),
    purl_namespace CHARACTER VARYING(255),
    purl_name      CHARACTER VARYING(255),
    origin         CHARACTER VARYING(20)       NOT NULL,
    CONSTRAINT t_plugin_cveradar_catalog_node_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_catalog_node_ecosystem_key UNIQUE (ecosystem_key)
);

CREATE TABLE T_PLUGIN_CVERADAR_CATALOG_EDGE
(
    pk          BIGINT                      NOT NULL,
    created     TIMESTAMP WITHOUT TIME ZONE,
    deleted     BOOLEAN                     NOT NULL,
    last_update TIMESTAMP WITHOUT TIME ZONE,
    parent_fk   BIGINT                      NOT NULL,
    child_fk    BIGINT                      NOT NULL,
    CONSTRAINT t_plugin_cveradar_catalog_edge_pkey PRIMARY KEY (pk),
    CONSTRAINT unique_t_plugin_cveradar_catalog_edge UNIQUE (parent_fk, child_fk)
);

ALTER TABLE T_PLUGIN_CVERADAR_CATALOG_EDGE
    ADD CONSTRAINT fk_t_plugin_cveradar_catalog_edge_parent FOREIGN KEY (parent_fk) REFERENCES T_PLUGIN_CVERADAR_CATALOG_NODE (pk);
ALTER TABLE T_PLUGIN_CVERADAR_CATALOG_EDGE
    ADD CONSTRAINT fk_t_plugin_cveradar_catalog_edge_child FOREIGN KEY (child_fk) REFERENCES T_PLUGIN_CVERADAR_CATALOG_NODE (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_catalog_edge_child_fk ON T_PLUGIN_CVERADAR_CATALOG_EDGE (child_fk);

CREATE TABLE T_PLUGIN_CVERADAR_TECHNOLOGY
(
    pk           BIGINT                      NOT NULL,
    created      TIMESTAMP WITHOUT TIME ZONE,
    deleted      BOOLEAN                     NOT NULL,
    last_update  TIMESTAMP WITHOUT TIME ZONE,
    component_fk BIGINT                      NOT NULL,
    catalog_node_fk BIGINT,
    kind         CHARACTER VARYING(20)       NOT NULL,
    vendor       CHARACTER VARYING(100),
    product      CHARACTER VARYING(255)      NOT NULL,
    technology_version CHARACTER VARYING(100),
    ecosystem    CHARACTER VARYING(50),
    cpe          CHARACTER VARYING(500),
    purl         CHARACTER VARYING(500),
    origin       CHARACTER VARYING(20)       NOT NULL,
    origin_date  DATE,
    CONSTRAINT t_plugin_cveradar_technology_pkey PRIMARY KEY (pk)
);

ALTER TABLE T_PLUGIN_CVERADAR_TECHNOLOGY
    ADD CONSTRAINT fk_t_plugin_cveradar_technology_component FOREIGN KEY (component_fk) REFERENCES T_PLUGIN_CVERADAR_COMPONENT (pk);
ALTER TABLE T_PLUGIN_CVERADAR_TECHNOLOGY
    ADD CONSTRAINT fk_t_plugin_cveradar_technology_catalog_node FOREIGN KEY (catalog_node_fk) REFERENCES T_PLUGIN_CVERADAR_CATALOG_NODE (pk);
CREATE INDEX idx_fk_t_plugin_cveradar_technology_component_fk ON T_PLUGIN_CVERADAR_TECHNOLOGY (component_fk);
CREATE INDEX idx_fk_t_plugin_cveradar_technology_catalog_node_fk ON T_PLUGIN_CVERADAR_TECHNOLOGY (catalog_node_fk);

-- Rollback:
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_TECHNOLOGY;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_CATALOG_EDGE;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_CATALOG_NODE;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_COMPONENT;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_INSTANCE;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_SYSTEM;
-- DROP TABLE IF EXISTS T_PLUGIN_CVERADAR_AREA;
-- DELETE FROM t_flyway_cveradar_schema_version WHERE version = '9.2.0.3';
