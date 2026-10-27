# Architecture of the `next` package

## What this package is

`projectforge-next` is a Next.js App Router application and the only frontend of ProjectForge. It
replaced, page by page, the Wicket and React frontends, which are removed. It is served by the Spring
Boot application under `/next` (`Constants.NEXT_APP_PATH`). Old `/wa/…` and `/react/…` URLs are answered
by `OrphanedLinkFilter`: links that left ProjectForge (mails, data transfer) land on their page here,
everything else on the start page.

Which pages exist and how the backend links to them is decided in one place, `PageRoutes` in
projectforge-business (see [Routing](#routing)).

## Two operating modes

The same source tree runs in two very different ways, and the difference shapes a lot of the code:

**Development** — `npm run dev` starts the Next dev server on `:3000`. `next.config.ts` proxies
`/rs/**` and `/rsPublic/**` to Spring on `:8080` via `rewrites()` with `basePath: false`, so backend
calls stay root-relative exactly as they are in production.

**Production** — `npm run build` produces a **static export** (`output: "export"`); there is no Node
process in production. Spring serves the emitted assets under `/next`. Consequences that permeate the
code:

- No server components that do real work at request time, no server actions, no route handlers, no
  `next-intl` plugin. Locale and message catalogs are resolved on the client
  (`i18n/locale-provider.tsx`).
- Every dynamic route is pre-rendered **once**, from the placeholder its `generateStaticParams()`
  returns (`/book/[id]` → `/book/new`). Therefore `useParams()` is unusable: it reports the baked-in
  placeholder for every URL. Pages read their parameters from `usePathname()` through
  `useRouteParams(pattern)` (`hooks/use-route-params.ts`, matching logic in `lib/route-params.ts`).
- Spring must answer a deep link such as `/next/book/4711` with the HTML of _that_ route, not
  with `404.html` or the list page. `scripts/generate-spa-shell-map.mjs` derives the mapping from
  Next's own build manifests into `out/next-spa-shell-map.json`, which
  `WebApplicationConfig.NextSpaResourceResolver` consumes. A new dynamic route reaches the server by
  rebuilding, not by editing Java.
- `output: "export"` is deliberately **off** in dev: the dev server refuses any dynamic param that
  `generateStaticParams()` does not list, so every deep link would answer 500 — precisely the URLs
  that need testing. `npm run build` still runs with the export on, so export-incompatible code is
  caught by the CI gate.
- `trailingSlash: true` (so Spring can serve `<route>/index.html`) with
  `skipTrailingSlashRedirect: true` (so a POST is not answered 308 and re-sent — for an upload, that
  means the file twice).

## Directory layout

```
app/                      routes only; pages compose features, no business logic
  layout.tsx              html shell: fonts, theme, LocaleProvider, QueryProvider, TooltipProvider, Toaster
  login/, password-*/     public routes
  (authenticated)/        everything behind AuthGuard + TwoFactorProvider + JobToasts
    <route>/              one directory per page (book/, order/, calendar/, …)
components/ui/            shadcn primitives — CLI-managed, never edited
components/data-table/    the generic table primitive: DataTable, filters, column state, paging
components/shared/        app-wide reusables: shells, form fields, attachments, history, tasks, chrome
components/features/<n>/  domain code per entity; no cross-feature imports
lib/                      pure TS: formatting, parsing, validation, page-def, metadata
lib/rs/                   the SOLE entry point for Spring backend calls
lib/metadata/*.generated  entity field metadata, generated from the backend entities
lib/page-def/             the page declaration contract and its derivation rules
hooks/                    cross-cutting React hooks
store/                    Zustand stores (client-only global state)
i18n/, messages/          next-intl config and locale catalogs
e2e/                      Playwright tests running against the real backend
```

The binding rules for placing code in these tiers are in `projectforge-next/CLAUDE.md`; this document only describes
what exists.

## Layers

### `lib/rs/` — the backend boundary

Every call to Spring goes through this folder; feature code never calls `fetch`.

- `client.ts` — the transport. Holds the session's CSRF token in a module variable (not
  `localStorage`, so an XSS cannot read it), sets `X-PF-CSRF-Token` on every state-changing call, and
  transparently retries once after a `403 csrfTokenRequired` by re-reading `userStatus`. A
  `403 twoFactorRequired` is routed to a handler the app shell registers
  (`components/shared/two-factor-provider.tsx`), so the interrupted call can simply be repeated after
  the second factor. Read helpers: `fetchList`, `fetchOne`, `fetchNew`, `fetchListMeta`,
  `fetchUserStatus`, `fetchMenu`, the filter-favorite calls, …
- `entity.ts` — the writes of an entity page (`AbstractEntityRest`): `saveOrUpdateEntity`,
  `markEntityAsDeleted`, `undeleteEntity`, `cancelEntityEdit`, `postEntityAction`. These speak the
  `PostData`/`ResponseAction` protocol rather than plain JSON: the body is a `PostData` envelope
  (`{ data }`), the answer is a `ResponseAction` (the new id arrives only as `variables.id`, so a caller
  re-reads the entity), and **HTTP 406 is a regular answer** carrying `validationErrors`.
- `types.ts` — the wire contracts: `MagicFilter`, `MagicFilterEntry`, `ResultSet`, `ListMetaData`,
  `ResponseAction`, `ValidationError`, `UserStatus`, …
- Further modules per concern (one file per area, e.g. `attachments.ts`, `history.ts`, `list-actions.ts`;
  browse the directory, it grows with every feature).

Because Spring's mapper uses `JsonInclude.Include.NON_NULL`, an empty field is **absent** from the
JSON rather than null. Every DTO type in this app therefore marks optional properties with `?`, and
each feature's `toFormValues` normalises `undefined` away before the value reaches a controlled
input.

### `lib/metadata/` — the entity rules, generated

`*.generated.ts` is written by `GenerateNextFieldMetadataMain` (part of `DevelopmentMainForRelease`)
from the backend entities' `@PropertyInfo` and JPA `@Column`, merged by
`ElementsRegistry.getElementInfo` — the same source the backend validation reads. Per field:
`dataType` (mirror of the backend's `UIDataType`), `i18nKey`, `required`, `maxLength`, `readOnly`,
`enumValues`. Per entity: the class name and `historizable`.

This is the single source of truth for what a field _allows_. `lib/validation/from-metadata.ts`
builds the Zod pieces from it (`requiredString`, `nullableString`, `intField`, `decimalField`,
`booleanField`, `entityField`, `enumField`), the field components read `required`, `maxLength` and the
enum constants from it via the form context, and the page declarations derive labels, alignment and
filter kinds from it. Nothing in the frontend restates a rule — a field renamed in the entity fails
`tsc`, and a changed column length changes the form by regenerating.

`lib/metadata/types.ts` is hand-written and declares only the contract.

### i18n

`I18nResources.properties` in projectforge-business is the source of truth.
`GenerateNextI18nMessagesMain` turns it into `messages/generated.<locale>.json`, which must never be
edited by hand (`GenerateNextI18nMessagesTest` fails the build if they drift).
`messages/de.json` / `messages/en.json` are hand-written and hold only texts with no backend
counterpart; `i18n/config.ts` deep-merges them over the generated catalogs, so both can share a
namespace. Dotted backend keys become nested namespaces, and a key that is both a leaf and a parent
(`fibu.kost1` is a text _and_ the parent of `fibu.kost1.title`) is exported as `<key>._` — which is
why `labelKeyFor` falls back to `${base}._`.

Both catalogs are bundled statically: the static export has no server to resolve them per request.

### Formatting

Dates, timestamps, numbers and currency are **the user's** — taken from `userData` (`locale`,
`timeZone`, `currency`), never from the runtime default. One helper, `lib/format.ts`, with the context
from `useFormatContext()` (`hooks/use-format.ts`, which reads the logged-in user). No
`toLocaleString()` at a call site, no ad-hoc `Intl.*`, no hand-built `dd.MM.yyyy`. Values the backend
already formatted (`sizeHumanReadable`, `attachmentsSizeFormatted`, …) are taken as they are; they
already come in the user's locale, and reformatting them would be a second place to be wrong.

E2E tests obey the same rule: `e2e/fixtures/format.ts` derives expected texts and dates from the
logged-in user, so an assertion does not silently only pass for a German account.

### Data and state

- **Server state: TanStack Query only.** No `useEffect` + `fetch`, no manual loading flags. Query
  keys include the full filter, so a refetch is automatic.
- **Local state first**, `useState`; lifted to **Zustand** (`store/`) only when it is shared across
  unrelated trees — currently `ui-store` (sidebar) and `job-store` (background job toasts, which must
  outlive the page that started the job).
- **Forms:** hand-built pages use `@tanstack/react-form` + a Zod schema built from the metadata. The
  mass update form deliberately uses no form library (its field set only exists at runtime and
  validation is the server's).

### Forms and validation, in short

The server is the authority. The Zod schema only _anticipates_ the entity's rules for immediate
feedback; a rejected entity comes back as HTTP 406 with `validationErrors`, and
`lib/validation/server-errors.ts` places each one on the field its `fieldId` names — nested paths
(`positionen[0].periodOfPerformanceEnd`) included. An error naming a field the form does not render
becomes a toast instead of vanishing.

### Pitfalls of the backend contract

Each of these cost a debugging session once.

- **Answers of a write.** An `AccessException` from one of the four CRUD endpoints is a **406**; an
  own endpoint (`postEntityAction`) answers a refusal as `kind: "rejected"` (a `TOAST`, while success
  carries `REDIRECT`); anything else that refuses answers **403** + `RestError` (`useReadAccessGuard`).
  An expired CSRF token is a `403 csrfTokenRequired`, retried once by `client.ts`.
- **A form posts its values as the DTO.** Schema and `toFormValues` must carry **every** field the
  page does not edit through unchanged (recurrence, attendees, reminders, `deleted`, …), or a save
  silently erases it.
- **Access flags on the DTO** (`writeAccess`/`deleteAccess`, `EntityAccessSupport`, filled in
  `AbstractEntityRest.getById`): a missing flag means allowed. Buttons without the right are left out,
  field-level rights render the field read-only with the backend's refusal as hint.
- **`MagicFilter`** carries only fields the Kotlin class can deserialize (HTTP 400 otherwise); the page
  size is a filter entry (`paginationPageSizeEntry()`), not a top-level field. Range bounds are
  `from`/`to` on the wire. A STRING filter is a `LIKE` over the whole field, so the client sets the
  wildcards.
- **Filter favorites** (`filter/select|create|update|rename|delete`): `create` without `id`; `update`
  replaces the whole favorite (send the name along) and answers an empty map; only `select` answers
  full `InitialListData`. The current filter reference (`id` + `name`) goes with every list call,
  because `getList` stores it as the remembered filter. The `["listMeta", entity]` cache is patched,
  not mirrored in own state.
- **Remembered list state:** `getList` stores the filter, `listMeta.filter` returns it; the list stays
  behind a spinner until `listMeta` **and** `columnStates` are there. Column filters are deliberately
  not restored (a hidden filter would silently narrow a reopened list).
- **Attachments:** every write answers the entity's **whole** new list (`setQueryData`, no re-read); a
  refusal is HTTP 200 with a `TOAST`.
- **Multi-selection / mass update** keeps its state in the HTTP session (keyed by the `*EntityRest`
  class, 60 min), like the paged id lists — multi-node needs sticky sessions.
- **`FormData` bodies:** don't set `Content-Type`, the browser adds the boundary.
- **Calendar** (`CalendarServicesRest`, `CalendarFilterServicesRest`): the changing endpoints answer
  _subsets_ of `CalendarInit` (`selectFilter` the whole of it, `changeStyle` no `isFilterModified`);
  event `start`/`end` are passed to FullCalendar unchanged (ISO instant or `yyyy-MM-dd`), or all-day
  events shift by the zone offset; `vacationGroupIds`/`vacationUserIds` are overwritten by the server
  and belong in the query key, not the body; a range over 50 days is a 400.

## Routing

`app/(authenticated)/` holds everything behind `AuthGuard`. **Every page is hand-built** and owns
concrete routes (`/book`, `/book/new`, `/book/[id]`, …). There is no catch-all: a URL no route serves
is a 404. The history of an entry is a tab of its edit page (`?tab=history`), not a route.

On the backend, `PageRoutes` (projectforge-business) is the single place that knows the pages: a
page's URL is generated in two unrelated places — the menu (`MenuItemDefId`) and every
`ResponseAction(url = …)` redirect after save/cancel/delete (via `PagesResolver`) — and both ask it.
It also records which old `/wa` or `/react` URLs are redirected (`legacyApp`).

### Adding a page

- **An entry in `PageRoutes.PAGES`** (REST category → next route) and its directory under
  `app/(authenticated)/`; `PageRoutesTest` checks that every route exists. A second perspective of
  an entity under another route names its URL directly in `MenuItemDefId` (task tree `next/taskTree`
  beside the list `next/task`).
- **The second factor is checked on the REST call**, not on the page URL (a static file passes no
  filter): register the `*Rest` class in `ProjectForge2FAInitialization` for reading
  (`registerShortCutClasses`) and `WRITE:<category>` in the matching `*_WRITE` shortcut for writing. The
  category is the REST path, not the DAO identifier (`order`, not `auftrag`). Both are needed, because
  each shortcut can be configured on its own. `PageRoutes2FATest` enforces it.
- **Access is checked by the endpoint** (`@AccessChecked`, see the security rules in the root
  `CLAUDE.md`); `userAccess` and the DTO's access flags only decide what the page shows.
- **Old links are redirected only if they left ProjectForge** — in a mail (notifications, reminders) or
  a data transfer. Bookmarks alone don't justify a redirect (`PageRoutesTest`, "only links that left
  ProjectForge are redirected").

## Page composition

Three shells, used by every page:

- `PageShell` — brand stripe, top navigation, scrollable main area.
- `ListPageShell` — toolbar, an optional banner for result-set aggregates, the table.
- `EditPageShell` — header, tab strip, optional sticky banner, the scrollable sections column,
  the bottom action bar. Anchor tabs are coupled positionally to the sections and driven by
  `useScrollSpy`; a URL hash opens the section it names.

What goes into them for an entity is not written per page but declared once — see
[page-declarations.md](page-declarations.md).

## Testing and quality gates

- `npm run typecheck` — zero errors.
- `npm run lint -- --fix` — zero remaining errors.
- `npm run format` — clean.
- `npm run test` — Vitest, for the pure logic (`lib/page-def/*`, `lib/format`, `lib/validation`,
  filter and column-order derivations). Deliberately DOM-free: what is worth asserting is the
  derivation, not the JSX around it.
- `npm run e2e` — Playwright against the **running** system, see "Testing against the running
  system" in `projectforge-next/CLAUDE.md` for the test accounts (written by `E2ETestAccountsService` in development
  mode), the targets (`npm run e2e` → the static export on :8080, `npm run e2e:dev` → the dev server),
  the slots of `bin/pfDev.sh` and the spec tags (`@parallel`, `@lane-*`, `@isolated`). A spec about a
  rights rule asks for the role that makes the refusal reachable and skips via `hasRole` where the
  instance has no such account.
- Against the dev server a run competes with compiling: the first visit of a route compiles it, and an
  edit made mid-run invalidates modules under the test's feet. Restart `next dev` before a long run and
  don't edit while it runs; for a full run use the static export.
