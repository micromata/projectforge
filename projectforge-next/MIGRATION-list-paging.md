# Server-side paging for entity lists, and one unified filter state

Detail plan alongside [MIGRATION.md](MIGRATION.md), like
[MIGRATION-calendar.md](MIGRATION-calendar.md). Stage 1 is partly done (see below); stages 2–6 are
**not yet implemented** — this document holds the analysis so the work can start without exploring
again.

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

## Stage 1 — the cheap wins, then re-measure

No contract change, and it decides how urgent the rest is.

**1a. Enable gzip. — done.** `projectforge-business/src/main/resources/application.properties` now sets
`server.compression.enabled`, a JSON/HTML/CSS/JS mime list and `min-response-size=2048`. Verified:
12.5 MB → 1.5 MB. Check no reverse proxy in the deployment compresses already, or the work is done
twice.

**1b. Trim the order list row. — open.** Follow the existing precedent (`ListAddress` in
`AddressPagesRest.postProcessResultSet`, `Timesheet4ListExport`) with a **new list-only DTO** rather
than trimming `Auftrag` — the legacy AG-Grid columns bind to `formattedNettoSumme` & co. and would
break. New `projectforge-rest/src/main/kotlin/org/projectforge/rest/dto/OrderListRow.kt`; return it from
`AuftragPagesRest.postProcessResultSet` for next clients only (the
`RestAuthenticationUtils.isNextClient(request)` branch already exists in
`AbstractPagesRest.getInitialList`).

Measured fat, per row ~1280 bytes over 30 fields: `contactPerson` 180 B, `project` 164 B, `customer`
58 B are full nested DTOs where the column shows one name; and every sum travels twice, raw and
pre-formatted (`nettoSumme` + `formattedNettoSumme`, ×4). Send the name and the number; the client
already formats through `lib/format.ts`. Frontend: `OrderListRow` in
`components/features/order/types.ts` must stop extending `OrderDetail` — that inheritance is why it
carries the fat shape — and `order.page.tsx` reads `customerName`/`projectName` while keeping the column
`id` for the sort path.

**1c. Debounce the search box. — open.** `hooks/use-debounced-value.ts` exists and `list-toolbar.tsx`
doesn't use it. 300 ms before the value reaches `query.setGlobalFilter`.

**Then re-measure and re-argue Stage 2's priority.** Expected after 1a+1b: ~300 KB on the wire,
sub-second download, leaving the 3.5–5.8 s of server time. That is what Stage 2 addresses.

---

## Stage 2 — server-side paging

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
3. **`customResultFilters`** — the order list has four: `AuftragPagesRest.preProcessMagicFilter` marks
   `positionsArt`, `positionsStatus`, `positionsPaymentType`, `fakturiert` as `synthetic = true` and
   walks `positionenExcludingDeleted` per row.

A naive `OFFSET/LIMIT` would return short pages with silently skipped rows, and `COUNT(*)` a number
larger than the user may see. Both worse than slow-but-correct.

The id-list approach changes **nothing** in the pipeline — access checks, result predicates, custom
filters, `ensureUniqueSet`, fulltext Kotlin sorting all run once, exactly as today. It also makes
`totalSize` correct for the first time (today it is always `list.size`, see `getList` in
`AbstractPagesRestUtils`), and it makes transient-column sorting possible at all (Stage 3). 7132 `Long`
= 57 KB, so memory is a non-issue.

What it does **not** fix: the _first_ request for a cold filter still pays the pipeline. Stage 1 takes
the DTO mapping down from 7132 rows to 50, which is most of it; the rest is Stage 5.

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
currency formats), the three access checks per row in `AuftragPagesRest.transformFromDB`, and 11.9 MB
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

## Stage 3 — sorting under paging

**3a. Resolve nested sort paths. — done.** `MagicFilterProcessor` stripped everything before the first
dot, so `kunde.displayName` became `displayName`, which `AuftragDO` hasn't got →
`DBQueryBuilderByCriteria.addOrder` swallowed the exception, logged `Can't add order`, and **the order
list came back silently unordered**. It now keeps a nested path that resolves against the entity and
shortens only one that doesn't (the legacy DTO paths this was written for), and `DBCriteriaContext`
creates the `LEFT` joins such a path needs — an implicit inner join would drop every order without a
customer as soon as the list is sorted by one. `AuftragPagesRest.postProcessMagicFilter` maps the two
DTO-only paths onto columns (`kunde.displayName` → `kunde.name`, `projekt.displayName` →
`projekt.name`), the way `Kost1PagesRest` does for `formattedNumber`.

**3b. Sort the id list in Kotlin for the computed columns. — open.** This is where the id-list design
pays off — 8 of 19 order columns cannot be an `ORDER BY` at all (`personDays`, `nettoSumme`,
`beauftragtNettoSumme`, `fakturiertSum`, `zuFakturierenSum` are `@get:Transient` on `AuftragDO` and come
from `OrderInfo`; `pos` is `"#" + count`; `assignedPersons` is a transient getter over four users) — and
those money columns are exactly what users sort by.

```kotlin
/**
 * Reorders the materialized id list for sort properties no database column can express.
 * Called once per (user, filter), not per page. Implementations must not query: an order's net sum is
 * a map lookup in AuftragsCache, which is what makes sorting 7000 ids a matter of milliseconds.
 * @return the reordered ids, or null if the order the database produced stands.
 */
open fun sortIds(ids: LongArray, magicFilter: MagicFilter): LongArray? = null
```

Override in `AuftragPagesRest`, mapping each sort id to an `OrderInfo` accessor. Two consequences:
remove those sort properties from the `QueryFilter` in `postProcessMagicFilter` (else `addOrder` logs
per request and the DB order is arbitrary), and add a stable tie-breaker (`nummer` desc) so page
boundaries are deterministic. `assignedPersons` needs `OrderInfo` to carry the joined names — fill it in
`AuftragsCache.refresh`, where the users are resolved anyway.

Rule for other lists: a computed column is sortable if derivable from a cache or the id alone — declare
it in `sortIds`; otherwise set `enableSorting: false` in the page-def rather than silently doing
nothing, which was the behaviour before 3a and worse.

---

## Stage 4 — one filter state (ships with Stage 2 for `order`)

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

## Stage 5 — optimizations, behind measurement

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

## Stage 6 — roll out

`book`, `cost1`, `taskTree`, the `components/dynamic/` grids. Then retire client paging from
`use-data-table`.

---

## Verification

**Stage 1.** `curl -s -o /dev/null -D - -H 'Accept-Encoding: gzip' -X POST .../rs/order/list` shows
`Content-Encoding: gzip`; payload ≤ ~400 KB after the DTO trim. Re-run the timing probe: first row well
under 1 s. Then re-argue Stage 2's priority against the new numbers.

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

1. **Stage 1 may be enough.** 7.1 s of the measured cost was download, and gzip + the DTO diet plausibly
   remove ~95 % of it. The remaining server time is better and more cheaply addressed by Stage 5's fast
   path than by the full paging architecture, for the users who see everything. **Do not start Stage 2
   before re-measuring.**
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
