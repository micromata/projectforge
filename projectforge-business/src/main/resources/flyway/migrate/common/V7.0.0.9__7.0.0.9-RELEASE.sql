ALTER TABLE t_employee_vacation ADD COLUMN replacement_id INTEGER;

ALTER TABLE t_employee_vacation
  ADD CONSTRAINT t_employee_vacation_replacement_fk FOREIGN KEY (replacement_id) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_employee_remaining_leave ADD COLUMN comment CHARACTER VARYING(4000);
