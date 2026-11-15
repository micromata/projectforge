-- The availability types are codes now, shown in the user's language (availability.types.<code>): the texts of the
-- former default list and of its English example in application.properties become their codes. Other values (an
-- installation's own list) stay as they are and are shown as given.
UPDATE t_user_availability SET type = 'parentalLeave' WHERE type IN ('Elternzeit', 'parental leave');
UPDATE t_user_availability SET type = 'sickness' WHERE type IN ('Krankheit', 'illness');
UPDATE t_user_availability SET type = 'absence' WHERE type IN ('Abwesenheit', 'absence');
UPDATE t_user_availability SET type = 'compensatoryTimeOff' WHERE type IN ('Zeitausgleich', 'compensatory time-off');
UPDATE t_user_availability SET type = 'mobileWork' WHERE type IN ('Mobile Tätigkeit', 'mobile work');
UPDATE t_user_availability SET type = 'workation' WHERE type IN ('Workation');

-- Rollback:
-- UPDATE t_user_availability SET type = 'Elternzeit' WHERE type = 'parentalLeave';
-- UPDATE t_user_availability SET type = 'Krankheit' WHERE type = 'sickness';
-- UPDATE t_user_availability SET type = 'Abwesenheit' WHERE type = 'absence';
-- UPDATE t_user_availability SET type = 'Zeitausgleich' WHERE type = 'compensatoryTimeOff';
-- UPDATE t_user_availability SET type = 'Mobile Tätigkeit' WHERE type = 'mobileWork';
-- UPDATE t_user_availability SET type = 'Workation' WHERE type = 'workation';
-- DELETE FROM t_flyway_schema_version WHERE version = '9.2.12.1';
