-- Copies the attendees of t_plugin_calendar_event_attendee into the JSON column t_plugin_calendar_event.attendees
-- (V8.0.30). Name: the user's, else the address's full name, else the common name. Mail: the user's or the
-- address's, else the url without "mailto:". Deleted attendees are skipped; a list too long for the column stays
-- only in the old table.
UPDATE t_plugin_calendar_event e
SET attendees = sub.json
FROM (SELECT att.team_event_fk AS event_id,
             json_agg(json_strip_nulls(json_build_object(
                     'name', COALESCE(NULLIF(TRIM(CONCAT_WS(' ', u.firstname, u.lastname)), ''),
                                      NULLIF(TRIM(CONCAT_WS(' ', a.first_name, a.name)), ''),
                                      NULLIF(TRIM(att.common_name), '')),
                     'email', COALESCE(NULLIF(TRIM(u.email), ''),
                                       NULLIF(TRIM(a.email), ''),
                                       NULLIF(TRIM(REGEXP_REPLACE(att.url, '^mailto:', '', 'i')), '')),
                     'status', att.status))
                      ORDER BY att.number, att.pk)::text AS json
      FROM t_plugin_calendar_event_attendee att
               LEFT JOIN t_pf_user u ON u.pk = att.user_id
               LEFT JOIN t_address a ON a.pk = att.address_id
      WHERE att.deleted = FALSE
        AND att.team_event_fk IS NOT NULL
      GROUP BY att.team_event_fk) sub
WHERE e.pk = sub.event_id
  AND e.attendees IS NULL
  AND LENGTH(sub.json) <= 10000;

-- The old table is kept as a backup, but nothing maps it any more: its foreign keys would otherwise block the
-- (forced) deletion of addresses, users and team events.
ALTER TABLE t_plugin_calendar_event_attendee DROP CONSTRAINT IF EXISTS fk5ls645xe5uhxcq8iqq1h8dtmn; -- address_id
ALTER TABLE t_plugin_calendar_event_attendee DROP CONSTRAINT IF EXISTS fknoqv6rc28lmv29nl9joxd8loc; -- user_id
ALTER TABLE t_plugin_calendar_event_attendee DROP CONSTRAINT IF EXISTS fkm7b18u3drw8nnyusv6o7snve1; -- team_event_fk

-- Rollback (the copied JSON stays, see V8.0.30 for dropping the column):
-- ALTER TABLE t_plugin_calendar_event_attendee ADD CONSTRAINT fk5ls645xe5uhxcq8iqq1h8dtmn FOREIGN KEY (address_id) REFERENCES t_address (pk);
-- ALTER TABLE t_plugin_calendar_event_attendee ADD CONSTRAINT fknoqv6rc28lmv29nl9joxd8loc FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);
-- ALTER TABLE t_plugin_calendar_event_attendee ADD CONSTRAINT fkm7b18u3drw8nnyusv6o7snve1 FOREIGN KEY (team_event_fk) REFERENCES t_plugin_calendar_event (pk);
-- DELETE FROM t_flyway_schema_version WHERE version = '8.0.31';
