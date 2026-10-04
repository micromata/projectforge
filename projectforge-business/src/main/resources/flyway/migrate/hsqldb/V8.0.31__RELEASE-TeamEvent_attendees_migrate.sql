-- The attendee table is unmapped since V8.0.30 (attendees as JSON on the event). HSQLDB is only used for tests
-- and holds no attendees to migrate (see the PostgreSQL variant), so its foreign keys are simply dropped.
ALTER TABLE t_plugin_calendar_event_attendee DROP CONSTRAINT fk5ls645xe5uhxcq8iqq1h8dtmn;
ALTER TABLE t_plugin_calendar_event_attendee DROP CONSTRAINT fknoqv6rc28lmv29nl9joxd8loc;
ALTER TABLE t_plugin_calendar_event_attendee DROP CONSTRAINT fkm7b18u3drw8nnyusv6o7snve1;
