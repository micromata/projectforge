-- Employee Availability System
-- Support for employee availability entries (absent, remote, partial absence, parental leave, etc.)
-- Independent from vacation days calculation

-- Main availability table
CREATE TABLE t_employee_availability
(
    pk                BIGINT       NOT NULL,
    created           TIMESTAMP    WITHOUT TIME ZONE,
    deleted           BOOLEAN      NOT NULL,
    last_update       TIMESTAMP    WITHOUT TIME ZONE,
    employee_id       INTEGER      NOT NULL,
    start_date        DATE         NOT NULL,
    end_date          DATE         NOT NULL,
    availability_type VARCHAR(50)  NOT NULL,
    percentage        INTEGER,
    replacement_id    INTEGER,
    comment           VARCHAR(4000)
);

ALTER TABLE t_employee_availability
    ADD CONSTRAINT t_employee_availability_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_availability_employee_id
    ON t_employee_availability (employee_id);

CREATE INDEX idx_fk_t_availability_replacement_id
    ON t_employee_availability (replacement_id);

ALTER TABLE t_employee_availability
    ADD CONSTRAINT t_employee_availability_employee_fk FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_employee_availability
    ADD CONSTRAINT t_employee_availability_replacement_fk FOREIGN KEY (replacement_id) REFERENCES t_fibu_employee (pk);

-- Other replacements table (many-to-many)
CREATE TABLE t_employee_availability_other_replacements
(
    availability_id INTEGER NOT NULL,
    employee_id     INTEGER NOT NULL
);

ALTER TABLE t_employee_availability_other_replacements
    ADD CONSTRAINT t_employee_availability_other_replacements_pkey PRIMARY KEY (availability_id, employee_id);

CREATE INDEX idx_fk_t_employee_availability_other_replacements_availability_id
    ON t_employee_availability_other_replacements (availability_id);

CREATE INDEX idx_fk_t_employee_availability_other_replacements_employee_id
    ON t_employee_availability_other_replacements (employee_id);

ALTER TABLE t_employee_availability_other_replacements
    ADD CONSTRAINT idx_fk_t_employee_availability_other_replacements_avail_fk FOREIGN KEY (availability_id) REFERENCES t_employee_availability (pk);

ALTER TABLE t_employee_availability_other_replacements
    ADD CONSTRAINT idx_fk_t_employee_availability_other_replacements_empl_fk FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);
