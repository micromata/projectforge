# Plan: migrate the vacation account (`react/vacationAccount/dynamic`) to Next.js

## Context
The leave account ("Urlaubskonto", menu `VACATION_ACCOUNT`, also in the user-name menu) is still a React
UILayout page (`VacationAccountPageRest` + the React custom components `vacation.statistics` /
`vacation.entries`). It moves to projectforge-next as a hand-built standalone page (like `hrList`,
`monthlyEmployeeReport`) on a layout-free JSON endpoint; the old page and its React components are deleted.

**Order (user decision):** before this plan is implemented, `vacation` (list **and** edit, together, as
one `MIGRATED` entry) and `leaveAccountEntry` are migrated to Next in their own tasks. This plan therefore takes every edit/new URL from the backend
(`PagesResolver`), so it points at whichever frontend serves those pages at the time — no frontend change
needed when they switch.

Two silent bugs of the old page get fixed: conflicting vacations get a red row (the class
`ag-row-red` was styled nowhere), and next year's statistics appear as a third column (computed but never sent).

## Backend

1. **New `projectforge-rest/.../rest/VacationAccountRest.kt`**, `@RequestMapping("${Rest.URL}/vacationAccount")`
   (same category, so 2FA/gateway config keeps matching), every method `@AccessChecked(...)`:
   - `GET /rs/vacationAccount?employeeId=` → `VacationAccount` DTO. Employee resolution as before:
     HR (`vacationService.hasLoggedInUserHRVacationAccess()`): param → user pref (`"vacation"/"account"`,
     `VacationAccountUserPref`) → own employee; non-HR: always own (the param is ignored). An HR request
     with an `employeeId` stores the pref (replaces `watchFields`). No employee for the user → DTO with
     `employee = null` instead of the old `!!` NPE.
   - `POST /rs/vacationAccount/recalculate?employeeId=` → HR only (otherwise 403 / `AccessException`),
     `remainingLeaveDao.markAsDeleted(employeeId, Year.now().value, checkAccess = false)`, returns the
     fresh `VacationAccount`.
   - Move over from the old class: `readVacations`, conflict flag via `ConflictingVacationsCache`, leave
     account entries (previous year Jan 1 to current year Dec 31, newest first), `getAverageWorkingTimeStats`.
2. **DTO** (`rest/dto/VacationAccount.kt`, plain classes like `dto/HRView.kt`):
   - `employee: {id, displayName}?`, `hrAccess: Boolean` (shows the employee picker), `canRecalculate: Boolean`
     (old condition: HR and `current.remainingLeaveFromPreviousYear != prev.vacationDaysLeftInYear`),
   - `statistics: List<VacationStatsFormatted>` (previous, current, next year),
   - `previousYearLeaveUnusedLabel: String` (the `translateMsg("vacation.previousyearleaveunused", date)` text),
   - `years: List<{year, vacations: List<Vacation>}>` (next, current, previous, as in the old table),
     each `Vacation` with `conflict` and an `editUrl`,
   - `leaveAccountEntries: List<LeaveAccountEntry>` with `editUrl`,
   - `workingHoursStatistics: String?`,
   - URLs: `newVacationUrl` (`getEditPageUrl(VacationPagesRest, params = employee + returnToCaller=account, absolute = true)`),
     `vacationListUrl` (`getListPageUrl(VacationPagesRest)` or its next successor), `exportUrl` (`getDynamicPageUrl(VacationExportPageRest)`, still React).
   Edit URLs via `PagesResolver.getEditPageUrl(..., id, returnToCaller = "account", absolute = true)`.
3. **Return path:** `VacationPagesRest.createReturnToCallerResponseAction` and
   `LeaveAccountEntryPagesRest.createReturnToCallerResponseAction` (or their next successors) answer
   `"account"` with `/${Constants.NEXT_APP_PATH}vacationAccount` (works from React via `RedirectToNext`
   and from Next via `resolveMenuUrl`). Same for `VacationSendMailService.getLinkToVacationEntry` only if
   its target needs it (it points to the vacation edit, which handles the return itself).
4. **Delete** `VacationAccountPageRest.kt`; remove its 3 lines from
   `projectforge-application/src/test/resources/rest-endpoint-access-baseline.txt`.
