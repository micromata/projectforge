# The three implemented entities

`book`, `cost1` and `order` are the pages migrated so far. They were picked in that order on purpose:
each one is a step up in difficulty, and together they define how far the declarative page concept
reaches (see [page-declarations.md](page-declarations.md)) and where the escape hatches are needed.

| Entity  | Backend                              | Migrated from        | Proves                                                           |
| ------- | ------------------------------------ | -------------------- | ---------------------------------------------------------------- |
| `book`  | `BookEntityRest`, `BookServicesRest` | React (page removed) | attachments, entity-specific writes, custom fields               |
| `cost1` | `Kost1PagesRest`                     | Wicket               | the minimum: what a page looks like with nothing special         |
| `order` | `OrderEntityRest`                    | Wicket               | the hard case: nested collections, server-computed sums, exports |

All three are registered in `NextMigration.MIGRATED` (backend) and in
`lib/hand-built-categories.ts` (frontend).

---

## `cost1` — the reference for the ordinary page

Files: `components/features/cost1/` — `cost1.page.ts`, `cost1-schema.ts`, `cost1-values.ts`,
`types.ts`, `cost-number-field.tsx`, `cost-number-segments.ts`.
Routes: `/cost1`, `/cost1/new`, `/cost1/[id]`, `/cost1/[id]/history`.

The whole page is 78 lines of declaration. Three columns (the three of
`Kost1PagesRest.createListLayout`, in its order) plus the two audit timestamps, and one section with
three entries. Every label, the status texts and every rule come from `Kost1DO` through the generated
metadata.

What it shows about the concept:

- **A page can consist of nothing but a declaration.** No custom cells, no custom sections, no
  actions, no statistics.
- **One custom field where the declaration genuinely cannot describe it.** The cost number is
  `6.100.01.02` — four `Int` properties of the entity that read as one number, so `CostNumberField`
  renders them as four boxes with separators (`SegmentedNumberField`). The list, by contrast, shows the
  entity's computed `formattedNumber` and filters it as text, because that is what a reader sees; the
  backend maps sorting on that property onto the four real columns
  (`Kost1PagesRest.postProcessMagicFilter`).
- **The one rule the metadata cannot carry is declared once, next to the field.** The range of each
  part (`min`/`max`) is not in the metadata — `@Column(length = 3)` is a digit count, not a `max = 999`
  — so `cost-number-segments.ts` holds it and both the input boxes and the Zod schema read it from
  there. The authority remains the backend's own check (`Kost1Dao.verifyKost`, HTTP 406).
- **`formattedNumber` is deliberately absent from the schema.** `Kost1DO` computes it and its getter
  has no backing field, so a value sent back would be dropped (`Kost1.copyTo`).
- **The history tab appears without being declared.** `KOST1_METADATA.historizable` is true —
  `Kost1DO`'s own `@WithHistory` is commented out, but `DefaultBaseDO` brings one.
- **A new entry's zeros are not a proposal.** `Kost1DO`'s four parts are Kotlin `Int` and arrive as
  `0` from `cost1/edit`; `toFormValues` leaves the boxes empty for an unsaved entry rather than
  offering the number `0.000.00.00`. A saved entry keeps its zeros — `0` is a valid part.

---

## `book` — attachments, own writes, and the way out of the legacy page

Files: `components/features/book/` — `book.page.tsx`, `types.ts`, `edit/book-edit-schema.ts`,
`edit/book-edit-values.ts`, `edit/sections/*`, `lend-out-column.tsx`, `loan-status.tsx`,
`status-badge.tsx`.
Routes: `/book`, `/book/new`, `/book/[id]`, `/book/[id]/history`.

The columns are `BookEntityRest.createListLayout`'s, in its order. Four sections: general, loan, notes,
attachments. Every label, every rule and the constants of both enums come from `BookDO`.

What it adds beyond `cost1`:

