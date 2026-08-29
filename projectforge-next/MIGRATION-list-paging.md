# Server-side paging for entity lists, and one unified filter state

Detail plan alongside [MIGRATION.md](MIGRATION.md), like
[MIGRATION-calendar.md](MIGRATION-calendar.md). This document holds the analysis so the work can start
without exploring again.

**Status**

| Stage                               | State                                                    |
| ----------------------------------- | -------------------------------------------------------- |
| 1a gzip                             | **done**                                                 |
| 1b lean list row                    | **done** — generic `BaseDTO.copyFrom4ListRow` mechanism  |
| 1c debounced search                 | **done** — `components/shared/list/search-input.tsx`     |
| Re-measure after stage 1            | **done** — live numbers below, and they decide stage 2   |
| 2 server-side paging                | open — analysis complete, nothing implemented            |
| 3a nested sort paths                | **done**                                                 |
| 3b sort in Kotlin for computed cols | **done** — in `filterList`; moves onto the id list later |
| 4 one filter state                  | open — ships together with stage 2 for `order`           |
| 5 optimizations                     | open — behind measurement                                |
| 6 roll out to further entities      | open                                                     |

## Context

The order list took ~10 s to show data. Measured against a real account with the filter cleared:

|                                     |                                      |
| ----------------------------------- | ------------------------------------ |
| Rows / payload                      | **7132 rows, 11.9 MB**, one response |
| Server time (`POST /rs/order/list`) | **3.5 – 5.8 s**                      |
| Browser download                    | **7.1 s**                            |
| `JSON.parse`                        | 29 ms                                |
| Rendering 50 rows (TanStack)        | 32 ms                                |

So the payload is the entire cost. TanStack is innocent — which also explains why the
`setTimeout took 213ms` violations looked two orders of magnitude too small.

**Why the whole table comes over the wire.** `AbstractPagesRest.getCurrentFilter` resets `maxRows` to
50 000 on every call, and the pagination plumbing is commented out in three places
(`MagicFilterProcessor.doIt`, `QueryFilter.createDBFilter`, `DBFilter`). `MagicFilter` has no offset at
all; `paginationPageSize` is a derived val read out of an `entries` element. Paging is deliberately
client-side because column filters must narrow _before_ paging (see the comment in
`components/data-table/use-magic-filter-query.ts`). Consequence: every keystroke in the search box and
every pill edit re-ships 12 MB.

**Two things measured that reshape the plan.** Responses carried no `Content-Encoding`, and the same
payload gzips to **1.5 MB** — 8×, from one property. And `getInitialList` already skips the query for a
next client, so there is exactly one full scan per load, not two.

Goal: the order list feels instant, and the three filter layers (column filters, pills, stored
favorites) become one coherent state instead of three that can silently disagree.

---

## Stage 1 — the cheap wins, then re-measure — done

No contract change, and it decided how urgent the rest is.

**1a. Enable gzip. — done.** `projectforge-business/src/main/resources/application.properties` now sets
`server.compression.enabled`, a JSON/HTML/CSS/JS mime list and `min-response-size=2048`. Verified:
12.5 MB → 1.5 MB. Check no reverse proxy in the deployment compresses already, or the work is done
twice.

**1b. Trim the list row. — done, as a generic mechanism.** Not the list-only DTO this document first
proposed (`OrderListRow.kt`, after the `ListAddress` precedent): more entities need this, so the
question "what does a row of this entity consist of?" belongs to the DTO, not to a second class per
page. What was built instead — a **third filling of the same DTO**, beside `copyFromMinimal` and
`copyFrom`:

- `BaseDTO.copyFrom4ListRow(src)` — defaults to `copyFrom`, so an entity whose rows are small enough
  (book, cost unit) needs nothing. Overriding it is what opts an entity in.
- `AbstractPagesRest.useListRow(request)` — the switch: next client **and** the page is in
  `NextMigration`, i.e. the same condition as `skipResultSet` and for the same reason. Only a hand
  built page knows which columns it renders; every other client renders from `UILayout`, whose columns
  bind to fields a lean row leaves empty.
