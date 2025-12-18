# SQLite Abfragen - ProjectForge Export

Dieses Dokument enthält hilfreiche SQL-Abfragen zur Analyse der exportierten ProjectForge-Daten.

## Datenbank öffnen

```bash
sqlite3 projectforge_export.db
```

## SQLite Einstellungen für bessere Ausgabe

```sql
.mode column
.headers on
.width auto
```

## Schema anzeigen

```sql
.schema T_TIMESHEET
.schema T_TASK
.schema T_PF_USER
.schema T_GROUP
```

## Beispielabfragen

### 1. Zeitberichte pro User

```sql
SELECT
    u.username,
    u.firstname || ' ' || u.lastname as fullname,
    COUNT(t.pk) as timesheet_count,
    ROUND(SUM((julianday(t.stop_time) - julianday(t.start_time)) * 24), 2) as total_hours
FROM T_TIMESHEET t
JOIN T_PF_USER u ON t.user_id = u.pk
WHERE t.deleted = 0
GROUP BY u.pk
ORDER BY total_hours DESC;
```

### 2. Zeitberichte pro Task

```sql
SELECT
    task.title,
    COUNT(ts.pk) as count,
    ROUND(SUM((julianday(ts.stop_time) - julianday(ts.start_time)) * 24), 2) as hours
FROM T_TIMESHEET ts
JOIN T_TASK task ON ts.task_id = task.pk
WHERE ts.deleted = 0
GROUP BY task.pk
ORDER BY hours DESC;
```

### 3. Monatliche Statistiken

```sql
SELECT
    strftime('%Y-%m', start_time) as month,
    COUNT(*) as timesheet_count,
    COUNT(DISTINCT user_id) as active_users,
    ROUND(SUM((julianday(stop_time) - julianday(start_time)) * 24), 2) as total_hours
FROM T_TIMESHEET
WHERE deleted = 0
GROUP BY month
ORDER BY month;
```

### 4. Tägliche Arbeitszeit pro User

```sql
SELECT
    u.username,
    DATE(ts.start_time) as date,
    ROUND(SUM((julianday(ts.stop_time) - julianday(ts.start_time)) * 24), 2) as hours
FROM T_TIMESHEET ts
JOIN T_PF_USER u ON ts.user_id = u.pk
WHERE ts.deleted = 0
GROUP BY u.pk, DATE(ts.start_time)
ORDER BY date DESC, hours DESC;
```

### 5. Task-Hierarchie anzeigen

```sql
-- Recursive CTE für Parent-Task-Hierarchie
WITH RECURSIVE task_path(pk, title, parent_task_id, level, path) AS (
    SELECT pk, title, parent_task_id, 0, title
    FROM T_TASK
    WHERE parent_task_id IS NULL
    UNION ALL
    SELECT t.pk, t.title, t.parent_task_id, tp.level + 1, tp.path || ' > ' || t.title
    FROM T_TASK t
    JOIN task_path tp ON t.parent_task_id = tp.pk
)
SELECT level, path, pk FROM task_path ORDER BY path;
```

### 6. Top 10 Tasks nach Zeitaufwand

```sql
SELECT
    t.title,
    t.status,
    COUNT(ts.pk) as timesheet_count,
    ROUND(SUM((julianday(ts.stop_time) - julianday(ts.start_time)) * 24), 2) as total_hours
FROM T_TASK t
LEFT JOIN T_TIMESHEET ts ON t.pk = ts.task_id AND ts.deleted = 0
WHERE t.deleted = 0
GROUP BY t.pk
ORDER BY total_hours DESC
LIMIT 10;
```

### 7. Zeitberichte eines bestimmten Users

```sql
SELECT
    DATE(ts.start_time) as date,
    TIME(ts.start_time) as start,
    TIME(ts.stop_time) as stop,
    ROUND((julianday(ts.stop_time) - julianday(ts.start_time)) * 24, 2) as hours,
    task.title,
    ts.description
FROM T_TIMESHEET ts
JOIN T_PF_USER u ON ts.user_id = u.pk
JOIN T_TASK task ON ts.task_id = task.pk
WHERE u.username = 'username_hier' AND ts.deleted = 0
ORDER BY ts.start_time DESC;
```

### 8. Wochentage-Analyse

