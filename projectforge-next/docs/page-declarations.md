# List and edit pages: one declaration per entity

## The idea

A ProjectForge entity page is, almost always, the same page: a filterable table of rows, and a form
of cards behind each row. Writing that twice per entity — once as a table, once as a form — is how the
legacy frontends ended up with the list and the form disagreeing about what a field is called, whether
it is mandatory, and how long its value may be.

So a page here is **data, not code**. One module per entity exports a `PageDef` describing _what is
shown, in which order, how wide and under which label_. Two generic components render it:

| Component           | File                                             | Renders                                   |
| ------------------- | ------------------------------------------------ | ----------------------------------------- |
| `EntityListPage`    | `components/shared/list/entity-list-page.tsx`    | the whole list page                       |
| `EntityEditPage`    | `components/shared/edit/entity-edit-page.tsx`    | the whole edit form                       |
| `EntityHistoryPage` | `components/shared/edit/entity-history-page.tsx` | the change-history tab of the same entity |

A route file then contains nothing but the wiring:

```tsx
// app/(authenticated)/cost1/page.tsx
"use client";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { COST1_PAGE } from "@/components/features/cost1/cost1.page";

export default function Cost1ListPage() {
  return <EntityListPage page={COST1_PAGE} />;
}
```

## What a declaration does _not_ say

A declaration never states whether a field is mandatory, how long its value may be, or which values an
enum has. Those are the backend's rules and come from the generated metadata
(`lib/metadata/*.generated.ts`) — the same source the Zod schema reads. That is why `PageDef` has no
`required`, and must not get one: a second place to declare it is how the form and the entity drifted
apart before.

Derived from the metadata rather than declared:

| Derived                                           | From                                 | Rule in                                                                |
| ------------------------------------------------- | ------------------------------------ | ---------------------------------------------------------------------- |
| Column / field label                              | the field's `i18nKey`                | `labelKeyFor` (`lib/page-def/define-page.ts`)                          |
| Column filter kind                                | the field's `dataType`               | `filterKindFor`                                                        |
| Column alignment                                  | the field's `dataType`               | `alignFor` (numbers right, rest left)                                  |
| Default cell rendering                            | the field's `dataType`, `enumValues` | `declaredCell` (`components/shared/list/declared-cell.tsx`)            |
| Form component per field                          | the field's `dataType`, `enumValues` | `DeclaredFormField` (`components/shared/edit/declared-form-field.tsx`) |
| `required`, `maxLength`, enum options in the form | the metadata via form context        | the field components in `components/shared/form/`                      |
| Whether there is a history tab                    | `EntityMetadata.historizable`        | `entityTabs`                                                           |

`definePage()` itself is the identity function. Its only job is to bind the declaration to one
entity's metadata: every `name` is typed as `keyof M["fields"]`, so a field renamed in the entity
fails `tsc` instead of silently rendering an empty column.

## The escape hatch is first class

Not every page is ordinary, and the declaration says so at every level rather than forcing a page to
opt out of the whole mechanism:

- a column may bring its own `cell`,
- a field may be replaced by `{ custom: MyComponent }`,
- a section may render its whole body itself (`render`),
- the page may add a `statistics` banner, `listActions`, a `saveOption`, an `editBanner`,
  `extraTabs`, a `rowClassName` and a colour `legend`.

And a page that does not fit the shape at all is not made to fit: `useEntityListPage` and
`useEntityEditForm` stay public hooks, and the generic components are nothing but their normal case.
Two levels — declarative for the ordinary page, hooks for the exception.

## The contract

Everything below lives in `lib/page-def/types.ts`, which carries the authoritative prose per field.

```ts
interface PageDef<Row, Values, Data, M extends EntityMetadata> {
  entity: string; // REST category, e.g. "cost1" — what every call and the stored column state key on
  metadata: M; // the generated entity metadata
  route: string; // "/cost1"; the edit page is `${route}/${id}`
  queryKey: readonly unknown[]; // React Query key of the list
  categoryKey: string; // the menu parent above the title, e.g. "menu.fibu"
  titleKey: string;
  searchPlaceholderKey: string;
  columns: ColumnDeclaration<Row, M>[];
  legend?: LegendEntry[]; // extra colour legend entries below the table
  deletedLabelKey?: string; // overrides the always-present "deleted" legend entry
  rowClassName?: (row: Row) => string | undefined; // row-deleted | row-red | row-green | row-blue
  statistics?: (ctx) => ReactNode; // aggregates over the whole result set
  listActions?: ComponentType<{ filter: MagicFilter }>;
  edit: EditDef<Values, Data, M>;
}
```

### Columns

Three kinds, told apart structurally:

- **`FieldColumn`** — `{ name }`, one property of the entity. Label, filter, alignment and cell all
  come from its metadata.
