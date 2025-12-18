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