```sql
SELECT
    CASE CAST(strftime('%w', start_time) AS INTEGER)
        WHEN 0 THEN 'Sonntag'
        WHEN 1 THEN 'Montag'
        WHEN 2 THEN 'Dienstag'
        WHEN 3 THEN 'Mittwoch'
        WHEN 4 THEN 'Donnerstag'
        WHEN 5 THEN 'Freitag'
        WHEN 6 THEN 'Samstag'
    END as weekday,
    COUNT(*) as timesheet_count,
    ROUND(SUM((julianday(stop_time) - julianday(start_time)) * 24), 2) as total_hours
FROM T_TIMESHEET
WHERE deleted = 0
GROUP BY strftime('%w', start_time)
ORDER BY CAST(strftime('%w', start_time) AS INTEGER);
```

## Abfragen für Finanzbuchhaltung

### 9. Auftragswert pro Kunde

```sql
SELECT
    k.name as kunde,
    k.identifier,
    COUNT(a.pk) as anzahl_auftraege,
    COUNT(ap.pk) as anzahl_positionen,
    ROUND(SUM(ap.nettoSumme), 2) as gesamt_netto
FROM T_FIBU_KUNDE k
LEFT JOIN T_FIBU_AUFTRAG a ON a.kunde_fk = k.pk AND a.deleted = 0
LEFT JOIN T_FIBU_AUFTRAG_POSITION ap ON ap.auftrag_fk = a.pk AND ap.deleted = 0
WHERE k.deleted = 0
GROUP BY k.pk
ORDER BY gesamt_netto DESC;
```

### 10. Offene Rechnungen

```sql
SELECT
    r.nummer as rechnung_nr,
    k.name as kunde,
    r.datum as rechnungsdatum,
    r.faelligkeit,
    r.status,
    ROUND(SUM(rp.menge * rp.einzelNetto * (1 + rp.vat/100)), 2) as brutto_summe,
    CASE
        WHEN r.bezahl_datum IS NULL THEN 'Offen'
        ELSE 'Bezahlt'
    END as zahlstatus
FROM T_FIBU_RECHNUNG r
JOIN T_FIBU_KUNDE k ON r.kunde_id = k.pk
LEFT JOIN T_FIBU_RECHNUNG_POSITION rp ON rp.rechnung_fk = r.pk AND rp.deleted = 0
WHERE r.deleted = 0 AND r.bezahl_datum IS NULL
GROUP BY r.pk
ORDER BY r.faelligkeit;
```

### 11. Rechnungsumsatz pro Monat

```sql
SELECT
    strftime('%Y-%m', r.datum) as monat,
    COUNT(r.pk) as anzahl_rechnungen,
    ROUND(SUM(rp.menge * rp.einzelNetto), 2) as netto_summe,
    ROUND(SUM(rp.menge * rp.einzelNetto * (1 + rp.vat/100)), 2) as brutto_summe
FROM T_FIBU_RECHNUNG r
LEFT JOIN T_FIBU_RECHNUNG_POSITION rp ON rp.rechnung_fk = r.pk AND rp.deleted = 0
WHERE r.deleted = 0
GROUP BY monat
ORDER BY monat DESC;
```

### 12. Projekt-Übersicht mit Aufträgen und Rechnungen

```sql
SELECT
    p.name as projekt,
    p.identifier,
    k.name as kunde,
    COUNT(DISTINCT a.pk) as anzahl_auftraege,
    COUNT(DISTINCT r.pk) as anzahl_rechnungen,
    ROUND(SUM(DISTINCT ap.nettoSumme), 2) as auftrag_summe,
    ROUND(SUM(DISTINCT rp.menge * rp.einzelNetto), 2) as rechnung_summe,
    p.status
FROM T_FIBU_PROJEKT p
LEFT JOIN T_FIBU_KUNDE k ON p.kunde_id = k.pk
LEFT JOIN T_FIBU_AUFTRAG a ON a.projekt_fk = p.pk AND a.deleted = 0
LEFT JOIN T_FIBU_AUFTRAG_POSITION ap ON ap.auftrag_fk = a.pk AND ap.deleted = 0
LEFT JOIN T_FIBU_RECHNUNG r ON r.projekt_id = p.pk AND r.deleted = 0
LEFT JOIN T_FIBU_RECHNUNG_POSITION rp ON rp.rechnung_fk = r.pk AND rp.deleted = 0
WHERE p.deleted = 0
GROUP BY p.pk
ORDER BY projekt;
```

### 13. Zahlungspläne - Übersicht

