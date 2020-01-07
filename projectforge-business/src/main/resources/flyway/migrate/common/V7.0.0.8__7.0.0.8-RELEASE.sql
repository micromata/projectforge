ALTER TABLE t_employee_vacation ADD COLUMN desciption CHARACTER VARYING(4000);
ALTER TABLE t_employee_vacation ADD COLUMN is_half_day_end BOOLEAN;

CREATE TABLE t_employee_leave_account_entry (
  pk                                     INTEGER                      NOT NULL,
  deleted                                BOOLEAN                      NOT NULL,
  created                                TIMESTAMP WITHOUT TIME ZONE,
  last_update                            TIMESTAMP WITHOUT TIME ZONE,
  tenant_id                              INTEGER,
  employee_id                            INTEGER                      NOT NULL,
  date                                   DATE                         NOT NULL,
  accounting_balance                     BOOLEAN,
  amount                                 NUMERIC(5, 2),
  description                            CHARACTER VARYING(4000)
);

ALTER TABLE t_employee_leave_account_entry
  ADD CONSTRAINT t_employee_leave_account_entry_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_leave_account_entry_employee_id
  ON t_employee_leave_account_entry (pk);

ALTER TABLE t_employee_leave_account_entry
  ADD CONSTRAINT t_employee_leave_account_entry_tenant_fk FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_employee_leave_account_entry
  ADD CONSTRAINT t_employee_leave_account_entry_employee_fk FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);