- `AbstractPagesRest.createListRow(obj)` / `AbstractDTOPagesRest.newDTO()` — the DTO page implements
  the row generically; an entity opts in with `override fun newDTO() = Auftrag()`. `newDTO` exists
  because Kotlin cannot reach `DTO` at runtime (erasure), and resolving it from the generic supertype
  breaks for a page inheriting through an intermediate class. Override `createListRow` directly only
  when building the row needs something the DTO cannot reach (as `AddressPagesRest` needs its image
  cache).
- `Auftrag.copyFrom4ListRow` — fills the 19 columns of `order.page.tsx`; `JsonInclude.NON_NULL` keeps
  the rest off the wire.

Measured fat, per row ~1755 bytes over 30 fields: the four manager DTOs cost ~524 B/row for a column
showing nothing but the derived `assignedPersons` string; `customer`/`project` are full nested DTOs
where the cell shows one name; every sum travels twice, raw and pre-formatted; and a dozen fields
(`bemerkung`, `statusBeschreibung`, the three access flags, `sendEMailNotification`,
`created`/`lastUpdate`, `bindungsFrist`, `beauftragungs*`, `kundeText`, `forecastType`,
`angebotsDatum`) are read by no column. The four boolean flags still travel (~107 B/row): they are
non-null `Boolean`s, so `NON_NULL` cannot drop them, and making them nullable would push the
false-vs-absent distinction into the edit form.

`customer`/`project` stay nested (an `EntityRefDto` carrying only `displayName`) rather than becoming
flat `customerName`/`projectName` strings: measured, flattening saves a further 21 KB gzipped — 4% —
and the row would stop being a projection of the DTO for it. So `order.page.tsx` keeps its accessors
unchanged. `OrderListRow` in `components/features/order/types.ts` no longer extends `OrderDetail`
though: a row carries a subset, and inheriting the full shape is what let the page reach for fields
that are not there.

**1c. Debounce the search box. — done.** New `components/shared/list/search-input.tsx`: it owns the
typed value and lets it through `useDebouncedValue` at 300 ms, so `ListToolbar` (and with it every
list page) debounces the same way. The caller's value still wins when it changes for another reason —
filter reset, saved filter applied — compared against what was last sent so the user's own typing is
not overwritten while typing.

**Re-measured after 1a+1b (live).** `POST /rs/order/list` against the changed backend, same account
and same 7132 rows:

|               | before       | after         |
| ------------- | ------------ | ------------- |
| wire (gzip)   | 1,534,730 B  | **548,625 B** |
| raw JSON      | 12,517,393 B | 5,281,610 B   |
| per row (raw) | 1755 B       | 741 B         |
| total request | 4.98 s       | **2.68 s**    |

2.80× smaller on the wire, matching the 527 KB projection. What is left is server time: of the 2.68 s
only ~0.4 s is download on a local connection, so ~2.3 s is the pipeline querying, mapping and
access-checking all 7132 rows before sending any of them — which 1b barely touches.

**Verdict on Stage 2.** Risk 1 below asked whether Stage 1 alone suffices. It does not: 2.7 s for a
list of 50 visible rows is still the dominant cost, and it is paid again on every pill edit. But the
remaining cost is now entirely server-side, which shifts the priority — **Stage 5's hybrid fast path
(real `OFFSET/LIMIT` for a user with uniform select access) is worth measuring before committing to
Stage 2's full id-list architecture**, since most finance users see every order anyway. Stage 2 stays
the correct general answer, and Stage 4 and 3b still depend on it.

---

## Stage 2 — server-side paging — open

### The design decision

**Materialize the ordered id list once per (user, filter), cache it in the session, serve pages by
slicing + `getListByIds`.** Not SQL `OFFSET/LIMIT`.

SQL offset is _incorrect_ here, because rows are dropped **after** the database returns them:

1. **Access check per row** — `baseDao.hasSelectAccess(next, loggedInUser)` in `DBQuery`. For orders
   that is `AuftragRight.hasAccess`: group membership, project manager group,
   `AuftragsCache.isVollstaendigFakturiert`, and a 1800-day rule. Not SQL-expressible without
   replicating the group cache in the query, for each of ~40 `*PagesRest` classes.
2. **`resultPredicates`** — predicates supporting neither criteria nor fulltext run in Kotlin
   (`DBQueryBuilder`). In FULLTEXT mode _every_ criteria-only predicate lands here, so the same entity
   filters in SQL or in Kotlin depending on whether the user typed in the search box.