5. **Menu:** `MenuItemDefId.VACATION_ACCOUNT` → `"next/vacationAccount"` (literal, like `HR_VIEW`).
6. **OrphanedLinkFilter:** `/react/vacationAccount` and `/react/vacationAccount/…` (incl. `/dynamic`,
   `/dynamic/{id}`, `?id=`) → `/next/vacationAccount` (keep a numeric id as `?employeeId=`), same pattern as
   `redirectScriptExecutePage`; tests in `OrphanedLinkFilterTest`.
7. **2FA:** the legacy page wasn't gated → no entry in `ProjectForge2FAInitialization`/`NextMigration2FATest`
   (check that `NextMigration2FATest` still passes).
8. Not in `NextMigration.MIGRATED` (standalone page; `HAND_BUILT_CATEGORIES` unchanged).
9. Test `VacationAccountRestTest` (HR vs. non-HR employee resolution, pref, recalculate rejected for non-HR).

## Frontend (projectforge-next)

- Route `app/(authenticated)/vacationAccount/page.tsx`: `"use client"`, `<Suspense>` around the page
  (`?employeeId=` via `useSearchParams`), as in `hrList/page.tsx`.
- `lib/rs/vacation-account.ts`: `fetchVacationAccount(employeeId?, signal)`, `recalculateRemainingLeave(employeeId)`
  via `request` from `lib/rs/client.ts`.
- `components/features/vacation-account/`:
  - `vacation-account-page.tsx`: `PageShell` + `PageTitleRow` (category `menu.vacation`? / title
    `vacation.leaveaccount.title`), actions: "New" (newVacationUrl), "Recalculate" (danger, only if
    `canRecalculate`, then `setQueryData`), links to the vacation list and export (`resolveMenuUrl`).
    `useQuery(["vacationAccount", employeeId], keepPreviousData)`. HR: `PersonSelect kind="employee"`
    (`components/shared/person-select.tsx`); a choice updates `employeeId` (URL replace, no `/next` prefix,
    see the basePath pitfall). Working-days line (`fibu.common.workingDays`) and the info alert
    (`vacation.subscription` / `vacation.subscription.info`).
  - `vacation-statistics-table.tsx`: hand-built matrix (like `hr-view-matrix.tsx`), header = years,
    3 value columns, the 10 rows from the old `VacationStatistics.jsx` (bold "subtotal"/"available"),
    a row dropped when all values are empty.
  - `vacation-entries-table.tsx`: columns as before (start, end, status, days, special, replacement,
    other replacements, manager, mode, comment), a year block per entry of `years` with a top border,
    conflict → red row, null-safe replacement/manager, row click → `editUrl` via `resolveMenuUrl`.
  - `leave-account-entries-table.tsx`: date, amount (`amountFormatted` instead of the raw BigDecimal),
    description; row click → `editUrl`.
  - `types.ts` mirrors the DTO.
- Texts only via `useTranslations` with the existing backend keys spelled out (scanner); then
  `bin/pfDev.sh gen` (check that the generated files were really rewritten, `--rerun-tasks` if needed).

## Delete from the old React app
- `projectforge-webapp/src/components/base/dynamicLayout/components/customized/components/vacation/`
  (`VacationStatistics*.jsx`, `VacationTable.jsx`, `VacationEntries.jsx`, `VacationLeaveAccountTable.jsx`,
  `Vacation.module.scss`) and the `vacation.entries`/`vacation.statistics` cases in `customized/index.jsx`
  (only used by this page — check with grep first).

## Docs / changelog
- `projectforge-next/MIGRATION.md`: a Phase-3 bullet "Urlaubskonto" (standalone, edit URLs from the backend,
  bugs fixed).
- `changelog/unreleased/20261009-vacation-account-next.json` (type `improved`, en + de, de with "du" or no direct address).

## Verification
- `./gradlew :projectforge-rest:test --tests "*VacationAccountRest*" --tests "*OrphanedLinkFilterTest*" --tests "*NextMigration*"`,
  `:projectforge-application:test --tests "*RestEndpointAccessCheckTest*" --tests "*GenerateNextI18nMessagesTest*"`.
- `npm run lint`, `tsc`, unit tests in projectforge-next.
- e2e `e2e/vacation-account.spec.ts` (new lane tag in `playwright.config.ts`): page loads, statistics with
  3 years, a seeded vacation (far future, marked deleted afterwards) appears and its row click leads to
  the edit page, which returns to `/next/vacationAccount`; HR picker switches the employee.
- By hand against the running instance (`pfDev.sh run/dev <n>`, test account): menu entry, old URL
  `/react/vacationAccount/dynamic` redirects, recalculate as HR.