- **`ComputedColumn`** — `{ id, labelKey, accessor }`, for a value the entity has no field for: a
  nested property (`kunde.displayName`), or a transient one the backend computes (an order's net sum).
  `id` **must** be the property the backend sorts by, because there is no field to take it from. Only
  a computed column may state a `dataType`, and only because there is no metadata to derive one from.
  For a value no SQL `ORDER BY` can express (a transient sum, `kunde.displayName`), the entity's REST
  class declares it in `AbstractEntityRest.computedSortProperties` under this same `id`; the base then
  drops it from the query and sorts the result in Kotlin, so the order the client picked drives both the
  list and every export. See `MIGRATION-list-paging.md` (Stage 3) for the mechanism.
- **`PeriodColumn`** — `{ periodLabelKey, begin, end }`, two date fields shown as the one value they
  are. A period is deliberately _not_ a data type of its own: the entity has two properties, the
  metadata reports two dates, and the backend sorts by `begin` (which is therefore the column's id).
  What is shared is only how it is _shown_. It offers no column filter — the two ends are one question
  the backend answers with overlap semantics, and a client-side filter over the rendered text would be
  a second, weaker one.

Shared options: `size` (in pixels; the fixed table layout ignores `minSize`, so this is what counts),
`minSize`, `className`, `align`, `filterKind` (`null` = offer none), `cell`, `pinned`, `labelKey`,
`headerLabelKey` (a shorter header where the full label does not fit).

**Pinning** is only a starting point. `defaultPinningOf(columns)` derives the initial pinning from the
declarations, in declaration order per edge — so the pinned edge and the column order are one
statement and cannot drift, and a "reset columns" returns to exactly that.

**Audit columns are appended, not declared.** Every entity carries `created` and `lastUpdate`
(`ExtendedBaseDO` / `BaseDTO`), so `auditColumnsFor` appends whichever of them the page has not
declared itself, hidden by default (`defaultVisibilityOf`). That makes "when was this made, when was
it last touched" answerable in _every_ list rather than in the ones that happened to declare it. A
page declaring one itself keeps its own declaration — including its visibility: an order's
`lastUpdate` is a column of the list, not an option of it.

### The edit half

```ts
interface EditDef<Values, Data, M> {
  schema: ZodType; // built from the metadata (lib/validation/from-metadata.ts)
  fieldNames: readonly string[]; // so a server error naming another field becomes a toast
  arrayFieldNames?: readonly string[];
  defaultValues: () => Values;
  toFormValues: (data: Data) => Values;
  title: (data: Data) => string; // heading of an existing entry
  newTitleKey: string; // heading while adding one
  savedMessageKey: string;
  sections: SectionDef<M>[];
  actions?: readonly string[]; // writes besides save, e.g. ["lendOut", "returnBook"]
  headerTrailing?: (data) => ReactNode;
  saveOption?: ComponentType; // a choice about the save itself, beside the save button
  editBanner?: ComponentType; // sticky bar under the tab strip
  extraTabs?: ExtraTabDef[]; // further pages of the entity, appended after the history
}
```

A **section** is one card of the form _and_ one anchor tab above it — `EditPageShell` couples them
positionally, which is why both come from this one array (`entityTabs`). A section either lists
`fields` (laid out in a three-column grid, stacked on a phone) or renders its whole `body` itself.

**Field declarations** come in four shapes:

- `DeclaredField` — `{ name }` plus presentation: `span` (1–3 grid columns), `startsRow`, `labelKey`,
  `hintKey`, `rows` (renders a textarea), `maxDigits`, `alignNumber`, `emphasized` (larger, in the
  accent colour — for the one value a reader looks for first), `readOnly`.
- `DatePeriodDeclaration` — `{ periodLabelKey, begin, end }`, the form counterpart of `PeriodColumn`.
  The two ends keep the labels of their own fields, so a screen reader names each box and a server
  error can land on one of them, while the legend above says the period once.
- `FieldGroupDeclaration` — `{ group: [...] }`, two or three narrow fields sharing **one** grid cell
  side by side (an order's number and offer date). Not one value: each member keeps its own label,
  error line and metadata. What the grouping says is that neither needs a third of the page, and that
  giving each its own cell would push the next field into the following row.
- `CustomField` — `{ custom: Component }`, for what the declaration cannot describe (a cost number,
  a keyword picker).

`startsRow` exists because the grid fills row by row: a field meant to begin a line the reader sees as
one unit (an order's three progress dates) would otherwise start in whatever column the previous field
left free.

## What the list renderer does

`EntityListPage` mounts in two phases. Both pieces of state that seed the table are stored
**server-side per user and entity** and must be there on the first render — TanStack's initial state
cannot be replaced afterwards without fighting the user's own edits:

1. `useStoredColumnState(entity)` — order, sizing, visibility, pinning, sorting, page size
   (`AbstractPagesRest.columnStates`).
2. `useRememberedFilter(entity)` — the filter the user last used, including the link to a saved
   filter favorite.

While either is pending it shows a spinner; a failed read simply starts from the defaults. Then
`DeclaredList` runs the derivations (append audit columns → `useDeclaredColumns` → `defaultPinningOf`)
and hands everything to `useEntityListPage`, which owns:

- the list query (`useMagicFilterQuery` → `POST /rs/{entity}/list` with a `MagicFilter`),
- sorting (server-side: `manualSorting`), search string, paging,
- the server-side pill filters (`useListFilters`; which fields exist is the **backend's** decision,
  derived per entity from the DAO's search fields and delivered by `listMeta`),
- the saved filters (`useFilterFavorites` — the same favorites the legacy list page offers),
- persistence of the column state and of the current filter,
- `resetColumns()` / `resetFilter()`.

Note the deliberate split: sorting and the search string go to Spring, while the header's column
filters and the paging work on the client, because `getList` returns the whole result set at once (up
to `maxRows`). `MIGRATION-list-paging.md` tracks moving paging to the server.

Two behaviours worth knowing because they are invisible in the declaration:

- **Row highlight on return.** The backend remembers per user which entry was edited last
  (`onAfterEdit`, and `onCancelEdit` too) and hands the id back with the list. `useHighlightedRow`
  marks that row and scrolls it into view, once per session per id.
- **The filter favorite travels with every list call.** The backend stores the filter it receives as
  the user's current one, so leaving the favorite's id and name out would drop the link to it — and
  with it the ability to save the edited filter back into that favorite.

## What the edit renderer does

`EntityEditPage` takes the declaration and an `id` (`null` = add):

1. `useEntityDetail(entity, id)` loads the entry — **including for a new one**: what a fresh entity
   looks like is the backend's decision (`newBaseDTO`), and an order in particular cannot be saved
   without the status preset there. A preset is not shared state, so it is never refetched.
2. `useEntityEditForm` sets up `@tanstack/react-form` with the Zod schema as its `onSubmit` validator
   and re-seeds the form whenever the loaded entity changes.
3. A submit runs `saveOrUpdate` — unless `meta.action` is one of the declared `actions`, in which case
   it posts to `/rs/{entity}/{action}` (see `lib/rs/submit-meta.ts`). Either way it is the _same_
   submit: same validation, same values, same 406 handling. Those endpoints save the whole posted
   entity, so anything else would let the page and the database drift apart.
4. HTTP 406 is a normal answer: `applyServerValidationErrors` places each error on its field, anything
   unplaceable becomes a toast.
5. A successful save shows a toast, resets the form and returns to the list (which is also where the
   backend's own `ResponseAction` points). Delete uses `markAsDeleted` — the delete a historized
   entity supports; the row survives and can be undeleted. Cancel calls `/rs/{entity}/cancel`, which
   writes nothing but runs the same `onAfterEdit`, and is awaited so the list is refetched with the id
   already remembered.
6. Every write invalidates the list, the entry and its history (`invalidateEntity`) rather than
   patching caches: the answer carries no entity, only an id, so the saved entry — including the fields
   the server computed — comes back on the next read.

The tab strip is `entityTabs(...)`: one anchor per section, then the history tab if
`metadata.historizable`, then the declared `extraTabs`. Pages beside the form follow one convention,
`${route}/${id}/${tab.id}`, so a declaration says a key and an id and nothing about routing. Seen from
such a page the section tabs become links back to the form with the section in the URL hash.

## Adding an entity

1. Make sure `lib/metadata/<entity>.generated.ts` exists — if not, run `DevelopmentMainForRelease`.
2. `components/features/<noun>/types.ts` — mirror the Spring DTO (optional props with `?`).
3. `<noun>-schema.ts` — the Zod schema via `fromMetadata(METADATA)`, plus the `FIELDS` name list.
4. `<noun>-values.ts` — `toFormValues` (normalising `undefined` away) and the empty values.
5. `<noun>.page.ts(x)` — the `definePage({...})` declaration.
6. Routes under `app/(authenticated)/<route>/`: `page.tsx` (list), `[id]/page.tsx` +
   `page-client.tsx`, `[id]/history/page.tsx` + `page-client.tsx`. The `page.tsx` files exist only to
   provide `generateStaticParams()` for the static export; the client reads the real id from the URL.
7. Register the category in `lib/hand-built-categories.ts` **and** in `NextMigration.MIGRATED`
   (projectforge-business) — a category is either hand-built or server-laid-out, never both.
8. New texts go into `I18nResources.properties` / `I18nResources_de.properties`, then regenerate.

The `/migrate-page` skill documents the full workflow including where to find the Wicket/React source
pages and DTOs.
