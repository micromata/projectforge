# Error dashboard log

Analyses of the problem dashboard exports (admin → problems → export JSON), newest first. Purpose: on the next
analysis, look up what was already fixed, what was deliberately left alone, and which fixes could have side
effects ("watch" column).

**No confidential data in this file:** no names of users, customers, projects or companies, no search terms, entity
ids, host names or URLs from the export. Describe problems generically (pattern of the input, number of users).

After a deploy, resolve the fixed problems in the dashboard; a REGRESSION entry means a
fix didn't hold.

Each entry lists the dashboard source (class:line or log event code) as it appeared, so a reappearing problem can
be matched even if line numbers move.

## 2026-10-07 (export 05:42, 17 problems OPEN, last 7 days, running version 9.0.2)

Deploy timeline derived from the data: 9.0.0 ~2026-10-05 21:40 UTC, 9.0.1 ~2026-10-06 08:20 UTC, 9.0.2
~2026-10-06 22:35 UTC.

### Already fixed before this analysis

| Dashboard source | Cause | Fix |
|---|---|---|
| `CalendarSubscriptionServiceRest:106` "can't get context user" (~1.35M) | Gateway ICS push (`GatewaySyncPushService.exportIcs`) called `exportCalendar` directly; its `finally` cleared the thread-local user, so every further export of the run failed. | `1a99fb8d2` (9.0.1); errors stop exactly at that deploy. |
| `CollectionHandler:228` PersistentBag | | `c85719fc9` (9.0.1) |

### Not a code problem

| Dashboard source | Assessment |
|---|---|
| `gateway.push.unreachable` (certificate without SAN for the DMZ host name) | Certificate/configuration; last seen 2026-10-06 21:07 UTC, apparently fixed by the admin. |
| `rest.noCredentials` on `/rs/menu` (~1000×, 2 users) | Open browser tabs with expired sessions, notify NONE. Noise. |

### Fixed in `04fd7072b` (develop, after 9.0.2)

| Dashboard source | Cause | Fix | Watch |
|---|---|---|---|
| `AbstractUserPrefCache:282` "User 'null' …" on `LoginService.logout` | Logout via next runs through `/rsPublic` without a thread-local user; `flushToDB(userId)` compared with `loggedInUserId`, skipped the flush, then `clear()` dropped prefs changed since the last periodic flush. | Public `flushToDB(userId)` (only caller: logout) no longer checks the thread-local user, demo-user check kept. | Prefs of a user written on logout although nobody is logged in on the thread; user comes from the own session, so that is intended. |
| `TaskServicesRest.getTree:593` NPE `getSession(...)` | `/rs/task/tree` without a session (`request.getSession(false)` = null). | `ListFilterService.getSearchFilter` and `UserPrefRestService.getEntry/putEntry` accept a null session (session is only used for demo users). | Without session the filter isn't kept per session for demo users. |
| `TimesheetDao.hasUpdateAccess:696` IllegalArgumentException "Required value was null" (+ rollback log `PfPersistenceService:479`) | Timesheet saved without task; `ElementsRegistry` ignores `required` on `@ManyToOne`, so it reached `requireNotNull(obj.taskId)` → 500. | `TimesheetEntityRest.validate` adds a required-field error for `task`. | |
| `DBQueryBuilderByCriteria:129` sort on `vacationModeString` | Computed column, no DB property. | `VacationPagesRest.computedSortProperties` sorts by the translated vacation mode. | |
| `OrderInfo.updateFields:133` "Order without status: {deleted:false}" | Unsaved order from the next edit form (`Auftrag.calculateOrderInfo`) has no status yet. | Error only logged for orders with id. | A persisted order without status is still reported. |
| `HibernateSearchAuftragsPositionBridge:48` "AuftragDO … is null" (10×, 3 users) | `RechnungsPosition.copyTo` sets an id-only `AuftragsPositionDO` stub; the `@IndexedEmbedded` bridge indexed an empty order position → invoice not found by order number until re-index. | `RechnungDao.resolveOrderPositions` replaces stubs by `getReference` in `onInsertOrModify`. | If the message reappears: another write path with stubs (mass update, import?). |
| `DBFullTextResultIterator:120/167` parse errors, e.g. a trailing `/` or a dangling `AND)` in the search term | User input with query syntax errors (HSEARCH600180) logged as ERROR. | Logged as WARN with `persistence.fulltextQueryInvalid` (CLIENT). The user still gets no result for such input (no escaped retry). | |
| `DBFullTextResultIterator` NPE "state is null" (AddressDO) | Presumably a query string predicate built without fields. **Not reproduced.** | `DBPredicate.search()` builds no query-string predicate without fields (string and numeric variant). | If the NPE reappears, the assumed cause was wrong. |
| `TeamEventExternalSubscriptionCache:305/315` "7 of 10 … failing" (ERROR every ~15 min) | Logged on every run in `handleFailingSubscriptions`. | At most once a day, log event `ical.subscriptions.systemicFailure` (EXTERNAL/ADMIN). Heuristic `isSystemicFailure` (≥3 and majority) **deliberately unchanged**. | See open items. |

### Open

- **7 of 10 calendar subscriptions failing** (since 9.0.0). Cause not in the export. Tracking/back-off
  (`e5a22d950`) and the pooled HTTP client with 30 s timeout / 10 MB limit (`c8fe5c757`) were introduced in 9.0.0;
  the old client had no proxy either. Check the server log for `Unable to gather subscription calendar #`:
  - same network error for all (timeout, DNS, proxy) → server network, or the new timeout/size limit is too strict;
  - 404/403 → subscriptions really dead; then the systemic heuristic prevents their deactivation with only
    ~10 active subscriptions (consider "systemic only if the failures started at the same time").