3. **`customResultFilters`** — the order list has four: `OrderEntityRest.preProcessMagicFilter` marks
   `positionsArt`, `positionsStatus`, `positionsPaymentType`, `fakturiert` as `synthetic = true` and
   walks `positionenExcludingDeleted` per row.

A naive `OFFSET/LIMIT` would return short pages with silently skipped rows, and `COUNT(*)` a number
larger than the user may see. Both worse than slow-but-correct.

The id-list approach changes **nothing** in the pipeline — access checks, result predicates, custom
filters, `ensureUniqueSet`, fulltext Kotlin sorting all run once, exactly as today. It also makes
`totalSize` correct for the first time (today it is always `list.size`, see `getList` in
`AbstractPagesRestUtils`), and it makes transient-column sorting possible at all (Stage 3). 7132 `Long`
= 57 KB, so memory is a non-issue.

What it does **not** fix: the _first_ request for a cold filter still pays the pipeline. It does take
the DTO mapping down from 7132 rows to 50 — which Stage 1 did _not_: 1b made each row cheaper, not
fewer. The rest is Stage 5.

### Backend

**Ids-only pass.** In `projectforge-business/.../api/impl/DBQuery.kt`, alongside `select`:

```kotlin
class DBIdResult(val ids: LongArray, val truncated: Boolean)

open fun <O : ExtendedBaseDO<Long>> selectIds(
    baseDao: BaseDao<O>, filter: QueryFilter,
    customResultFilters: List<CustomResultFilter<O>>?, checkAccess: Boolean = true,
): DBIdResult
```

Initially it delegates to the existing `select` and maps to ids — saving nothing on the database, but
removing what actually costs: `Auftrag.copyFrom` × 7132 (`PfCaches.initialize`, `orderInfo`, four
currency formats), the three access checks per row in `OrderEntityRest.transformFromDB`, and 11.9 MB
of Jackson. A `SELECT id` projection is Stage 5, behind measurement — it is the only place semantics
could drift, so it must not be in Stage 2. Mirror as `BaseDao.selectIds`, next to `select`.

**Cache.** New `projectforge-rest/.../core/ListPageCache.kt`, stored via the existing
`ExpiringSessionAttributes` — built for exactly this, per-session objects with a TTL sweeper, already
used by `MultiSelectionSupport`. Key `"listPageIds:$category"`, LRU-capped at 4 entries per category,
30 min TTL. Not `userPrefService`: that persists to the database and these are scratch data.

```kotlin
class CachedIdList(
    val fingerprint: String, val ids: LongArray, val truncated: Boolean,
    val changeCounter: Long, val createdMillis: Long,
)
```

**Fingerprint.** New on `MagicFilter`: `@get:JsonIgnore val resultFingerprint: String` — SHA-256 of the
canonical JSON of everything deciding _which rows in which order_: `entries` (sorted by field,
`paginationPageSize` excluded), `searchString`, `searchHistory`, `deleted`, `sortProperties`,
`autoWildcardSearch`, `extended`, `maxRows`. **Excluded:** `id`/`name` (the favorite reference — must
not invalidate), `multiSelection`. Prefix with a version constant so a release changing filter
semantics invalidates every cached list.

**Invalidation**, three cheap mechanisms: TTL; a per-entity change counter (register one listener on the
existing `BaseDOChangedRegistry` fan-out, bump an `AtomicLong` per `doClass`, compare on read — so
"someone added an order while I was on page 3" self-heals); and explicit `refresh=true` after the
client's own write. A stale list can only produce a _short page_, never a forbidden row, because
`getListByIds` → `BaseDao.select(ids)` → `filterAccess` re-checks per row. Worth saying in the KDoc.

**Serving a page.** New in `AbstractPagesRestUtils.kt`, beside `getList`:

```kotlin
fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>> getListPage(
    request: HttpServletRequest, pagesRest: AbstractPagesRest<O, DTO, B>, baseDao: BaseDao<O>,
    magicFilter: MagicFilter, offset: Int, limit: Int, refresh: Boolean,
): ResultSet<O>
```

1. Multi-selection short circuit — keep the existing branch verbatim; registered ids _are_ the id list,
   so slice them and skip the cache.
2. Cache hit (fingerprint + change counter + TTL) → use it. Miss → build the `QueryFilter` exactly as
   `getObjectList` does (`preProcessMagicFilter`, `AttachmentsFilterSupport.preProcessMagicFilter`,
   `MagicFilterProcessor.doIt`, `postProcessMagicFilter`), `baseDao.selectIds(...)`, then
   `pagesRest.sortIds(...)` (Stage 3), cache.
