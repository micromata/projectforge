# Removing Wicket (`projectforge-wicket`, `/wa`)

> Plan for removing the Wicket frontend entirely. It complements Phase 4 of
> [projectforge-next/MIGRATION.md](../projectforge-next/MIGRATION.md), which only states the goal
> ("remove `projectforge-wicket` from `settings.gradle.kts` + build, remove `/wa` serving/filters").
> Status as of 2026-10-03: **no core menu entry opens Wicket any more**. The last two were the HR view
> (`next/hrList`) and the deprecated favorites (`wa/userPrefList`, menu entry removed).

## Goal and non-goals

- **Goal:** delete the module `projectforge-wicket`, the Wicket libraries, the Wicket filters and the
  `/wa/*` serving; plugins no longer depend on Wicket.
- **Kept:** `OrphanedLinkFilter` (old bookmarks and links in sent mails must keep working),
  `WicketSupport` in `projectforge-business` (a plain Spring bean locator used in ~40 files – renaming is
  optional), `Constants.WICKET_APPLICATION_PATH` as long as the redirects need it.
- **Not part of this plan:** removing the old React app (`projectforge-webapp`). It is still needed
  (address, user, vacation, employee, … point to `react/`); only its `/wa` bits go.

## Current state (inventory)

### Remaining Wicket pages and how they are reached

44 concrete page classes are left (`projectforge-wicket`).
"Reached" means a real link or registration – mentions in comments are not counted. Sources:
`MenuItemRegistry`, `WebRegistry.init()`, `NextMigration.MIGRATED`, `wa/…` links in next/REST code.

`MenuItemRegistry` is gone: every menu entry resolves to `next/…`, so `WicketMenuBuilder` only uses the url.