- **Attachments, generically.** `attachmentsColumn<Row>()`
  (`components/shared/attachments/attachments-column.tsx`) is declarable by any entity that has them.
  It is a _computed_ column: `attachmentsSize` is the property the backend sorts by, while the cell
  shows the string the backend formatted ("5,2MB (3)") — sorting on that string would put 900KB after
  1,1MB. It offers no filter of its own, because "has attachments" is a filter the backend already
  offers on the entity (`AttachmentsFilterSupport`). The edit page's attachment section is the one
  section that needs a persisted id to hang off, and says so itself.
- **Writes besides save.** `actions: ["lendOut", "returnBook"]`. `BookServicesRest` changes a few
  fields server-side and then runs the posted book through the very same `saveOrUpdate` — so these run
  through the _form's_ submit (`lib/rs/submit-meta.ts`): same Zod validation, same values, same 406
  handling. Declaring the names is all the renderer needs to route `meta.action` to
  `/rs/book/{action}`. What _triggers_ them stays the book's business: `BookLoanActions` inside the
  loan section's `render`.
- **`headerTrailing`** puts the loan badge beside the page heading, on the form and on the history page
  alike — hence it takes the entity, not the form values.
- **Presentation-only column overrides.** `yearOfPublishing` is a string in `BookDO` but gets
  `filterKind: "number"`, because what a reader wants is "greater than 2015", not "contains 2015"; its
  header uses a short label, because the full one is four times the column's width.
- **Sections render for a new book too.** The legacy page hid the loan block only because lending out
  needs a saved entity, which `BookLoanActions` checks itself.
- **The way back is gone.** The React page it was migrated from has been removed, so
  `NextMigration` records `legacyApp = null` and no `LegacyPageLink` appears.

Also of note: the loan section shows `lendOutBy` and `lendOutDate` rather than editing them — both are
set by the server from the session and the current date, so an input could only offer a user the
backend cannot resolve. Only `lendOutComment` is the user's to write. There is no loan history:
`BookDO` stores the current loan only, and past ones surface as `lendOutBy` changes in the change
history tab.

---

## `order` (Auftragsbuch) — the hard case

Files: `components/features/order/` — `order.page.tsx`, `order-schema.ts`, `order-values.ts`,
`types.ts`, `use-order-sums.ts`, `order-statistics.ts`, `order-statistics-line.tsx`,
`order-list-actions.tsx`, `forecast-export-dialog.tsx`, `edit/*`, `forecast/order-forecast-page.tsx`.
Routes: `/order`, `/order/new`, `/order/[id]`, `/order/[id]/history`, `/order/[id]/forecast`.

This is the page the server-laid-out `UILayout` renderer cannot express, and the reason the
declaration has an escape hatch at every level. Everything _around_ those exceptions is ordinary — the
fields, their labels, their rules and the history tab come from `AuftragDO` through the generated
metadata, exactly as for a book or a cost unit.

### The list

The 19 columns of `OrderEntityRest.createListLayout` plus `lastUpdate`, in an order of their own.

- **Five pinned columns.** Number, entry date, customer, project and title stay in view while the sums,
  the period and the rest are scrolled sideways. The pinning is a starting point the user owns; a reset
  returns to it (`defaultPinningOf`).
- **Six computed columns.** The customer and the project are `KundeDO`/`ProjektDO`, for which there is
  no `UIDataType`, so the metadata cannot carry them however the entity is annotated. The position
  count, the assigned persons, the person days and the four sums are transient properties computed by
  `OrderInfo` (`@get:Transient`). Each names as its `id` exactly the property the backend sorts by —
  including the cases the backend sorts in memory (`kunde.displayName`, `pos`). Those in-memory cases
  are declared once in `OrderEntityRest.computedSortProperties` (mapping each `id` to an `OrderInfo`
  accessor); the generic base in `AbstractEntityRest` strips them from the query and sorts by them, for
  the paged id list as for the whole `POST list` and every export alike. `OrderEntityRest` sets
  `hasComputedSortById = true` so its thousands of rows sort from `AuftragsCache` by id, without loading
  an entity per comparison.
