# History loading: N+1 and the history index

Working note, done. The history is searched by SQL (`DBHistoryQuery`), never by Lucene.

## What was done

- `HistoryEntryDO` and `HistoryEntryAttrDO` are no longer `@Indexed`. The hourly `CronReindexingHourlyJob`
  (its only purpose was the history re-index) and the related plumbing (`ReindexerRegistry`, special case in
  `HibernateSearchReindexer`, `BaseDao` reindex lists) are removed. The load storm of the MassIndexer on
  `t_pf_history_attr` (tens of millions of rows) is gone.
- The SQL value search (`LIKE '%term%'` on `value` / `old_value`) is accelerated on PostgreSQL by `pg_trgm` GIN
  indexes on `lower(value)` / `lower(old_value)` plus a btree on `entity_name`
  (`flyway/migrate/postgresql/V8.0.24__RELEASE-HistoryAttr_Trigram_Indexes.sql`). HSQLDB (dev/test) keeps the
  plain `LIKE`. Measured on a large PostgreSQL 16 database:

  | Search | before | after |
  |---|---|---|
  | orders (tens of thousands), rare term | ~1.65 s | 5.7 ms |
  | timesheets (millions), rare term | 4.36 s | 448 ms |
  | orders, very common term | 1.65 s | ~1.13 s |

  Very common terms stay bounded by the number of hits; no index helps there.
- The app-side `addOwnHistoryEntries` overrides called `loadHistory` once per child instance (N+1). They use
  `loadAndMergeHistory(entityClass, entityIds, context)` now (one query per child class) in `AuftragDao`,
  `RechnungDao`, `EingangsrechnungDao`, `EmployeeDao`, `ProjektDao` and `CurrencyPairDao`. An invoice with
  ~100 children went from ~100 child queries to 2, an order with ~80 positions from ~80 to 1.
- `@BatchSize` on the attributes is not needed: the history named queries already `left join fetch` them.

## Left open

- **Rollout:** the database user needs the right to `CREATE EXTENSION pg_trgm` (a trusted extension since
  PostgreSQL 13, so the CREATE right on the database is enough; otherwise create it beforehand as superuser).
  Documented in the admin guide.
- **Deliberately not batched:** `HRPlanningDao` and `TeamEventDao` set their display prefix with a per-entry
  `customize` callback that the collection variant cannot express. Batching them needs a per-entity prefix
  mapping in `processAndMergeHistory` first.
- Optional confirmation on a running system: `spring.jpa.properties.hibernate.generate_statistics=true` and open
  an invoice or order with many positions; the query count must match the numbers above.
