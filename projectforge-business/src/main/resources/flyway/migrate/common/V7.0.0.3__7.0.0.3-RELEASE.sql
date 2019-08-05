-- T_CALENDAR
--CREATE TABLE t_calendar_event (
--  pk                          INTEGER                NOT NULL,
--  created                     TIMESTAMP WITHOUT TIME ZONE,
--  deleted                     BOOLEAN                NOT NULL,
--  last_update                 TIMESTAMP WITHOUT TIME ZONE,
--  all_day                     BOOLEAN,
--  end_date                    TIMESTAMP WITHOUT TIME ZONE,
--  start_date                  TIMESTAMP WITHOUT TIME ZONE,
--  tenant_id                   INTEGER,
--  calendar_id                 INTEGER                NOT NULL,
--  uid                         CHARACTER VARYING(255) NOT NULL,
--  dt_stamp                    TIMESTAMP WITHOUT TIME ZONE,
--  ownership                   BOOLEAN
--);

--ALTER TABLE t_calendar_event
--  ADD CONSTRAINT t_calendar_event_pkey PRIMARY KEY (pk);

--ALTER TABLE t_calendar_event
--  ADD CONSTRAINT unique_t_calendar_event_uid_calendar_fk UNIQUE (uid, calendar_fk);

--CREATE INDEX idx_fk_t_calendar_event_calendar_fk
--  ON t_calendar_event (calendar_id);

--CREATE INDEX idx_fk_t_calendar_event_tenant_id
--  ON t_calendar_event (tenant_id);

--CREATE INDEX idx_cal_end_date
--  ON tcalendar_event (calendar_id, end_date);

--CREATE INDEX idx_cal_start_date
--  ON t_calendar_event (calendar_id, start_date);

--CREATE INDEX idx_cal_time
--  ON t_calendar_event (calendar_id, start_date, end_date);

--ALTER TABLE t_calendar_event
--  ADD CONSTRAINT t_calendar_event_tenant_fk FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

--ALTER TABLE t_calendar_event
--  ADD CONSTRAINT t_calendar_event_calendar_fk FOREIGN KEY (calendar_id) REFERENCES t_calendar (pk);
