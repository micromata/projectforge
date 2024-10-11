-- Migration to remove MGC.

-- Hibernates-default sequence is used for all tables.
-- Hibernate users a puffer size of 50 by default.
ALTER SEQUENCE public.hibernate_sequence INCREMENT BY 50;

-- INTEGER value is replaced by LONG.
update t_configuration set configurationtype='LONG' where configurationtype='INTEGER';

ALTER TABLE t_pf_history_attr ADD COLUMN old_value CHARACTER VARYING(50000);
ALTER TABLE t_pf_history_attr ADD COLUMN optype CHARACTER VARYING(32);

-- Replaces tables t_orga_visitorbook_timed, t_orga_visitorbook_timedattr and t_orga_visitorbook_timedattrdata.
-- t_orga_visitorbook_timedattrdata wasn't in use and empty.
CREATE TABLE t_orga_visitorbook_entry
(
    pk                        BIGINT                        NOT NULL,
    created                   TIMESTAMP WITHOUT TIME ZONE,
    deleted                   BOOLEAN                       NOT NULL,
    last_update               TIMESTAMP WITHOUT TIME ZONE,
    visitorbook_fk            BIGINT                        NOT NULL,
    date_of_visit             DATE                          NOT NULL,
    arrived                   CHARACTER VARYING(100),
    departed                  CHARACTER VARYING(100),
    comment                   CHARACTER VARYING(4000)
);

ALTER TABLE t_orga_visitorbook_entry
    ADD CONSTRAINT t_orga_visitorbook_entry_pk PRIMARY KEY (pk);

CREATE INDEX idx_t_orga_visitorbook_entry_fk_visitorbook
    ON t_orga_visitorbook_entry (visitorbook_fk);

ALTER TABLE t_orga_visitorbook_entry
    ADD CONSTRAINT t_orga_visitorbook_entry_fk_visitorbook FOREIGN KEY (visitorbook_fk) REFERENCES t_orga_visitorbook (pk);

-- Replaces tables t_fibu_employee_timed, t_fibu_employee_timedattr and t_fibu_employee_timedattrdata.
-- t_fibu_employee_timedattrdata wasn't in use and empty.
CREATE TABLE t_fibu_employee_validity_period_attr
(
    pk                        BIGINT                        NOT NULL,
    created                   TIMESTAMP WITHOUT TIME ZONE,
    deleted                   BOOLEAN                       NOT NULL,
    last_update               TIMESTAMP WITHOUT TIME ZONE,
    employee_fk               BIGINT                        NOT NULL,
    attribute                 CHARACTER VARYING(30), -- ANNUAL_LEAVE or STATUS
    valid_from                DATE                          NOT NULL,
    value                     CHARACTER VARYING(255),
    comment                   CHARACTER VARYING(4000)
);

ALTER TABLE t_fibu_employee_validity_period_attr
    ADD CONSTRAINT t_fibu_employee_validity_period_attr_pkey PRIMARY KEY (pk);

CREATE INDEX idx_t_fibu_employee_validity_period_attr_fk_employee
    ON t_fibu_employee_validity_period_attr (employee_fk);

ALTER TABLE t_fibu_employee_validity_period_attr
    ADD CONSTRAINT t_fibu_employee_validity_period_attr_fk_employee FOREIGN KEY (employee_fk) REFERENCES t_fibu_employee (pk);