| Group | Pages | Reached from |
|---|---|---|
| Escape hatch ("classic version") behind a migrated next page | access (`AccessListPage`/`AccessEditPage`), account (`KontoListPage`/`KontoEditPage`), accountingRecord (`AccountingRecordListPage`/`AccountingRecordEditPage`), cost1 (`Kost1*Page`), cost2 (`Kost2*Page`), cost2Type (`Kost2Art*Page`), gantt (`GanttChart*Page`), project (`Projekt*Page`), task (`TaskListPage`/`TaskEditPage`) | `NextMigration` entries with `legacyApp = WICKET` and `offerLegacyLink = true` |
| Escape hatch, hard-coded in next | `TaskTreePage` (task favorites not yet in next), `AdminPage`, `PhoneCallPage` | `legacyUrl` in `taskTree/page.tsx`, `system-page.tsx` (`wa/admin?legacyEscape`), `phone-call-page.tsx` |
| Only reached from other legacy pages | `TaskWizardPage` (from `TaskTreePage`, `AccessListPage`), `TimesheetListPage`/`TimesheetEditPage` (from task tree/edit, old calendar), `GroupListPage`/`GroupEditPage` (group select panels, `TaskWizardForm`), `UserPrefListPage`/`UserPrefEditPage` (favorites in list pages and `TimesheetEditPage`), `EingangsrechnungListPage`/`EingangsrechnungEditPage` (`offerLegacyLink = false`, mount only), `CalendarPage` (`wa/oldCalendar`), `TeamCalCalendarPage` (`wa/oldTeamCalendar`) and below it `TeamCalListPage`, `TeamCalEditPage`, `TeamEventListPage`, `TeamEventEditPage`, `TeamCalImportPage` | Wicket pages among themselves; "classics" links of `react/teamCal` (`TeamCalPagesRest`) and `react/group` (`GroupPagesRest.kt:153` still answers `wa/groupList`, although `NextMigration` sends the group's way back to React) |
| Mounted, but no way back from next | `DatevImportPage` (`wa/datevImport`, menu points to `next/datev-import`, only `CallAllPagesTest` uses it) | URL typed by hand only |
| Infrastructure | `ErrorPage`, `PageExpiredPage`, `MessagePage`, the other `Abstract*Page`s | Wicket itself |

Without the escape hatches and the hidden mounts, Wicket is still needed only for the task
wizard, the user favorites (`UserPref`) and the old calendar pages. An escape-hatch category can only go
after its `offerLegacyLink` has been set to `false`.

### Plugins

| Plugin | Wicket code | Status |
|---|---|---|
| ihk | none (`IHKPage`, `IHKForm` deleted) | migrated to next (`/next/ihk`, `IHKRest`); no Wicket dependency |
| todo | none (`ToDoListPage/Form`, `ToDoEditPage/Form`, `ToDoPagesRest` deleted) | migrated to next (`/next/todo`, `ToDoEntityRest`); no Wicket dependency |
| liquidityplanning | none (pages, forms, `LiquidityChartBuilder`, `.html` deleted) | migrated to next (`/next/liquidity`); no Wicket dependency |
| licensemanagement | none (unused owner helpers of `LicenseDao` with `UsersProvider` deleted) | migrated; no Wicket dependency |
| marketing | none | migrated; no Wicket dependency |
| banking, datatransfer, memo, merlin, skillmatrix | none | React, no Wicket dependency |

## Phase 1 – Remove hidden dependencies (low risk, can start now)

These would break silently if the module were simply deleted.

- [x] **Main i18n bundle** – registered by `I18nServiceImpl.init()` (`I18nHelper.addBundleName`), the
      plugin bundles by `PluginAdminService.activatePlugin`; independent of `WicketApplication`.
- [x] **Filters used by `WebXMLInitializer`** – `ResponseHeaderFilter` moved to
      `projectforge-application` (`org.projectforge.config`, Kotlin); `SpringThreadLocalFilter` and
      `SpringContext` deleted (nothing read the context).
- [x] **`LicenseDao`** – its owner helpers (`setOwners`, `getSortedOwners`, `getSortedOwnernames`) had no
      callers left (the REST DTO maps `ownerIds` itself) and were deleted with the `UsersProvider` use;
      licensemanagement no longer depends on `projectforge-wicket`.
- [x] **marketing** – `api(project(":projectforge-wicket"))` and the `**/*.html` resource include removed.
- [x] **Plugin menu registration** – all plugins register via `MenuCreator`;
      `PluginWicketRegistrationService` deleted.
- [x] **`ProjectForgeEndpoints`** – implements only `SystemDiagnosticsExport` now (REST endpoints, no Wicket
      mount points); `AdminPage` gets the dump via `WicketSupport.get(SystemDiagnosticsExport.class)`,
      `IProjectForgeEndpoints` deleted.
- [x] **`MenuCustomizationController`** (`/rs/menucustomization`) – no caller anywhere, deleted.
- [x] **Dead code** – `PacmanViewPage` (+ `scripts/pacman`, LESS rule), `AbstractViewPage`,
      `AbstractSecuredPopupPage` and `MenuItemRegistry` deleted.
- [x] **`TeamEventDao`** – stack-trace check for `EditPageSupport` removed.
- [ ] **Hard-coded `wa/` links outside Wicket:**
  - ~~`task.page.tsx` "show access rights" (`wa/accessList?taskId=…`, lost the `taskId` in the redirect)~~
    – done: `lib/access-links.ts` → `next/access?taskId=…`, seeded as a transient task filter.
  - ~~`EingangsrechnungUploadPageRest`, `IncomingInvoicePosImportPageRest` `callerPage` `/wa/incomingInvoiceList`~~
    – done: `NextMigration.listUrl(DaoConst.INCOMING_INVOICE)`.
  - `GroupPagesRest.kt:153`, `TeamCalPagesRest.kt:106` – `classicsLinkListUrl` into Wicket; remove once
    the targets are gone (TeamCal: see Phase 2).

## Phase 2 – Migrate or deliberately drop the remaining functionality

Each item: build in next, or decide with the product owner that it goes away.

- [x] **IHK plugin** – migrated: next page `/next/ihk` (week picker, missing-description list with next
      timesheet links, setup instructions) on `IHKRest`/`IHKService`, reusing `IHKExporter`. Wicket pages deleted.
- [x] **ToDo plugin** – migrated: next pages `/next/todo` (list with "only recent" filter and highlighted
      recent rows, edit with close dialog, templates, notification option) on `ToDoEntityRest`; the
      notification mail is sent from the REST save path with a next edit link. The templates are new
      favorites (`ToDoFavoritesService`), the old `UserPrefArea.TODO_FAVORITE` entries are not migrated;
      "send short message" (commented out in Wicket) is dropped. Wicket pages deleted.
- [x] **Liquidity planning** – Wicket pages/forms, `LiquidityChartBuilder`, `registerWeb`/`addMountPage`,
      the `/wa/...` 2FA shortcut values and the `projectforge-wicket` dependency deleted; the menu entry is
      registered via `MenuCreator`. The extra sheets of the Wicket export (cash flow, forecast "all",
      debitor/creditor invoices) moved to the forecast tab's Excel export (`/rs/liquidity/forecast/excel`,
      `LiquidityForecastExcelExport`); only the "paranoia case" forecast series was dropped. The
      `NextMigration` entry keeps its `legacyRoute`s so old bookmarks still redirect.
