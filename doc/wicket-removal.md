# Removing Wicket (`projectforge-wicket`, `/wa`)

> Plan for removing the Wicket frontend entirely. It complements Phase 4 of
> [projectforge-next/MIGRATION.md](../projectforge-next/MIGRATION.md), which only states the goal
> ("remove `projectforge-wicket` from `settings.gradle.kts` + build, remove `/wa` serving/filters").
> Status as of 2026-10-03: **no core menu entry opens Wicket any more**. The last two were the HR view
> (`next/hrList`) and the deprecated favorites (`wa/userPrefList`, page removed since 2026-10-04).

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

29 concrete page classes are left (`projectforge-wicket`).
"Reached" means a real link or registration – mentions in comments are not counted. Sources:
`MenuItemRegistry`, `WebRegistry.init()`, `NextMigration.MIGRATED`, `wa/…` links in next/REST code.

`MenuItemRegistry` is gone: every menu entry resolves to `next/…`, so `WicketMenuBuilder` only uses the url.

| Group | Pages | Reached from |
|---|---|---|
| Escape hatch ("classic version") behind a migrated next page | access (`AccessListPage`/`AccessEditPage`), account (`KontoListPage`/`KontoEditPage`), accountingRecord (`AccountingRecordListPage`/`AccountingRecordEditPage`), gantt (`GanttChart*Page`), task (`TaskListPage`/`TaskEditPage`) | `NextMigration` entries with `legacyApp = WICKET` and `offerLegacyLink = true` |
| Escape hatch, hard-coded in next | `TaskTreePage`, `PhoneCallPage` | `legacyUrl` in `taskTree/page.tsx`, `phone-call-page.tsx` |
| Only reached from other legacy pages | `TaskWizardPage` (from `TaskTreePage`, `AccessListPage`), `TimesheetListPage`/`TimesheetEditPage` (from task tree/edit, old calendar), `GroupListPage`/`GroupEditPage` (group select panels, `TaskWizardForm`), `CalendarPage` (`wa/oldCalendar`), `TeamCalCalendarPage` (`wa/oldTeamCalendar`) and below it `TeamCalListPage`, `TeamCalEditPage`, `TeamEventListPage`, `TeamEventEditPage`, `TeamCalImportPage` | Wicket pages among themselves; "classics" links of `react/teamCal` (`TeamCalPagesRest`) and `react/group` (`GroupPagesRest.kt:153` still answers `wa/groupList`, although `NextMigration` sends the group's way back to React) |
| Infrastructure | `ErrorPage`, `PageExpiredPage`, `MessagePage`, the other `Abstract*Page`s | Wicket itself |

Without the escape hatches and the hidden mounts, Wicket is still needed only for the task
wizard and the old calendar pages. An escape-hatch category can only go
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
      mount points), used by `SystemRest`; `IProjectForgeEndpoints` deleted.
- [x] **`MenuCustomizationController`** (`/rs/menucustomization`) – no caller anywhere, deleted.
- [x] **Dead code** – `PacmanViewPage` (+ `scripts/pacman`, LESS rule), `AbstractViewPage`,
      `AbstractSecuredPopupPage` and `MenuItemRegistry` deleted. A second sweep removed 42 further classes
      (+ their `.html`) that nothing referenced any more (old panels, providers, validators, …) and the
      i18n keys only they used. Classes used only by still-mounted legacy pages (e.g.
      `BirthdayEventsProvider` of the old calendar) stay until their page goes.
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
- [x] **Exports only in Wicket pages** – Kost1/Kost2 Excel export (`Kost1EntityRest`/`Kost2EntityRest`
      `exportAsExcel`), incoming invoice list and cost-assignment Excel (list bar of `next/creditor-invoice`),
      SEPA transfer (pain.001) as a button of the edit page and the mass update (Wicket never had a separate
      SEPA page), CSV import via `next/creditor-invoice-import`. The Wicket pages of cost 1/2, cost 2 types,
      projects and incoming invoices are removed; `wa/cost*`, `wa/project*`, `wa/incomingInvoice*` are
      redirected by `OrphanedLinkFilter` (see MIGRATION.md).
- [x] **DATEV import** (`DatevImportPage`) – removed with its form, storage panel and the legacy
      `BuchungssatzExcelImporter`/`KontenplanExcelImporter`; `next/datev-import` covers it (status filter,
      per-row error texts, BWA preview, log viewer). The upload limit is `projectforge.max-file-size.datev`
      again (default now `10MB`, the original file includes the report sheets). `wa/datevImport` redirects
      to `next/datev-import` (`OrphanedLinkFilter`). The Kost1/Kost2 tooltips of the preview are back
      (`ImportColumn.tooltipField`); dropped: the Excel download of the validated file.
