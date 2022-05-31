ALTER TABLE t_address
  ADD CONSTRAINT t_address_pkey PRIMARY KEY (pk);

ALTER TABLE t_address
  ADD CONSTRAINT unique_t_address_uid_tenant UNIQUE (uid, tenant_id);

CREATE INDEX idx_fk_t_address_tenant_id
  ON t_address (tenant_id);

CREATE INDEX idx_fk_t_address_uid_tenant_id
  ON t_address (uid, tenant_id);

ALTER TABLE t_plugin_marketing_address_campaign_value
  ADD CONSTRAINT fk2t7kbiaiidsonilvvnkbscqwo FOREIGN KEY (address_fk) REFERENCES t_address (pk);

ALTER TABLE t_plugin_calendar_event_attendee
  ADD CONSTRAINT fk5ls645xe5uhxcq8iqq1h8dtmn FOREIGN KEY (address_id) REFERENCES t_address (pk);

ALTER TABLE t_address
  ADD CONSTRAINT fk8vfxjobsyhxsvk7fd6284sy3o FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_address_attr
  ADD CONSTRAINT fkhfq0wdwdl0j26s0i207ix2teg FOREIGN KEY (parent) REFERENCES t_address (pk);

ALTER TABLE t_personal_address
  ADD CONSTRAINT fkjxldx7k4brci3utrao5lab5bb FOREIGN KEY (address_id) REFERENCES t_address (pk);

ALTER TABLE t_addressbook_address
  ADD CONSTRAINT fkrifs3n41q95o6s4ykhljb2ghq FOREIGN KEY (address_id) REFERENCES t_address (pk);

ALTER TABLE t_plugin_calendar
  ADD CONSTRAINT t_plugin_calendar_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_calendar_owner_fk
  ON t_plugin_calendar (owner_fk);

CREATE INDEX idx_fk_t_plugin_calendar_tenant_id
  ON t_plugin_calendar (tenant_id);

ALTER TABLE t_plugin_calendar
  ADD CONSTRAINT fk65ntyj5d14w3jx85bnlqy5ple FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_calendar_event
  ADD CONSTRAINT fkf1wmirbbeqct28l5s66qdgqvf FOREIGN KEY (calendar_fk) REFERENCES t_plugin_calendar (pk);

ALTER TABLE t_plugin_calendar
  ADD CONSTRAINT fknxcrji5h9y862nhk4plhosaqm FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_employee_vacation_calendar
  ADD CONSTRAINT fkquvj60f7obiyo6khuebfb3srj FOREIGN KEY (calendar_id) REFERENCES t_plugin_calendar (pk);

ALTER TABLE t_plugin_calendar_event_attachment
  ADD CONSTRAINT t_plugin_calendar_event_attachment_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_calendar_event_attachment_team_event_fk2
  ON t_plugin_calendar_event_attachment (team_event_fk2);

CREATE INDEX idx_fk_t_plugin_calendar_event_attachment_tenant_id
  ON t_plugin_calendar_event_attachment (tenant_id);

ALTER TABLE t_plugin_calendar_event_attachment
  ADD CONSTRAINT fk9jiskfrkuhy916dwkxcgfjrpw FOREIGN KEY (team_event_fk2) REFERENCES t_plugin_calendar_event (pk);

ALTER TABLE t_plugin_calendar_event_attachment
  ADD CONSTRAINT fksp4avg4xxshri06ko17sk5mn7 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_lm_license
  ADD CONSTRAINT t_plugin_lm_license_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_plugin_lm_license_tenant_id
  ON t_plugin_lm_license (tenant_id);

ALTER TABLE t_plugin_lm_license
  ADD CONSTRAINT fkeh16e02wshfwpjfmhyjmfyvjn FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_script
  ADD CONSTRAINT fkqo1egipssw1h00diuwrk2yu7s FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_script
  ADD CONSTRAINT t_script_pkey PRIMARY KEY (pk);

CREATE INDEX idx_fk_t_script_tenant_id
  ON t_script (tenant_id);