- [ ] **TeamCal administration** (`TeamCalListPage`/`TeamCalEditPage`), **ICS import**
      (`TeamCalImportPage`, `TeamCalImportDao`), **team event list**, ICS download in
      `TeamEventEditPage`, the ICS subscription-link dialogs (`TimesheetsICSExportDialog`,
      `TeamCalICSExportDialog`) – check what React/next already covers.
- [ ] **Exports only in Wicket list pages** – Kost1/Kost2 Excel export, incoming invoice cost-assignment
      Excel / XML export (`EingangsrechnungListPage`), SEPA transfer export page, CSV/SEPA import wizard
      (see MIGRATION.md, Kreditorenrechnungen TODO).
- [ ] **`AdminPage`** tools and dumps not yet in `next/system`.
- [ ] **User preferences** (`UserPrefListPage`) – no replacement planned. Decide on the Wicket-only
      `UserPrefArea`s (`USER_FAVORITE`, `TIMESHEET_TEMPLATE`, `TASK_FAVORITE`, `KUNDE_FAVORITE`, …):
      drop or migrate their data.
- [ ] **`ExternalResourceLoader`** (customer i18n overrides, Wicket only) – check whether next needs an
      equivalent.

## Phase 3 – Close the escape hatches

- [ ] `NextMigration`: remove `offerLegacyLink`/`ESCAPE_HATCH_PARAM`; **keep** `legacyApp`/`legacyRoute`
      for the `OrphanedLinkFilter` redirects. Adjust `NextMigrationTest`, `PageResolverTest`.
- [ ] projectforge-next: `LegacyPageLink` usages, `hooks/use-legacy-edit-url.ts`, the `wa/` handling in
      `lib/menu-url.ts`, `lib/config.ts`, `task-edit-link.tsx`, `system-alert-banner.tsx`; the hard
      links in `taskTree/page.tsx`, `system-page.tsx`, `phone-call-page.tsx`. Tests: e2e
      `legacy-page-link.spec.ts`, `invoice-edit.spec.ts`, `task-edit.spec.ts`, `quick-access.spec.ts`;
      unit `menu-url.test.ts`, `menu-search.test.ts`.
- [ ] `MenuItemDefId.TASK_TREE`: drop the `wa/taskTree` fallback of `nextRouteUrl`.
- [ ] `OrphanedLinkFilter`: remove the escape-hatch pass-throughs (`?legacyEscape`, `/wa/admin`), add a
      **catch-all `/wa/*` → `/next/`** so every unmapped old link lands somewhere instead of a 404.
      Extend `OrphanedLinkFilterTest`.
- [ ] 2FA: remove the `/wa/...` shortcut values in `ProjectForge2FAInitialization`, `"/wa"` in
      `My2FARequestHandler.ALL`, the docs in `My2FARequestConfiguration`; adjust
      `My2FARequestHandlerTest`, `NextMigration2FATest`. Release note: customer `projectforge.properties`
      may still list `/wa` paths (harmless).

