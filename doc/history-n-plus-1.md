# History-Laden: N+1-Thematik

> Arbeitsnotiz zum Untersuchen und Optimieren des History-Ladens.
> Status: Re-Index-Strang **umgesetzt** (siehe Abschnitt „Entscheidung" + „Empfehlung");
> App-seitiges `loadHistory` als nachrangiges Backlog offen.

> **Hinweis zum Ist-Stand (nach Umsetzung):** Die Abschnitte „Symptom", „Auslöser",
> „Einordnung" und „Zwei getrennte Pfade" beschreiben den **ursprünglichen** Zustand
> (`HistoryEntryDO`/`HistoryEntryAttrDO` waren `@Indexed`, stündlicher
> `CronReindexingHourlyJob`). Beides ist inzwischen entfernt — der komplette
> History-Reindex und damit der beschriebene Load-Sturm existieren nicht mehr. Sie bleiben
> als Herleitung stehen; der Ist-Stand steht unter „Entscheidung" und „Empfehlung".

## Symptom

Im Log erscheinen dutzende wiederholte, praktisch identische Statements:

```sql
select ... from t_pf_history_attr head1_0 where head1_0.pk = any (?)
select ... from t_pf_history     hed1_0  where hed1_0.pk  = any (?)
```

Sie wechseln sich ab und wiederholen sich viele Male hintereinander.

## Auslöser: der Re-Index-Job (nicht der App-Ladepfad!)

**Update:** Die Statements stammen aus dem **Hibernate-Search-MassIndexer**, nicht aus dem
normalen History-Laden der Anwendung. Beleg aus dem Log:

```
o.p.f.p.database.IndexProgressMonitor : GroupTaskAccessDO: Starting indexing...
select count(pk) from T_GROUP_TASK_ACCESS
select pk from T_GROUP_TASK_ACCESS
select ... from T_GROUP_TASK_ACCESS gtad1_0 (mit Joins auf Group/Task) ...
```

Muster des MassIndexer: `count(pk)` → `select pk` (alle Ids) → dann die Entitäten
**in Id-Batches** laden (`where pk = any (?)`). Genau das erzeugt die wiederholten
`t_pf_history` / `t_pf_history_attr`-Selects: `HistoryEntryDO` ist `@Indexed`, und der
Indexer streamt alle Zeilen batchweise durch.

**Wichtige Korrektur zur Einordnung:** `pk = any (?)` ist hier das **erwartete,
effiziente** Batch-Laden des MassIndexers — *kein* zu behebendes N+1. Es ist auch
nicht der `join fetch`-Pfad der NamedQueries (der greift nur beim App-seitigen
`loadHistory`, siehe unten).

Relevante Stellen:
- `DatabaseDao.kt:71` `reindex`, `:92` `reindexObjects`, `:110` `reindexSuspending`,
  `:129` `searchSession.massIndexer(clazz)`.
- `IndexProgressMonitor.kt` — die `Starting indexing...`-Logmeldung.
- ~~`CronReindexingHourlyJob.java` — der stündliche Auslöser.~~ (inzwischen gelöscht, s.u.)

## Einordnung

- `pk = any (?)` ist Hibernate 6's Array-Form von `IN (…)`, also **Batch-Fetching**.
  Innerhalb *eines* Aufrufs bündelt Hibernate bereits — es ist **kein** Statement pro Zeile.
- Im Re-Index-Kontext ist dieses Batch-Laden by-Id das **normale, gewollte** Verhalten
  des MassIndexers (siehe oben). Kein zu behebendes N+1.
- Die NamedQuery mit `left join fetch m.attributes` (unten) ist ein **anderer** Pfad —
  sie wird beim App-seitigen `loadHistory` verwendet, **nicht** beim Re-Index.
- Zu klären bleibt nur: Ist das `t_pf_history_attr`-Batch-Laden während des Indexierens
  überhaupt nötig (werden Attribute für das Lucene-Dokument gebraucht?) und ist die
  Batch-Größe sinnvoll gewählt.

## Relevante Code-Stellen

- `HistoryEntryDO.kt:54-62` — NamedQueries `SELECT_HISTORY_FOR_BASEDO` /
  `SELECT_HISTORY_BY_ENTITY_IDS`, beide mit `left join fetch m.attributes`.