- [x] **`AdminPage`** – all tools live in `next/system` (`SystemRest`); `AdminPage`/`AdminForm` deleted,
      `wa/admin` always redirects to `next/system` (`OrphanedLinkFilter`, no escape hatch any more). Dropped:
      "Dump database" (Wicket only threw `UnsupportedOperationException`) and "Update all user prefs"
      (`UserXmlPreferencesMigrationDao` deleted – XML user prefs are no longer written, only read on a cache
      miss and stored as JSON).
- [x] **User preferences** (`UserPrefListPage`/`UserPrefEditPage`) – removed without replacement, together
      with the Wicket favorites widgets (`FavoritesChoicePanel` in the task/customer/project select panels,
      the template dropdown and "save as template" of `TimesheetEditForm`, the "Favorites"/"Templates" menu
      entries). `wa/userPrefList`/`wa/userPrefEdit` redirect to the next start page (`OrphanedLinkFilter`).
      The `UserPrefArea`s:
      - `TASK_FAVORITE` kept: next reads and writes it (`TaskFavoritesService`/`TaskFavoritesRest`,
        `TaskFavoritesMenu` in the task select field).
      - `TIMESHEET_TEMPLATE` kept read-only: `TimesheetFavoritesService` migrates the entries into the JSON
        timesheet favorites (automatically and via the "old templates" button of the timesheet form).
      - `KUNDE_FAVORITE`, `PROJEKT_FAVORITE`, `USER_FAVORITE` dropped (with `KundeFavorite`,
        `ProjektFavorite`, `UserFavorite`): only Wicket read them, `USER_FAVORITE` not even that.
      The database rows of the dropped areas (and the old `TODO_FAVORITE` rows) are left untouched; nothing
      reads them any more.
- [x] **`ExternalResourceLoader`** – only a Wicket adapter on `I18nHelper`; next already applies the
      customer overrides (`I18nCustomerNextRest`, `i18n/customer-overrides.test.ts`). Goes with the module.

## Phase 3 – Close the escape hatches

- [ ] `NextMigration`: remove `offerLegacyLink`/`ESCAPE_HATCH_PARAM`; **keep** `legacyApp`/`legacyRoute`
      for the `OrphanedLinkFilter` redirects. Adjust `NextMigrationTest`, `PageResolverTest`.
- [ ] projectforge-next: `LegacyPageLink` usages, `hooks/use-legacy-edit-url.ts`, the `wa/` handling in
      `lib/menu-url.ts`, `lib/config.ts`, `task-edit-link.tsx`, `system-alert-banner.tsx`; the hard
      links in `taskTree/page.tsx`, `phone-call-page.tsx` (`system-page.tsx`: done). Tests: e2e
      `legacy-page-link.spec.ts`, `invoice-edit.spec.ts`, `task-edit.spec.ts`, `quick-access.spec.ts`;
      unit `menu-url.test.ts`, `menu-search.test.ts`.
- [ ] `MenuItemDefId.TASK_TREE`: drop the `wa/taskTree` fallback of `nextRouteUrl`.
- [ ] `OrphanedLinkFilter`: remove the escape-hatch pass-throughs (`?legacyEscape`; `/wa/admin`: done), add a
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
- [ ] Delete `projectforge-business/.../user/filter/WicketUserFilter.kt` (its dead `/wa/setup` branch is
      already removed – setup lives in next).
- [ ] Static resources: the module ships `src/main/webapp` (images, styles, fonts, scripts) as `static`.
      Verify nothing outside Wicket loads them (favicon, mail templates, React) before deleting.
- [ ] Split packages: `org.projectforge.web`, `.web.session`, `.web.teamcal.event` also exist in other
      modules – delete only the Wicket module's files.
- [ ] Config/log cleanup: `projectforge.wicket.developmentMode` (`application.properties`,
      `ConfigurationService`), `Constants.WICKET_REQUEST_TIMEOUT_MINUTES`, `LoggingFilter` `/wa` /
      `/styles/` exclusions (+ `LoggingFilterTest`), `logback-spring.xml` Wicket logger,
      `TomcatConfig` comment (`maxPartCount` for Wicket forms).
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