3. Slice, then `getListByIds(pageIds)` and **restore the order** — `BaseDao.select(idList)` uses an `IN`
   predicate and returns arbitrary order. `associateBy { it.id }` then map over `pageIds`.
4. `ResultSet(page, totalSize = ids.size, offset, limit, ...)`.

Two caveats: `getListByIds` ignores `queryFilter.entityGraphName`, so a page may N+1 where the full
query fetch-joined (acceptable at 50 rows; note it). And `AddressCampaignValuePagesRest` overrides
`getListByIds` — check before enabling paging there.

**Contract: a new path, not an extension of `POST list`.** `POST /rs/{entity}/listPage` with a wrapper
DTO (`filter`, `offset`, `limit`, `refresh`), `RestPaths.LIST_PAGE = "listPage"`.

- Offset must **not** go into `MagicFilter`: that object is the persisted favorite and the argument of
  `isModified`, so every page flip would mark the favorite modified. `paginationPageSize` living as an
  `entries` element is the cautionary tale — see the comment in `AbstractPagesRest.getCurrentFilter`.
- `POST list` stays byte-identical, because the legacy React `DynamicListPageTable.jsx`,
  `startMultiSelections`, and the three `exportAsExcel` endpoints all need every row. Zero regression
  surface there. Wicket is untouched (it calls `BaseDao.select` directly).

`ResultSet` gains `offset: Int?`, `limit: Int?`, `totalSizeExact: Boolean`. `totalSize` changes meaning
only for `listPage`; `size` stays the page size, so the legacy `${data.size}/${data.totalSize}` keeps
working. Gate the `resultSet.size == magicFilter.maxRows` truncation heuristic on `offset == null` and
drive `totalSizeExact` from `DBIdResult.truncated` instead.

**Two blockers to clear in this stage** — places that filter _after_ the pipeline, which would break
both page completeness and `totalSize`: `MyScriptPagesRest.filterList` and
`BankAccountRecordPagesRest.postProcessResultSet` (`doublets`, `checksumErrors`). Convert to
`CustomResultFilter`s (`match(list, element)` receives the accumulated list, which is how
`AuftragsPositionsArtFilter` works) or opt those two entities out of paging. Separately,
`TimesheetPagesRest.postProcessResultSet` folds the whole result set into `resultInfo` statistics —
under paging that silently becomes "sum of this page", so suppress `resultInfo` for paged responses (a
proper aggregate hook is Stage 5).

**Test the invariant the whole design rests on:** for a filter exercising all four order custom filters,
`concat(all pages) == POST list` result, and `totalSize == that size`. Plus: an id deleted between calls
yields a short page, not an exception.

### Frontend

- `lib/rs/client.ts`: `fetchListPage(entity, filter, offset, limit, opts?, signal?)`.
- `use-magic-filter-query.ts`: new `serverPaging?: boolean` (default false, so unmigrated pages are
  untouched). Move `pageSize` out of the filter memo into the request envelope — it is currently inside
  the filter, so a page-size change would needlessly re-materialize the id list. Keep _sending_ the
  entry (the backend stores it, `AGGridSupport` reads it) but exclude it from the fingerprint.
  `queryKey: [...queryKey, filter, pageIndex, pageSize]`. Keep `keepPreviousData` — it is what stops a
  page flip blanking the table. Reset `pageIndex` on any filter/sorting change, not just on search
  (today only `setGlobalFilter` and `applyFilter` do; a stale index past the new end shows an empty
  table under `manualPagination`).
- `hooks/use-entity-list-page.ts`: set `manualPagination: true` and `manualFiltering: true` alongside
  the existing `manualSorting: true`, gated on a new `PageDef.serverPaging`. Both options already exist
  in `components/data-table/use-data-table.ts` and no call site has ever set them. With
  `manualPagination`, `rowCount` is honoured and `DataTablePagination` works off the server total with
  no change to that component.
- Grep for `getFilteredRowModel().rows.length` before shipping; `table.getRowCount()` is fine.

---

## Stage 3 — sorting under paging — done

