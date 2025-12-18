ProjectForge SQL-Export
=======================

Exportiert am: {{EXPORT_DATE}}
Filter: Timesheets ab 01.01.2024

Statistik:
----------
- Users: {{USER_COUNT}}
- Groups: {{GROUP_COUNT}}
- Tasks: {{TASK_COUNT}}
- Timesheets: {{TIMESHEET_COUNT}}

Inhalt:
-------
- README.txt: Diese Datei
- QUERIES.md: Beispielabfragen und SQLite-Dokumentation
- 00_schema.sql: CREATE TABLE Statements
- 01_users.sql: Benutzer (T_PF_USER)
- 02_groups.sql: Gruppen (T_GROUP)
- 03_tasks.sql: Tasks (T_TASK)
- 04_timesheets.sql: Timesheets (T_TIMESHEET)

Import-Reihenfolge (wegen Foreign Keys):
-----------------------------------------
1. 00_schema.sql (Tabellen erstellen)
2. 01_users.sql (Users müssen vor Tasks/Timesheets existieren)
3. 02_groups.sql (Groups können parallel importiert werden)
4. 03_tasks.sql (Tasks müssen vor Timesheets existieren)
5. 04_timesheets.sql (Timesheets zuletzt)

SQLite Import:
--------------
Alle Dateien nacheinander importieren:

  sqlite3 projectforge_export.db < 00_schema.sql
  sqlite3 projectforge_export.db < 01_users.sql
  sqlite3 projectforge_export.db < 02_groups.sql
  sqlite3 projectforge_export.db < 03_tasks.sql
  sqlite3 projectforge_export.db < 04_timesheets.sql

Oder alle auf einmal:

  cat *.sql | sqlite3 projectforge_export.db

Hinweise:
---------
- Die IDs (pk) werden original übernommen
- Timestamps sind im Format 'yyyy-MM-dd HH:mm:ss'
- Boolean-Werte: 0=false, 1=true
- NULL-Werte werden als NULL exportiert
- String-Werte: Einfache Anführungszeichen werden verdoppelt

Datenformat:
------------
- SQLite Version 3
- UTF-8 Encoding
- Foreign Keys sind aktiviert (PRAGMA foreign_keys = ON)

Datenanalyse:
-------------
Siehe QUERIES.md für viele Beispielabfragen mit sqlite3.

Schnellstart:
  sqlite3 projectforge_export.db
  .mode column
  .headers on

Beispielabfrage - Zeitberichte pro User:
  SELECT u.username, COUNT(t.pk) as count
  FROM T_TIMESHEET t
  JOIN T_PF_USER u ON t.user_id = u.pk
  GROUP BY u.pk;

Export als CSV:
  sqlite3 projectforge_export.db ".mode csv" "SELECT * FROM T_TIMESHEET" > timesheets.csv
