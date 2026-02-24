-- Employee availability table for tracking availability periods.
CREATE TABLE t_employee_availability
(
    pk                  BIGINT                      NOT NULL,
    created             TIMESTAMP WITHOUT TIME ZONE,
    deleted             BOOLEAN                     NOT NULL DEFAULT FALSE,
    last_update         TIMESTAMP WITHOUT TIME ZONE,
    employee_id         BIGINT                      NOT NULL,
    start_date          DATE                        NOT NULL,
    end_date            DATE                        NOT NULL,
    type                VARCHAR(100),
    availability_status VARCHAR(30),
    location            VARCHAR(30),
    description         VARCHAR(4000),
    CONSTRAINT t_employee_availability_pkey PRIMARY KEY (pk)
);

CREATE INDEX idx_fk_t_employee_availability_employee_id
    ON t_employee_availability (employee_id);

ALTER TABLE t_employee_availability
    ADD CONSTRAINT fk_t_employee_availability_employee_id
        FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);