- **Amounts render the numeric fields, not the `formatted*` strings** the legacy list uses: a string
  column sorts "900,00" after "1.100,00". A computed column may state `dataType: "AMOUNT"`, which is
  what makes a transient sum read as money in the user's currency.
- **One period column** for the period of performance — both ends as the one value the form asks for
  and the backend filters with overlap semantics.
- **Row colours mirror `AuftragListPage`'s `CellItemListener`,** first match wins: deleted / ABGELEHNT
  / ERSETZT → `row-deleted`; something to be invoiced → `row-red`; BEAUFTRAGT or LOI → `row-green`;
  ESKALATION → `row-red`. The legend below the table names them, with the always-present "deleted"
  entry relabelled, because this page colours rejected and replaced orders the same way.
- **`statistics`** renders `OrderEntityRest.OrderStatistics` — the sums over the whole result set —
  between toolbar and table, where the Wicket list shows them. `PageDef.statistics` passes the value as
  `unknown` on purpose: its shape belongs to the entity's REST class, and the declaration is the one
  place that knows which, so it narrows there and nothing generic carries a type it cannot check. The
  "leave out a line whose counter is 0" rule lives in `order-statistics.ts`, DOM-free and unit-tested.
- **`listActions`** are the two exports of the Wicket list's content menu: the list as Excel, and the
  forecast (which asks for its start month in a dialog first). Both are handed the `MagicFilter` the
  list call sends, so an export acts on exactly the rows the table shows. A filter matching nothing is
  answered 404 and reported as "no records", not as an error.

### The form

- **Two nested collections of unbounded length.** `PositionsSection` and `PaymentScheduleSection`
  render their bodies themselves, on the shared mechanics (`useFieldArray`, `RepeatableList`,
  `NestedFieldMetadata` — which scopes field metadata so `positionen[2].titel` is validated and
  labelled against the _position's_ metadata rather than silently falling back to "optional string").
  Deleted rows stay in the values with `deleted: true`: the backend's `CollectionHandler` physically
  removes — history and all — whatever a posted collection leaves out, and `number` travels back
  untouched because `AuftragsPositionDO.equals` matches on it.
- **Sums the server computes from what is currently in the form.** `useOrderSums` posts a
  deliberately narrow slice of the values (`sumsInput`) to a recalculation endpoint, debounced and
  keyed by that slice — so typing in the title does not ask the server about numbers it cannot move.
- **A pair of fields that fill each other in.** `CustomerProjectFields`: picking a project fills in
  what the project knows (its customer and three managers), but only where the field is still _empty_ —
  an order may deliberately name a different customer than its project does
  (`fibu.auftrag.hint.kannVonProjektKundenAbweichen`) or a stand-in manager, and overwriting that would
  quietly undo the user's choice.
- **A sticky `editBanner`** (number, status, forecast type, live sums) so the reader never has to scroll
  back to the head section while working through positions.
- **A `saveOption`** — "send an e-mail notification?". Not a field of a section: it says what the _save_
  does, so it belongs where the save is pressed.
- **A field group and `startsRow`.** The order number (read-only, assigned by
  `AuftragDao.getNextNumber` on the first save, but shown because it is how an order is referred to)
  shares one grid cell with the offer date. `startsRow` puts the three progress dates and the period on
  lines of their own, which the three-column grid would otherwise break.
- **An `extraTab`:** the forecast analysis at `/order/[id]/forecast`. A page of its own rather than a
  section, because the analysis is computed over the _saved_ order and a form may hold unsaved changes.

### Rules that stay on the server

The period-of-performance rules (`PeriodOfPerformanceValidator` — an order's begin becomes mandatory as
soon as a position inherits it, a position's end as soon as it has its own) and everything
`AuftragDao.onInsertOrModify` checks are not expressible in the metadata and are not restated here.
They come back as HTTP 406 and land on the field their `fieldId` names, nested paths included.