## Phase 4 – Delete the module

- [ ] `settings.gradle.kts`: remove `include(":projectforge-wicket")`.
- [ ] `projectforge-application/build.gradle.kts`: remove the module dependency and the Wicket libs
      (wicket myextensions, wicket.spring, wicketstuff html5/select2); check rhino (only the Wicket LESS
      compiler?) and jsp-api.
- [ ] `gradle/libs.versions.toml`: remove the wicket/wicketstuff versions and libraries.
- [ ] `WebXMLInitializer`: remove `WicketUserFilter`, the `WicketFilter`
      on `/wa/*`; keep locale filter, `restUserFilter`, `calendarSubscriptionFilter`, `OrphanedLinkFilter`.
- [ ] Delete `projectforge-business/.../user/filter/WicketUserFilter.kt` (its `/wa/setup` branch is dead
      already – setup lives in next).
- [ ] Static resources: the module ships `src/main/webapp` (images, styles, fonts, scripts) as `static`.
      Verify nothing outside Wicket loads them (favicon, mail templates, React) before deleting.
- [ ] Split packages: `org.projectforge.web`, `.web.session`, `.web.teamcal.event` also exist in other
      modules – delete only the Wicket module's files.
- [ ] Config/log cleanup: `projectforge.wicket.developmentMode` (`application.properties`,
      `ConfigurationService`), `Constants.WICKET_REQUEST_TIMEOUT_MINUTES`, `LoggingFilter` `/wa` /
      `/styles/` exclusions (+ `LoggingFilterTest`), `logback-spring.xml` Wicket logger,
      `TomcatConfig` comment, `SetupService` comment, stale comments in `ProjectForgeApp` and
      `DatabaseSupport`.
- [ ] Optional business cleanup: `imageDimensions.xml` (read only by Wicket `WebConstants`),
      `@Deprecated` "used by Wicket" methods (`TimesheetDao`), `ProjectServiceImpl.isNumberFreeForCustomer`,
      "open/Serializable for Wicket" comments.
- [ ] i18n tooling: remove `parseWicketHtml` / the Wicket branch in `I18nKeysSourceAnalyzer`, run
      `bin/pfDev.sh gen`; Wicket-only i18n keys then show up as unused and can be pruned.
- [ ] `projectforge-webapp`: remove the `/wa` proxy (`vite.config.ts`) and `path="/wa/*"`
      (`AuthorizedRoutes.jsx`).
- [ ] Tests: `projectforge-wicket/src/test` goes (`CallAllPagesTest`, `WicketPageTestBase`, …); move
      `UrlHelperTest`/`AssignListHelperTest` if their helpers survive. Adjust `CardDavFilterTest`,
      `GatewayEndpointFilterTest`.
- [ ] Docs: MIGRATION.md (Phase 4, `/wa` mentions), MIGRATION-TaskTree.md, `docs/architecture.md`,
      `site/_docs/development.adoc`, `adminguide.adoc`, `installation.adoc`, `doc/HOWTO-TEST-GATEWAY.md`.
- [ ] Optional afterwards: `NEXT_APP_PATH` → `/` as default (MIGRATION.md Phase 4).

## Verification

- After Phase 1: start the app, check translated texts in next and React (i18n bundle), static resource
  caching headers, plugin menus, license list.
- After each Phase 2 item: e2e spec against the running instance (pattern: `projectforge-next/e2e/*.spec.ts`,
  throw-away entities marked deleted), e.g. ToDo save → notification mail sent with a next link.
- Before Phase 4: `git grep -n "wa/\|WICKET_APPLICATION_PATH\|org.apache.wicket\|org.projectforge.web.wicket"`
  outside `projectforge-wicket` must only show `OrphanedLinkFilter`/`NextMigration` redirect code.
- After Phase 4: `./gradlew build`, full e2e suite, manual smoke test: login + 2FA, calendar, a few old
  bookmarks (`/wa/taskTree`, `/wa/timesheetEdit?id=…`, `/wa/anything`) redirect into next.