**3a. Resolve nested sort paths. — done.** `MagicFilterProcessor` stripped everything before the first
dot, so `kunde.displayName` became `displayName`, which `AuftragDO` hasn't got →
`DBQueryBuilderByCriteria.addOrder` swallowed the exception, logged `Can't add order`, and **the order
list came back silently unordered**. It now keeps a nested path that resolves against the entity and
shortens only one that doesn't (the legacy DTO paths this was written for), and `DBCriteriaContext`
creates the `LEFT` joins such a path needs — an implicit inner join would drop every order without a
customer as soon as the list is sorted by one. `OrderEntityRest.postProcessMagicFilter` maps the two
DTO-only paths onto columns (`kunde.displayName` → `kunde.name`, `projekt.displayName` →
`projekt.name`), the way `Kost1PagesRest` does for `formattedNumber`.

**3b. Sort the loaded list in Kotlin for the computed columns. — done, ahead of paging.** 6 of the 19
order columns cannot be an `ORDER BY` at all — `nettoSumme`, `beauftragtNettoSumme`, `fakturiertSum`,
`zuFakturierenSum` and `personDays` are `@get:Transient` on `AuftragDO` and come from `OrderInfo`, `pos`
is `"#" + count` — and those money columns are exactly what users sort by. Sorting by one of them did
**nothing**: `addOrder` swallowed the exception, logged `Can't add order`, and the query went out with no
`ORDER BY` at all.

Done without waiting for the id list, since the whole result set is loaded anyway (that is what stage 2
changes) — and done the way the **Wicket** list has always done it: `MyListPageSortableDataProvider` never
pushes these into the query either, it loads the complete list and sorts it with `MyBeanComparator`.

- `SortPropertyComparator` (`projectforge-business/.../persistence/api/`) — the comparator that was inline
  in `DBFullTextResultIterator.sort`, extracted, with a `valueOf` hook for values reflection cannot reach
  cheaply. Keeps the criteria search's semantics (blank ranks lowest and therefore flips with the
  direction, strings through a locale `Collator`), so a list sorted in Kotlin and one sorted by the
  database read the same. Covered by `SortPropertyComparatorTest`.
- The mechanism is generic on `AbstractEntityRest` (it was hand-rolled per entity at first; lifted into
  the base once three finance lists shared the same trio of overrides). A page only **declares** its
  computed columns; the base owns the rest:
  - `computedSortProperties: Map<String, (O) -> Comparable<*>?>` — the sort ids the client sends (the
    column `id` in `*.page.tsx`) mapped to the value a loaded entity sorts by. Empty default = no
    computed column, so both generic paths below are no-ops and every other page is untouched.
  - `computedSortTieBreak: SortProperty` — the stable last criterion, since 0.00 is the most common of
    all sums and a customer has many orders. Default primary key desc; the invoice lists override it
    (`nummer`/`datum`).
  - `AbstractPagesRestUtils.buildQueryFilter` strips these from the `QueryFilter` once for every entity
    (else `addOrder` swallows them and ships an unordered query), never from `magicFilter.sortProperties`.
  - the generic `filterList` sorts the loaded list by them via `SortPropertyComparator`; `pos` sorts by
    the position count, so `#2` precedes `#10`.
- `OrderEntityRest` keeps its private `COMPUTED_SORT_PROPERTIES` map (each sort id → an `OrderInfo`
  accessor, so a comparison is an `AuftragsCache` lookup, not a query) and exposes it through
  `computedSortProperties`.

This runs on the materialized id list under paging (`sortIds(ids, filter)`, once per (user, filter)
rather than per page), not only on the loaded list — and by construction the two orderings are
byte-for-byte identical: same computed selection, same `computedSortTieBreak`, same comparator. Two
paths: `OrderEntityRest` opts into the cheap one (`hasComputedSortById = true` → `computedSortValueById`
reads the value straight from `AuftragsCache`, no entity load, for its thousands of rows); the invoice
lists take the default one (`sortIds` loads the matching entities and reuses `filterList`), since their
customer/project `displayName` is in no cache.

`assignedPersons` is left to reflection: it is a `@Transient` getter of `AuftragDO`, so it resolves
against the entity and only its own `addOrder` fails — but it works in fulltext mode, where the sort runs
in Kotlin. Making it work in criteria mode too means either letting `OrderInfo` carry the joined names
(fill it in `AuftragsCache.refresh`, where the users are resolved anyway) or sorting it in `filterList`
at the cost of a `UserGroupCache` lookup per user per comparison.