- `HistoryEntryDO.kt:127-135` — `attributes` als `@OneToMany(fetch = LAZY)`.
- `HistoryService.kt:262-297` — `loadAndMergeHistory` (Einzel-Id und Id-Collection).
- `HistoryService.kt:299-392` — `processAndMergeHistory`:
  - Zeile 314-390: für jede `@OneToMany`-Property werden eingebettete Objekte gesammelt.
  - **Zeile 381-389:** **rekursiver** Aufruf `loadAndMergeHistory(clazz, entityIds, …)`
    pro eingebetteter Klasse (z.B. `AuftragDO` → `AuftragsPositionDO`).

## Erkenntnis: Der `t_pf_history_attr`-Index ist überflüssig (verifiziert)

Untersuchung, ob die Attribute fürs Indexieren gebraucht werden — Ergebnis: **nein.**

1. **`HistoryEntryDO`** (`@Indexed`, `HistoryEntryDO.kt:79`) indexiert nur zwei Felder:
   `entityName` und `userComment` (`@GenericField`, Zeilen 91/124). `modifiedBy`,
   `modifiedAt`, `entityId` sind auskommentiert. Die `attributes`-Collection ist
   **nicht** `@IndexedEmbedded` (`HistoryEntryDO.kt:127-135`) → Indexieren eines
   History-Eintrags braucht die Attribute **nicht**.
2. **`HistoryEntryAttrDO`** (`@Indexed`, `HistoryEntryAttrDO.kt:74`) hat **kein einziges
   indexiertes Feld** — kein `@GenericField`/`@FullTextField` auf `value`, `oldValue`,
   `propertyName`, `propertyTypeClass`; `@ClassBridge` auskommentiert. Der MassIndexer
   lädt trotzdem alle `t_pf_history_attr`-Zeilen (25er-Batches) und baut **leere
   Lucene-Dokumente**. → Das ist der beobachtete Load-Sturm.
3. **Diese Indizes werden per Volltext nirgends durchsucht.** History-Suche läuft über
   `DBQuery` → `DBHistoryQuery.searchHistoryEntryByCriteria`, eine JPA-CriteriaBuilder/
   SQL-Abfrage (`DBHistoryQuery.kt:40,81`), **nicht** Hibernate Search. Der Code sagt es
   selbst: `DBQuery.kt:179-181` — *"a full text search over this index can answer none of
   them."*

### Suche nach alten Werten ist bereits abgedeckt (per SQL)

Wichtig für die Frage „müssen wir `old_value` nicht indizieren?": **Nein.** Die Suche in
der Historie nach (alten) Werten **existiert schon** — SQL-basiert, nicht Lucene.
In `DBHistoryQuery.kt:68-80` joint die Criteria-Abfrage auf `t_pf_history_attr` und prüft
den Suchstring per `LIKE` gegen **beide** Spalten:

```kotlin
val attributes = root.join<HistoryEntryDO, Any>("attributes")
cb.or(
    cb.like(cb.lower(attributes.get<String>("value")),    pattern),
    cb.like(cb.lower(attributes.get<String>("oldValue")), pattern),  // <-- old_value wird durchsucht
)
```

Der Kommentar dort erklärt auch, warum beide nötig sind: ein Insert-Eintrag hat gar keinen
old value, würde also bei reiner `old_value`-Suche nie gefunden. → Ein `@FullTextField` auf
`old_value` würde nur einen zweiten, ungenutzten Suchweg bauen. Der SQL-Pfad deckt es ab.

Einziges denkbares Argument für einen Lucene-Index: die SQL-`LIKE`-Suche mit `%term%` kann
keinen DB-Index nutzen und wird bei sehr großen History-Tabellen langsam. Das wäre aber ein
bewusster Umbau von `DBHistoryQuery` auf Hibernate Search — nicht der Ist-Zustand. Solange
die Suche SQL-basiert bleibt, ist der Attr-Index reiner Overhead.

### Entscheidung (gemessen am Produktivabzug, PostgreSQL 16): pg_trgm-Index

Der Produktivabzug ist groß: `t_pf_history` 3,66 Mio. Zeilen / 916 MB, **`t_pf_history_attr`
37,6 Mio. Zeilen / 7,5 GB** (avg. `value` 11 Zeichen, `old_value` 20 — die 50000er-
Spaltenbreite ist reine Obergrenze). Die SQL-`LIKE '%term%'`-Suche (siehe oben) kann keinen
Btree nutzen und war entsprechend langsam:

| Suche | vorher | nachher |
|---|---|---|
| AuftragDO (53k), seltener Begriff | ~1,65 s | **5,7 ms** |
| TimesheetDO (2,1 Mio.), seltener Begriff | 4,36 s | **448 ms** |
| AuftragDO, sehr häufiger Begriff („test") | 1,65 s | ~1,13 s |

Gewählt: **PostgreSQL `pg_trgm` GIN-Indizes** auf `lower(value)`/`lower(old_value)` plus ein
Btree auf `entity_name`. Das beschleunigt das *bestehende* `LIKE`-Prädikat direkt (kein
Umbau von `DBHistoryQuery`, Substring-Semantik unverändert) und vermeidet den Lucene-
Reindex-Sturm, den ein `@FullTextField` auf 37,6 Mio. Zeilen erzeugen würde. GIN-Build-Zeit
gemessen: value ~99 s / 639 MB, old_value ~7 s / 46 MB, entity_name ~2 s. Migration:
`flyway/migrate/postgresql/V8.0.24__RELEASE-HistoryAttr_Trigram_Indexes.sql`.

Sehr häufige Begriffe bleiben durch die reine Treffermenge begrenzt (kein Index hilft, wenn
„alles was 'test' enthält" gesucht wird); der `entity_name`-Btree ersetzt dort immerhin den
3,66-Mio.-Seq-Scan durch einen Bitmap-Scan. HSQLDB (nur Dev/Test) behält den `LIKE`-Fallback,
kein Gegenstück-Migration nötig.

### Empfehlung

- **Umgesetzt:** `@Indexed` von `HistoryEntryAttrDO` entfernt. Baute nur leere Dokumente,
  wurde nie durchsucht → eliminiert den kompletten `t_pf_history_attr`-Load beim Re-Index
  samt Index-Pflege. Verifiziert ungefährlich: der Reindex-All-Pfad iteriert
  `Search.mapping(...).allIndexedEntities()` (`HibernateSearchReindexer.kt:70/131`), und
  `ReindexerRegistry` registriert nur `HistoryEntryDO` explizit — die Attr-DO war nur wegen
  `@Indexed` in der Liste.
- **Umgesetzt:** SQL-Wertsuche per `pg_trgm`-GIN + `entity_name`-Btree beschleunigt (Migration
  V8.0.24), siehe Messungen oben.
- **Umgesetzt:** `@Indexed` auch von `HistoryEntryDO` entfernt. Die History-Suche ist
  criteria-/SQL-basiert (`DBHistoryQuery`), der Lucene-Index wurde nie durchsucht. Damit
  entfällt der komplette History-Reindex. Mitgeräumt: `reindexClasses4NewestEntries`/
  `reindexClasses` in `BaseDao` ohne `HistoryEntryDO`, `ReindexerRegistry`-Registrierung
  entfernt (Fallback-Strategien bleiben), `HibernateSearchReindexer`-Sonderfall entfernt,
  der dormant gewordene `CronReindexingHourlyJob` (einziger Zweck: History-Reindex) gelöscht.

## Zwei getrennte Pfade — nicht verwechseln

1. **Re-Index-Job (ursprünglicher Auslöser, inzwischen entfernt):** Der MassIndexer lud
   `HistoryEntryDO` & `HistoryEntryAttrDO` batchweise per Id (`pk = any (?)`) — erwartetes
   Verhalten, aber für einen Index, der nie durchsucht wurde. Da beide DOs nicht mehr
   `@Indexed` sind, entfällt dieser Pfad komplett; der beschriebene Load-Sturm tritt nicht
   mehr auf.
2. **App-seitiges `loadHistory` (separates Thema, weiterhin relevant):** nutzt die NamedQuery mit
   `join fetch` und rekursiert über eingebettete `@OneToMany`-Objekte
   (`processAndMergeHistory:381`). Hier *kann* ein N+1 auf Objekt-/Rekursionsebene
   entstehen, wenn Aufrufer pro Entität statt gebündelt laden — **derzeit aber nicht
   die Quelle des beobachteten Logs.**

## Offene Fragen / TODO

Re-Index:
- [x] Werden die History-**Attribute** fürs Lucene-Dokument gebraucht? → **Nein** (siehe oben).
- [x] Müssen wir `old_value` indizieren, um in der Historie nach alten Werten zu suchen?
      → **Nein.** Diese Suche läuft bereits SQL-basiert über beide Spalten
      (`DBHistoryQuery.kt:68-80`), nicht über Lucene.
- [x] `@Indexed` von `HistoryEntryAttrDO` entfernen (leere Dokumente, nie durchsucht). → erledigt.
- [x] SQL-Wertsuche für große History-Tabelle beschleunigen → `pg_trgm`-GIN + `entity_name`-Btree
      (Migration V8.0.24), gemessen 1,6–4,4 s → 5–450 ms für selektive Begriffe.
- [x] `@Indexed` auch von `HistoryEntryDO` entfernt (Suche ist criteria-/SQL-basiert); die
      Reindex-Plumbing (BaseDao-Listen, `ReindexerRegistry`, `HibernateSearchReindexer`-
      Sonderfall, dormanter `CronReindexingHourlyJob`) mitentfernt. → erledigt.
- [ ] Vor Prod-Rollout: prüfen, ob der Prod-DB-User `CREATE EXTENSION pg_trgm` darf
      (trusted extension seit PG13 → CREATE-Recht auf DB reicht; sonst DBA vorab).

App-seitiges loadHistory:
- [x] Geprüft, ob `loadHistory` pro Objekt in einer Schleife aufgerufen wird → **ja**, in den
      `addOwnHistoryEntries`-Overrides: je Kind-Instanz ein eigener Query (N+1). Auf die
      Collection-Variante `loadAndMergeHistory(entityClass, entityIds, context)` umgestellt in
      `AuftragDao`, `RechnungDao`, `EingangsrechnungDao`, `EmployeeDao`, `ProjektDao`,
      `CurrencyPairDao` (je Kind-Klasse jetzt ein Query statt N; bei Rechnung/Eingangsrechnung
      werden die `kostZuweisungen` aller Positionen zu einem Call aggregiert). → erledigt.
      - Bewusst **nicht** umgestellt: `HRPlanningDao` und `TeamEventDao`. Beide setzen den
        Anzeige-Präfix per **pro-Eintrag**-`customize`-Callback (`projektName`/`status` bzw.
        `attendee.toString()`), den die Collection-Variante nicht ausdrücken kann. Ein Batchen
        hier bräuchte erst eine per-Entity-Präfix-Zuordnung in `processAndMergeHistory`.
- [x] `@BatchSize` auf `HistoryEntryAttrDO` / der `attributes`-Collection geprüft → **nicht
      nötig.** Die History-NamedQueries (`SELECT_HISTORY_FOR_BASEDO`/`…_BY_ENTITY_IDS`) laden
      die Attribute bereits per `left join fetch m.attributes`, d.h. innerhalb *eines* Querys —
      es gibt hier keinen lazy-per-Attribut-N+1, den `@BatchSize` mildern würde. Ein globales
      `hibernate.default_batch_fetch_size` existiert nirgends; das wäre ein app-weiter Eingriff
      ohne Bezug zum History-Pfad und bleibt bewusst außen vor.

Allgemein:
- [x] Statement-Zahl vor/nach der Umstellung gemessen (am Produktivabzug, PostgreSQL 16). Die
      alte Zahl ist deterministisch aus den Kind-Zahlen ableitbar (ein `loadAndMergeHistory`-Call
      je Kind-Instanz, jeder in eigenem `runReadOnly`), die neue ist ein Call je Kind-Klasse:

      | Parent (dickster im Dump) | Kinder | alt (Child-Querys) | neu | + Parent = gesamt alt→neu |
      |---|---|---|---|---|
      | RechnungDO `1288095` | 28 Pos. + 69 KostZuw. | 97 | 2 | 98 → 3 |
      | AuftragDO `32507562` | 84 Pos. + 0 Payment | 84 | 1 | 85 → 2 |

      Server-Zeit ist dabei **nicht** der Engpass: die gebündelte Query über *alle* 28 Positionen
      der Rechnung `1288095` läuft in **0,164 ms** — praktisch so teuer wie *eine* der alten
      Einzel-Querys (0,115 ms, `ix_pf_history_ent`-Index-Scan). Der Gewinn ist also fast
      vollständig das Wegfallen von ~95 JDBC-Round-Trips und ebenso vielen `runReadOnly`-
      Transaktions-/EntityManager-Setups pro History-Aufruf.
- [ ] Optional zur Bestätigung am laufenden System: `spring.jpa.properties.hibernate.generate_statistics=true`
      setzen und die o.g. Rechnung/den Auftrag in der UI öffnen — die Query-Zählung von
      Hibernate muss die Tabelle bestätigen (reproduziert nur die deterministischen Werte oben).

## Mögliche Maßnahmen (Ideen, noch nicht bewertet)

- Re-Index: Attribut-Ladung während des Indexierens vermeiden bzw. Batch-Größe erhöhen.
- App-Pfad: Aufrufer bündeln (`loadAndMergeHistory(entityClass, entityIds, …)` statt N Einzelaufrufe).
- Eingebettete Objekte in einem Zug über alle Klassen/Ids laden.
