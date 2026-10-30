# Server-side paging of entity lists

By default a list page receives its whole result set (`POST /rs/<entity>/list`, up to `maxRows`) and
pages, sorts by column and filters by column in the browser. That is fine for a few hundred rows. For the
big finance lists it was not: the order list shipped several thousand rows / more than 10 MB per request and took seconds, nearly
all of it the payload and the per-row pipeline on the server. Those lists are served one page at a time.

Opted in today (`serverPaging: true` on the `PageDef`): `order`, `invoice`, `creditor-invoice`,
`timesheet`, `accountingRecord`. Every other list pages in the browser.

Two cheaper measures apply to every list: gzip (`server.compression.*` in `application.properties`, 8×
on JSON) and lean list rows (`BaseDTO.copyFrom4ListRow`, which an entity overrides to fill only the
columns its page renders; `NON_NULL` keeps the rest off the wire).

## Why not SQL `OFFSET/LIMIT`

Rows are dropped **after** the database returns them, so an offset query would give short pages with
silently skipped rows and a `COUNT(*)` larger than what the user may see:

1. **Access check per row** (`BaseDao.hasSelectAccess`), e.g. `AuftragRight.hasAccess`: group
   membership, project manager group, fully invoiced, a 1800-day rule — not expressible in SQL.
2. **`resultPredicates`** — predicates without criteria or full-text support run in Kotlin
   (`DBQueryBuilder`); in full-text mode that is every criteria-only predicate.
3. **`customResultFilters`** — e.g. the order list's position filters (`positionsArt`,
   `positionsStatus`, `positionsPaymentType`, `fakturiert`).

## How it works

- **One id list per (session, filter).** `getListPage` (`EntityRestUtils.kt`) runs the unchanged
  pipeline once — query, access checks, result predicates, custom filters — and keeps only the ordered
  ids. They are cached in the session (`ListPageCache`, via `ExpiringSessionAttributes`), keyed by
  `MagicFilter.resultFingerprint`.
- **Each page is a slice**, loaded with `AbstractEntityRest.getListByIds` → `BaseDao.select(ids)` (with
  the DAO's list entity graph, chunked `IN` clauses). So DTO mapping, formatting and Jackson run on 50
  rows instead of thousands.
- **Self-healing cache.** `ListPageChangeCounter` counts inserts/updates/deletes per entity; a cached list
  built before a change is rebuilt. It bumps before the commit and once more when the transaction ends, so a
  list built in between (old database and index state) isn't taken for current. A change past the DAOs (a
  restored dump, SQL) isn't counted: the first request of a mounted list therefore sends `refresh`, so a reload
  of the page always rebuilds. `refresh` also forces a rebuild right after the client's own write. The
  cache is never a correctness authority: every served row is access-checked again, so a stale list can
  at worst give a short page, never a forbidden row.
- **Sorting** a computed column happens on the id list (`AbstractEntityRest.sortIds`): from a cache value
  per id where the entity offers one (`hasComputedSortById`, the order book), otherwise by loading the
  entities and reusing `filterList` (the invoice lists). The result is the same order as `POST list`.
- **Statistics** over the whole result (sums in the footer) come from `AbstractEntityRest.aggregate`,
  computed once per id list and kept beside it (`CachedIdList.statistics`).
- **Endpoint:** `POST /rs/<entity>/listPage` (`ListPageRequest`: filter, `offset`, `limit`, `refresh`,
  `doNotStore`). `POST list` stays as it is — the multi-selection start and the Excel exports need every
  row.
- **Frontend:** `use-entity-list-page.ts` / `use-magic-filter-query.ts` switch to `listPage` when
  `serverPaging` is on.

## Turning it on for a page

Only when nothing filters or aggregates after the pipeline in the frontend or in `postProcessResultSet` —
those would see one page, not the result. Move such work into a `CustomResultFilter` and `aggregate`
first. Do not enable it for a page whose `getListByIds` has id semantics of its own (e.g. synthetic
negative ids). Add the invariant test like `OrderListPageTest` / `OutgoingInvoiceListPageTest` /
`IncomingInvoiceListPageTest`: all pages concatenated equal `POST list`, same total, and a row deleted
mid-paging gives a short page rather than a stale or forbidden one.

## Known limits

- **A column-header funnel** still filters in the browser: while one is set, the page falls back to
  fetching the whole result (`POST list`). Moving column filters to the server is open, see
  [open-points.md](open-points.md).
- **The first request for a new filter** still pays the whole pipeline; only later pages are cheap.
- **Sticky sessions** are required on a multi-node setup (the cache is per JVM, like
  `MultiSelectionSupport`).
- A row deleted between two page loads leaves a page one row short and a total one too high. Cosmetic,
  never a leak.