```sql
SELECT
    a.nummer as auftrag_nr,
    a.titel,
    ps.schedule_date,
    ps.amount,
    ps.reached,
    ps.vollstaendigFakturiert,
    CASE
        WHEN ps.reached = 1 THEN 'Erreicht'
        ELSE 'Offen'
    END as status
FROM T_FIBU_PAYMENT_SCHEDULE ps
JOIN T_FIBU_AUFTRAG a ON ps.auftrag_fk = a.pk
WHERE ps.deleted = 0
ORDER BY ps.schedule_date;
```

## Abfragen für Kostenrechnung

### 14. Buchungssätze nach Kostenträger (Kost2)

```sql
SELECT
    k2.nummernkreis || '.' || k2.bereich || '.' || k2.teilbereich as kost2_nr,
    k2.description as kost2_bezeichnung,
    k2art.name as kost2_art,
    COUNT(b.pk) as anzahl_buchungen,
    ROUND(SUM(b.betrag), 2) as summe
FROM T_FIBU_KOST2 k2
LEFT JOIN T_FIBU_KOST2ART k2art ON k2.kost2_art_id = k2art.pk
LEFT JOIN T_FIBU_BUCHUNGSSATZ b ON b.kost2_id = k2.pk AND b.deleted = 0
WHERE k2.deleted = 0
GROUP BY k2.pk
ORDER BY kost2_nr;
```

### 15. Monatliche Buchungssätze

```sql
SELECT
    b.year || '-' || printf('%02d', b.month) as monat,
    COUNT(*) as anzahl_buchungen,
    ROUND(SUM(CASE WHEN b.sh = 'S' THEN b.betrag ELSE 0 END), 2) as soll,
    ROUND(SUM(CASE WHEN b.sh = 'H' THEN b.betrag ELSE 0 END), 2) as haben
FROM T_FIBU_BUCHUNGSSATZ b
WHERE b.deleted = 0
GROUP BY b.year, b.month
ORDER BY b.year DESC, b.month DESC;
```

### 16. Kost2 Auslastung (mit Timesheets)

```sql
SELECT
    k2.nummernkreis || '.' || k2.bereich || '.' || k2.teilbereich as kost2_nr,
    k2.description,
    p.name as projekt,
    COUNT(ts.pk) as anzahl_zeitberichte,
    ROUND(SUM((julianday(ts.stop_time) - julianday(ts.start_time)) * 24), 2) as stunden
FROM T_FIBU_KOST2 k2
LEFT JOIN T_FIBU_PROJEKT p ON k2.projekt_id = p.pk
LEFT JOIN T_TIMESHEET ts ON ts.kost2_id = k2.pk AND ts.deleted = 0
WHERE k2.deleted = 0
GROUP BY k2.pk
HAVING stunden > 0
ORDER BY stunden DESC;
```

## Abfragen für Personal & Urlaub

### 17. Urlaubstage pro Employee

```sql
SELECT
    e.staffNumber as personal_nr,
    u.firstname || ' ' || u.lastname as name,
    COUNT(v.pk) as anzahl_urlaube,
    SUM(julianday(v.end_date) - julianday(v.start_date) + 1) as urlaubstage_gesamt,
    v.vacation_status
FROM T_FIBU_EMPLOYEE e
JOIN T_PF_USER u ON e.user_id = u.pk
LEFT JOIN T_EMPLOYEE_VACATION v ON v.employee_id = e.pk AND v.deleted = 0
WHERE e.deleted = 0
GROUP BY e.pk
ORDER BY name;
```

### 18. Urlaubskalender (aktueller Monat)

```sql
SELECT
    v.start_date,
    v.end_date,
    julianday(v.end_date) - julianday(v.start_date) + 1 as tage,
    ue.staffNumber as employee_nr,
    u.firstname || ' ' || u.lastname as employee,
    v.vacation_status as status,
    v.comment
FROM T_EMPLOYEE_VACATION v
JOIN T_FIBU_EMPLOYEE ue ON v.employee_id = ue.pk
JOIN T_PF_USER u ON ue.user_id = u.pk
WHERE v.deleted = 0
  AND strftime('%Y-%m', v.start_date) = strftime('%Y-%m', 'now')
ORDER BY v.start_date;
```

### 19. Urlaubsvertretungen - Übersicht

