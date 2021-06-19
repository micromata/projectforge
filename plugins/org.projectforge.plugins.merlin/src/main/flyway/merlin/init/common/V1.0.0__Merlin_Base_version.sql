-- This is the initial script for setting up the data base for this plugin.
-- For specific data base dialects, place sql scripts in the sub directory init/{vendor}

CREATE TABLE t_plugin_merlin_template
(
    pk                            INTEGER                NOT NULL,
    created                       TIMESTAMP WITHOUT TIME ZONE,
    deleted                       BOOLEAN                NOT NULL,
    last_update                   TIMESTAMP WITHOUT TIME ZONE,
    name                          CHARACTER VARYING(100) NOT NULL,
    admin_ids                     CHARACTER VARYING(4000),
    access_group_ids              CHARACTER VARYING(4000),
    access_user_ids               CHARACTER VARYING(4000),
    description                   CHARACTER VARYING(4000),
    fileNamePattern               CHARACTER VARYING(1000),
    strongly_restricted_filenames BOOLEAN,

    variables                     CHARACTER VARYING(100000),
    dependent_variables           CHARACTER VARYING(100000),

    attachments_names             CHARACTER VARYING(10000),
    attachments_ids               CHARACTER VARYING(10000),
    attachments_size              SMALLINT,
    attachments_last_user_action  CHARACTER VARYING(10000)
);

ALTER TABLE t_plugin_merlin_template
    ADD CONSTRAINT t_plugin_merlin_template_pkey PRIMARY KEY (pk);