Rule for other lists: a computed column is sortable if derivable from a cache or the id alone — map it
like `COMPUTED_SORT_PROPERTIES` does; otherwise set `enableSorting: false` in the page-def rather than
silently doing nothing, which was the behaviour before this and worse.

---

## Stage 4 — one filter state (ships with Stage 2 for `order`) — open

Column filters must not regress, so paging and unification land together for the order list. Without
this, `manualFiltering` would let a column filter narrow only the loaded page while the footer still
claims 7132 rows — a silent correctness bug — and the "selection" checkbox mode would list the 50 loaded
values as if they were the whole column (`use-distinct-filter-values.ts`, and `column-filter.tsx` even
picks the default popover mode from that count).

**4a. An operator on the wire — and no new predicates.** `DBPredicate` already covers every operator the
column filters offer: `contains`/`startsWith`/`endsWith` → `Like` (which derives its `MatchType` from
the wildcards in `init`), `equals`/`notEqual` → `Equal`/`NotEqual`, `greaterThan`/`lessThan` and date
`before`/`after` → `Greater`/`Less`, `between` → `Between`, `blank` → `Or(IsNull, Equal(""))`, selection
→ `IsIn`. So:

- `MagicFilterEntry` gains `var operator: FilterOperator?` — **null means the type's default**, which is
  what every existing client sends and every stored favorite holds, so the null path must behave exactly
  as `createFieldSearchEntry` does today. `isModified` must compare it, or a favorite differing only in
  operator counts as unchanged.
- `MagicFilterProcessor.createFieldSearchEntry` gets an early branch to a new `predicateFor(...)`; the
  existing type-driven code stays as the null path. Additive; legacy clients bit-for-bit unaffected.
- Note in `predicateFor` that `Not`/`Equal`/`Greater` have `fullTextSupport = false`, so a search string
  plus an operator filter flips that filter into `resultPredicates` — correct, slower.

**4b. Layout contract.** `UIFilterElement` gains `operators: List<FilterOperator>?` and
`distinctValuesUrl: String?`, filled in `LayoutListFilterUtils.createNamedSearchFilterContainer` from
the property class. Additive JSON; the legacy `SearchFilter.jsx` reads named props and ignores the rest.
Mirror both on `FilterElement` in `lib/rs/types.ts`.

**4c. Distinct values.** `GET /rs/{entity}/distinctValues?field=<property>&max=200` returning
`{values: [{value, displayName}], truncated}` — `SELECT DISTINCT` with `setMaxResults(max+1)`, only for a
property that resolves to a column; transient columns answer `truncated=true` with an empty list, so the
client offers the comparison filter only, which is honest. Deliberately **not** narrowed by the active
filter in v1: facet counts would need the id list _and_ entity loads, i.e. the work paging just removed.
`useDistinctFilterValues` switches from `getFacetedUniqueValues()` to a `useQuery` on this endpoint;
`column-filter.tsx` picks its default mode from `truncated`.

**4d. Unify on the client — mostly a deletion.** `useListFilters.values` becomes the single source of
truth (value type widening to carry `operator`). The column-header popover writes through `applyValues`
instead of `column.setFilterValue`. TanStack's `columnFilters` becomes **derived, display-only** — a memo
so the funnel icon and `column.getIsFiltered()` still work while nothing filters client-side. Then pills
and favorites come for free: `filter-pills.tsx` already renders from `values` × `elements`
(`describeFilterValue` needs an operator-aware branch: "contains Migration", "≥ 10.000"), and
`useFilterFavorites` already saves `query.filter`, which is built from `filters.entries`. A column filter
stored in a favorite and restored on the next visit becomes automatic — and _should_, since the reason
`useTableState` refuses to restore `columnFilters` today is precisely that a hidden filter would silently
narrow a reopened list. Now it is a visible pill.

Column→field mapping: most page-def columns already name the entity property; add `filterField?: string`
to the column declaration (`lib/page-def/types.ts`) for the ones that don't (`kunde.displayName`). A
column whose `filterField` matches no `FilterElement` offers no filter — the correct fallback, and it
removes the "filter a computed column client-side" trap. Stop writing `ColumnState.columnFilters` from
`useColumnStatePersistence` (leave the slot for AG-Grid).