```sql
SELECT
    v.start_date,
    v.end_date,
    ue.staffNumber as urlaub_employee,
    u1.firstname || ' ' || u1.lastname as im_urlaub,
    ve.staffNumber as vertretung_nr,
    u2.firstname || ' ' || u2.lastname as vertretung
FROM T_EMPLOYEE_VACATION v
JOIN T_FIBU_EMPLOYEE ue ON v.employee_id = ue.pk
JOIN T_PF_USER u1 ON ue.user_id = u1.pk
JOIN T_FIBU_EMPLOYEE ve ON v.replacement_id = ve.pk
JOIN T_PF_USER u2 ON ve.user_id = u2.pk
WHERE v.deleted = 0
ORDER BY v.start_date DESC;
```

### 20. Urlaubskonto-Entwicklung

```sql
SELECT
    e.staffNumber,
    u.firstname || ' ' || u.lastname as name,
    lac.date,
    lac.amount,
    lac.description,
    SUM(lac.amount) OVER (PARTITION BY e.pk ORDER BY lac.date) as kontostand
FROM T_EMPLOYEE_LEAVE_ACCOUNT_ENTRY lac
JOIN T_FIBU_EMPLOYEE e ON lac.employee_id = e.pk
JOIN T_PF_USER u ON e.user_id = u.pk
WHERE lac.deleted = 0
ORDER BY e.staffNumber, lac.date;
```

### 21. Employee Übersicht mit Kost1

```sql
SELECT
    e.staffNumber,
    u.firstname || ' ' || u.lastname as name,
    e.position_text as position,
    e.division,
    e.abteilung,
    k1.nummernkreis || '.' || k1.bereich || '.' || k1.teilbereich || '.' || k1.endziffer as kost1,
    e.eintritt as eintrittsdatum,
    e.austritt as austrittsdatum
FROM T_FIBU_EMPLOYEE e
JOIN T_PF_USER u ON e.user_id = u.pk
LEFT JOIN T_FIBU_KOST1 k1 ON e.kost1_id = k1.pk
WHERE e.deleted = 0
ORDER BY e.staffNumber;
```

## Export-Befehle

### CSV Export

```sql
.mode csv
.output statistics.csv
SELECT ... FROM ...;
.output stdout
```

### HTML Export

```sql
.mode html
.output report.html
SELECT ... FROM ...;
.output stdout
```

### Markdown Tabelle

```sql
.mode markdown
SELECT ... FROM ...;
```

### CSV Export direkt aus der Shell

```bash
sqlite3 -csv projectforge_export.db "SELECT * FROM T_TIMESHEET" > timesheets.csv
```

## Nützliche SQLite-Funktionen

### Datumsfunktionen

- `DATE(timestamp)` - Nur Datum (YYYY-MM-DD)
- `TIME(timestamp)` - Nur Uhrzeit (HH:MM:SS)
- `strftime('%Y-%m', timestamp)` - Jahr-Monat
- `strftime('%w', timestamp)` - Wochentag (0=Sonntag, 6=Samstag)
- `strftime('%W', timestamp)` - Wochennummer
- `julianday(timestamp)` - Für Datumsberechnungen

### Aggregation

- `COUNT(*)` - Anzahl Zeilen
- `COUNT(DISTINCT column)` - Anzahl unterschiedlicher Werte
- `SUM(column)` - Summe
- `AVG(column)` - Durchschnitt
- `MIN(column)` / `MAX(column)` - Minimum/Maximum

### String-Funktionen

- `||` - String-Konkatenation (z.B. `firstname || ' ' || lastname`)
- `UPPER(text)` / `LOWER(text)` - Groß-/Kleinschreibung
- `SUBSTR(text, start, length)` - Teilstring
- `REPLACE(text, old, new)` - Text ersetzen

### Bedingte Logik

- `CASE WHEN condition THEN value ELSE other END` - If-Then-Else
- `COALESCE(value1, value2, ...)` - Erster nicht-NULL Wert
- `IFNULL(value, default)` - NULL-Behandlung

## Tipps

1. **Performance**: Für große Datenmengen Indizes nutzen (bereits im Schema vorhanden)
2. **Debugging**: `.explain` vor einer Abfrage zeigt den Ausführungsplan
3. **Formatierung**: `.mode column` mit `.width auto` für gut lesbare Ausgabe
4. **Persistent**: Einstellungen in `~/.sqliterc` speichern
5. **Batch-Modus**: Abfragen aus Datei ausführen: `sqlite3 db.db < queries.sql`

## Weitere Ressourcen

- SQLite Dokumentation: https://www.sqlite.org/docs.html
- SQL Tutorial: https://www.sqlitetutorial.net/
- Datums- und Zeitfunktionen: https://www.sqlite.org/lang_datefunc.html
