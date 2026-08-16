# Architecture of the `next` package

## What this package is

`projectforge-next` is a Next.js App Router application that replaces, page by page, the legacy
Wicket and React frontends of ProjectForge. All three run side by side inside the one Spring Boot
application:

| Frontend                      | Path     | Constant                            |
| ----------------------------- | -------- | ----------------------------------- |
| Wicket (legacy)               | `/wa`    | `Constants.WICKET_APPLICATION_PATH` |
| React (`projectforge-webapp`) | `/react` | `Constants.REACT_APP_PATH`          |
| This app                      | `/next`  | `Constants.NEXT_APP_PATH`           |

A page is migrated by moving it here and registering it on the backend; nothing else has to move with
it. Which frontend serves a page is decided in exactly one place, `NextMigration` in
projectforge-business (see [Routing and the migration switch](#routing-and-the-migration-switch)).

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
- Spring must answer a deep link such as `/next/book/25219084` with the HTML of _that_ route, not
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
    book/, cost1/, order/ the hand-built entity pages
    [category]/…          the provisional server-laid-out renderer's catch-alls
components/ui/            shadcn primitives — CLI-managed, never edited
components/data-table/    the generic table primitive: DataTable, filters, column state, paging
components/shared/        app-wide reusables: shells, form fields, attachments, history, tasks, chrome
components/features/<n>/  domain code per entity; no cross-feature imports
components/dynamic/       provisional UILayout renderer (see docs/README.md)
lib/                      pure TS: formatting, parsing, validation, page-def, metadata
lib/rs/                   the SOLE entry point for Spring backend calls
lib/metadata/*.generated  entity field metadata, generated from the backend entities
lib/page-def/             the page declaration contract and its derivation rules
hooks/                    cross-cutting React hooks
store/                    Zustand stores (client-only global state)
i18n/, messages/          next-intl config and locale catalogs
e2e/                      Playwright tests running against the real backend
```

The binding rules for placing code in these tiers are in `../CLAUDE.md`; this document only describes
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
- `entity.ts` — the writes of an entity page (`AbstractPagesRest`): `saveOrUpdateEntity`,
  `markEntityAsDeleted`, `undeleteEntity`, `cancelEntityEdit`, `postEntityAction`. These speak the
  UILayout protocol rather than plain JSON: the body is a `PostData` envelope (`{ data }`), the answer
  is a `ResponseAction` (the new id arrives only as `variables.id`, so a caller re-reads the entity),
  and **HTTP 406 is a regular answer** carrying `validationErrors`.
- `types.ts` — the wire contracts: `MagicFilter`, `MagicFilterEntry`, `ResultSet`, `ListMetaData`,
  `ResponseAction`, `ValidationError`, `UserStatus`, …
- Further modules per concern: `attachments.ts`, `auth.ts`, `column-state.ts`, `download.ts`,
  `filter-elements.ts`, `history.ts`, `jobs.ts`, `list-actions.ts`, `order.ts`, `submit-meta.ts`,
  `task.ts`, `upload.ts`.

Because Spring's mapper uses `JsonInclude.Include.NON_NULL`, an empty field is **absent** from the
JSON rather than null. Every DTO type in this app therefore marks optional properties with `?`, and
each feature's `toFormValues` normalises `undefined` away before the value reaches a controlled
input.

### `lib/metadata/` — the entity rules, generated

`*.generated.ts` is written by `GenerateNextFieldMetadataMain` (part of `DevelopmentMainForRelease`)
from the backend entities' `@PropertyInfo` and JPA `@Column`, merged by
`ElementsRegistry.getElementInfo` — the same source Wicket and the UILayout pages read. Per field:
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
  provisional UILayout renderer deliberately uses no form library (its field set only exists at
  runtime and validation is the server's).

### Forms and validation, in short

The server is the authority. The Zod schema only _anticipates_ the entity's rules for immediate
feedback; a rejected entity comes back as HTTP 406 with `validationErrors`, and
`lib/validation/server-errors.ts` places each one on the field its `fieldId` names — nested paths
(`positionen[0].periodOfPerformanceEnd`) included. An error naming a field the form does not render
becomes a toast instead of vanishing.

## Routing and the migration switch

`app/(authenticated)/` holds everything behind `AuthGuard`. Inside it:

- **Hand-built entities** own concrete routes: `/book`, `/book/new`, `/book/[id]`,
  `/book/[id]/history`, and the same shape for `cost1` and `order` (plus `/order/[id]/forecast`).
  Next resolves these before the generic catch-alls.
- **`[category]/…`** is the provisional renderer for pages that still get their layout from the
  server. `lib/hand-built-categories.ts` lists the hand-built categories so reaching one of them
  through the catch-all is treated as a wrong URL rather than as a fallback — it must stay in sync
  with `NextMigration.MIGRATED`.

On the backend, `NextMigration` (projectforge-business) is the single place that decides which
frontend a page belongs to. It has to be single, because a page's frontend URL is generated in two
unrelated places — the menu (`MenuItemDefId`) and every `ResponseAction(url = …)` redirect after
save/cancel/delete (via `PagesResolver`) — and if they disagree, the user is thrown from one frontend
into the other mid-workflow. One entry in `MIGRATED` switches a page over; removing it rolls the
switch back.

It also records the _way back_: a page may have been migrated straight from Wicket, which the React
migration never reached (`cost1`, `order`), so `legacyListUrl` / `legacyEditPage` are their own
mapping rather than derivable from the next URL. That is what feeds the `LegacyPageLink` escape hatch
beside every page title, and it answers `null` once the legacy page is gone (as for `book`).

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
- `npm run e2e` — Playwright against the **running** system. Credentials of a local test account are
  in `~/ProjectForge/testAccount.txt` (format `username/password` in one line); the suite logs in via `POST /rsPublic/nextLogin` and takes the
  `csrfToken` from `GET /rs/userStatus`. Seeing a real response settles at once whether a field is
  missing, null or merely displayed wrong.