One real behavioural difference: `SelectionFilterValue` currently holds distinct _display texts_;
against the server it must hold stored values, which is what the `value`/`displayName` split in 4c is
for.

---

## Stage 5 — optimizations, behind measurement — open

- `SELECT id` projection in `selectIds` (scope to CRITERIA mode: `DBFullTextResultIterator.sort` sorts
  entities after the loop, so the fulltext path needs that sort reworked first).
- **Hybrid fast path:** when a query has no `customResultFilters`, no `resultPredicates`, no history
  search, is in CRITERIA mode, and the DAO can answer "row-level access is uniform for this user" (a new
  `open fun hasUniformSelectAccess(user): Boolean`, true for `AuftragDao` when the user is in
  CONTROLLING/FINANCE — see `AuftragRight.hasAccess`), push real `setFirstResult`/`setMaxResults` +
  `COUNT` and skip materialization. Makes the _first_ page fast for the users who see everything, which
  is most finance users. Purely additive: if the preconditions don't hold, Stage 2's path runs.
- Aggregate hook so `resultInfo` statistics work over the id list.
- Remove `AddressPagesRest`'s `limitResultSize` ("first page only, no offset, no total") when address
  gets `serverPaging`, or the two mechanisms fight and produce a 25-row `totalSize`.

## Stage 6 — roll out — open

`book`, `cost1`, `taskTree`, the `components/dynamic/` grids. Then retire client paging from
`use-data-table`.

---

## Verification

**Stage 1. — done.** `curl -s -o /dev/null -D - -H 'Accept-Encoding: gzip' -X POST .../rs/order/list`
shows `Content-Encoding: gzip`; payload 549 KB after the DTO trim (target was ~400 KB — the four
non-null booleans and the still-nested customer/project account for the difference, both deliberate).
Total 2.68 s, so "first row well under 1 s" is _not_ reached and the remaining cost is server-side. See
the verdict under Stage 1 for what that means for Stage 2's priority.

**Stage 2.** The invariant test above (pages concatenate to the unpaged result, with the four custom
filters active). Then in the browser: page 2/3 flip with no full reload, footer total = 7132, sorting a
DB column reorders across pages, a pill edit returns to page 1. Network tab: each page ~50 rows.

**Stage 3.** Sort by Nettosumme and by Kunde — both must actually reorder (Kunde silently did nothing
before 3a), orders without a customer must still be listed, and page boundaries must not repeat or drop
rows across pages.

**Stage 4.** Set a column filter → it appears as a pill, narrows the _whole_ result (footer total
drops), survives save-as-favorite and a reload. The selection list shows the column's real distinct
values, not just the loaded page's. An operator-bearing filter combined with a search string still
returns correct rows (the `resultPredicates` path).

E2E lives in `projectforge-next/e2e/` against the running system, deriving labels and formats from the
logged-in user via `e2e/fixtures/format.ts` — never hardcoded.

---

## Risks

1. **Stage 1 may be enough. — measured, and it is not.** The wire is down to 549 KB / ~0.4 s, but the
   request still takes 2.68 s, and that residue is the pipeline over all 7132 rows. So the risk
   materialized only in part: Stage 2 remains justified, and the re-measurement moved Stage 5's fast
   path forward as the cheaper first attempt at the server-side residue for users who see everything.
2. **The first request stays slow.** Paging makes every request _after_ the first cheap. A user who edits
   a pill on every interaction re-materializes the id list each time and gains only wire size — which
   Stage 1 already gave them.
3. **Session affinity.** `ExpiringSessionAttributes` is per-JVM, so multi-node needs sticky sessions.
   `MultiSelectionSupport` already requires this, so no new constraint — but it deepens the assumption.
4. **Stale id-list holes.** A row deleted between page loads gives 49 rows of 50 and a `totalSize` one
   too high. Cosmetic, never a leak. Don't try to fix it perfectly.
5. **`operator` in stored favorites.** A favorite written by a new backend and read by an old one
   degrades to the default operator rather than failing (`FAIL_ON_UNKNOWN_PROPERTIES` is false, set
   explicitly in `UserPrefDao`). Version the fingerprint so a semantics change invalidates caches.
6. **Scope.** Stages 2+4 together are a large single change touching the generic list stack every entity
   uses. The `serverPaging` flag keeps it opt-in per page, which is what makes that safe.
