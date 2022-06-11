-- Vacation forms of employees supports now others substitutes (replacements) in addition to the main substitue (replacement)

CREATE TABLE t_employee_vacation_other_replacements
(
    vacation_id INTEGER NOT NULL,
    user_id     INTEGER NOT NULL
);

ALTER TABLE t_employee_vacation_other_replacements
    ADD CONSTRAINT t_employee_vacation_other_replacements_pkey PRIMARY KEY (vacation_id, user_id);

CREATE INDEX idx_fk_t_employee_vacation_other_replacements_vacation_id
    ON t_employee_vacation_other_replacements (vacation_id);

CREATE INDEX idx_fk_t_employee_vacation_other_replacements_user_id
    ON t_employee_vacation_other_replacements (user_id);

ALTER TABLE t_employee_vacation_other_replacements
    ADD CONSTRAINT idx_fk_t_employee_vacation_other_replacements_vacation_fk FOREIGN KEY (vacation_id) REFERENCES t_employee_vacation (pk);

ALTER TABLE t_employee_vacation_other_replacements
    ADD CONSTRAINT idx_fk_t_employee_vacation_other_replacements_user_fk FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);
