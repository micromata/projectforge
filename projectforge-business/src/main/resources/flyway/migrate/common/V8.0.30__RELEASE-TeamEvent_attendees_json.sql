-- TeamEventDO: the attendees of an event as a JSON list (name, email, status), shown read-only. Replaces the
-- unused table t_plugin_calendar_event_attendee (see V8.0.31 for the migration of its rows).
ALTER TABLE t_plugin_calendar_event ADD COLUMN attendees VARCHAR(10000);

-- Rollback:
-- ALTER TABLE t_plugin_calendar_event DROP COLUMN IF EXISTS attendees;
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.30';
