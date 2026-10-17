-- Which absences a team calendar includes of its people (comma separated AbsenceKind names, e.g. VACATION,NOT_AVAILABLE):
-- null for all of them, as before.
ALTER TABLE T_CALENDAR ADD COLUMN include_absence_kinds CHARACTER VARYING(255);

-- Rollback (the team calendars include all kinds of absences again):
-- ALTER TABLE T_CALENDAR DROP COLUMN include_absence_kinds;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.8.2';
