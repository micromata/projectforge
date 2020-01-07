CREATE TABLE t_employee_vacation_remaining (
  pk                                     INTEGER                      NOT NULL,
  deleted                                BOOLEAN                      NOT NULL,
  created                                TIMESTAMP WITHOUT TIME ZONE,
  last_update                            TIMESTAMP WITHOUT TIME ZONE,
  tenant_id                              INTEGER,
  employee_id                            INTEGER                      NOT NULL,
  year                                   INTEGER                      NOT NULL,
  carry_vacation_days_from_previous_year NUMERIC(5, 2)                NOT NULL
);

ALTER TABLE t_employee_vacation_remaining
  ADD CONSTRAINT t_employee_vacation_remaining_pkey PRIMARY KEY (pk);

ALTER TABLE t_employee_vacation_remaining
  ADD CONSTRAINT unique_t_employee_vacation_remaining UNIQUE (tenant_id, employee_id, year);

CREATE INDEX idx_fk_t_vacation_remaining_employee_id
  ON t_employee_vacation_remaining (pk);

ALTER TABLE t_employee_vacation_remaining
  ADD CONSTRAINT t_employee_vacation_remaining_tenant_fk FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_employee_vacation_remaining
  ADD CONSTRAINT t_employee_vacation_remaining_employee_fk FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);
