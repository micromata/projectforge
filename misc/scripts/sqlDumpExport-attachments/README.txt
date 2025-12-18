ProjectForge SQL-Export
=======================

Exportiert am: {{EXPORT_DATE}}
Filter: Timesheets ab 01.01.2024

Statistik:
----------
Stammdaten:
- Users: {{USER_COUNT}}
- Groups: {{GROUP_COUNT}}
- Tasks: {{TASK_COUNT}}
- Timesheets: {{TIMESHEET_COUNT}}

Finanzbuchhaltung:
- Konten: {{KONTO_COUNT}}
- Kunden: {{KUNDE_COUNT}}
- Projekte: {{PROJEKT_COUNT}}
- Aufträge: {{AUFTRAG_COUNT}}
- Auftragspositionen: {{AUFTRAGSPOSITION_COUNT}}
- Zahlungspläne: {{PAYMENT_SCHEDULE_COUNT}}
- Rechnungen: {{RECHNUNG_COUNT}}
- Rechnungspositionen: {{RECHNUNGSPOSITION_COUNT}}

Kostenrechnung:
- Kost1: {{KOST1_COUNT}}
- Kost2Arten: {{KOST2ART_COUNT}}
- Kost2: {{KOST2_COUNT}}
- Buchungssätze: {{BUCHUNGSSATZ_COUNT}}

Personal & Urlaub:
- Employees: {{EMPLOYEE_COUNT}}
- Urlaubseinträge: {{VACATION_COUNT}}
- Urlaubsvertretungen: {{VACATION_REPLACEMENT_COUNT}}
- Urlaubskonto-Historie: {{LEAVE_ACCOUNT_ENTRY_COUNT}}

Inhalt:
-------
- README.txt: Diese Datei
- QUERIES.md: Beispielabfragen und SQLite-Dokumentation
- 00_schema.sql: CREATE TABLE Statements (20 Tabellen mit Foreign Keys und Indizes)
- 01_users.sql: Benutzer (T_PF_USER)
- 02_groups.sql: Gruppen (T_GROUP)
- 03_tasks.sql: Tasks (T_TASK)
- 04_timesheets.sql: Timesheets (T_TIMESHEET)
- 05_konten.sql: Konten (T_FIBU_KONTO)
- 06_kunden.sql: Kunden (T_FIBU_KUNDE)
- 07_projekte.sql: Projekte (T_FIBU_PROJEKT)
- 08_kost2arten.sql: Kostenarten (T_FIBU_KOST2ART)
- 09_kost1.sql: Kostenträger 1 (T_FIBU_KOST1)
- 10_kost2.sql: Kostenträger 2 (T_FIBU_KOST2)
- 11_employees.sql: Mitarbeiter (T_FIBU_EMPLOYEE)
- 12_auftraege.sql: Aufträge (T_FIBU_AUFTRAG)
- 13_auftragspositionen.sql: Auftragspositionen (T_FIBU_AUFTRAG_POSITION)
- 14_zahlungsplaene.sql: Zahlungspläne (T_FIBU_PAYMENT_SCHEDULE)
- 15_rechnungen.sql: Rechnungen (T_FIBU_RECHNUNG)
- 16_rechnungspositionen.sql: Rechnungspositionen (T_FIBU_RECHNUNG_POSITION)
- 17_buchungssaetze.sql: Buchungssätze (T_FIBU_BUCHUNGSSATZ)
- 18_urlaubseintraege.sql: Urlaubseinträge (T_EMPLOYEE_VACATION)
- 19_urlaubsvertretungen.sql: Urlaubsvertretungen (M:N)
- 20_urlaubskonto_historie.sql: Urlaubskonto-Historie (T_EMPLOYEE_LEAVE_ACCOUNT_ENTRY)

Import-Reihenfolge (wegen Foreign Keys):
-----------------------------------------
WICHTIG: Diese Reihenfolge muss eingehalten werden!

1. 00_schema.sql (Tabellen erstellen)
2. 01_users.sql (Users - referenced by many tables)
3. 02_groups.sql (Groups)
4. 03_tasks.sql (Tasks)
5. 05_konten.sql (Konten - referenced by Kunden, Projekte, Rechnungen)
6. 06_kunden.sql (Kunden - referenced by Projekte, Aufträge, Rechnungen)
7. 07_projekte.sql (Projekte - referenced by Aufträge, Rechnungen, Kost2)
8. 08_kost2arten.sql (Kost2Arten - referenced by Kost2)
9. 09_kost1.sql (Kost1 - referenced by Employees, Buchungssätze)
10. 10_kost2.sql (Kost2 - referenced by Buchungssätze, Timesheets)
11. 11_employees.sql (Employees - referenced by Urlaubseinträge)
12. 12_auftraege.sql (Aufträge)
13. 13_auftragspositionen.sql (Auftragspositionen)
14. 14_zahlungsplaene.sql (Zahlungspläne)
15. 15_rechnungen.sql (Rechnungen)
16. 16_rechnungspositionen.sql (Rechnungspositionen)
17. 17_buchungssaetze.sql (Buchungssätze - nach Konten/Kost1/Kost2)
18. 04_timesheets.sql (Timesheets - nach Kost2)
19. 18_urlaubseintraege.sql (Urlaubseinträge)
20. 19_urlaubsvertretungen.sql (Urlaubsvertretungen M:N)
21. 20_urlaubskonto_historie.sql (Urlaubskonto-Historie)

SQLite Import:
--------------
Alle Dateien nacheinander importieren (in numerischer Reihenfolge):

  sqlite3 projectforge_export.db < 00_schema.sql
  sqlite3 projectforge_export.db < 01_users.sql
  sqlite3 projectforge_export.db < 02_groups.sql
  sqlite3 projectforge_export.db < 03_tasks.sql
  sqlite3 projectforge_export.db < 05_konten.sql
  sqlite3 projectforge_export.db < 06_kunden.sql
  sqlite3 projectforge_export.db < 07_projekte.sql
  sqlite3 projectforge_export.db < 08_kost2arten.sql
  sqlite3 projectforge_export.db < 09_kost1.sql
  sqlite3 projectforge_export.db < 10_kost2.sql
  sqlite3 projectforge_export.db < 11_employees.sql
  sqlite3 projectforge_export.db < 12_auftraege.sql
  sqlite3 projectforge_export.db < 13_auftragspositionen.sql
  sqlite3 projectforge_export.db < 14_zahlungsplaene.sql
  sqlite3 projectforge_export.db < 15_rechnungen.sql
  sqlite3 projectforge_export.db < 16_rechnungspositionen.sql
  sqlite3 projectforge_export.db < 17_buchungssaetze.sql
  sqlite3 projectforge_export.db < 04_timesheets.sql
  sqlite3 projectforge_export.db < 18_urlaubseintraege.sql
  sqlite3 projectforge_export.db < 19_urlaubsvertretungen.sql
  sqlite3 projectforge_export.db < 20_urlaubskonto_historie.sql

Oder alle auf einmal mit numerischer Sortierung:

  for f in 00_schema.sql 01_users.sql 02_groups.sql 03_tasks.sql 05_konten.sql 06_kunden.sql 07_projekte.sql 08_kost2arten.sql 09_kost1.sql 10_kost2.sql 11_employees.sql 12_auftraege.sql 13_auftragspositionen.sql 14_zahlungsplaene.sql 15_rechnungen.sql 16_rechnungspositionen.sql 17_buchungssaetze.sql 04_timesheets.sql 18_urlaubseintraege.sql 19_urlaubsvertretungen.sql 20_urlaubskonto_historie.sql; do sqlite3 projectforge_export.db < "$f"; done

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
