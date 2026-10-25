# Kost2 reallocation of accounting records (retroactive project move)

Target release: 9.0.16.

## Context
Timesheets are to be moved retroactively from a collective task to a project of their own, with its own Kost2.
An example of a collective task is "other projects" of a business unit, booked on collective Kost2 A.

The accounting records imported from DATEV still carry the Kost2 of that time:
- **Salaries:** `EmployeeSalaryExportDao` splits the gross salary of each employee and month over Kost2, weighted by
  the timesheet hours (account 6000, counter account 3791, Kost1 = employee).
  - The split is not stored.
  - Every export recomputes it from `TimesheetDO.kost2`.
- **Revenues:** booked on revenue accounts.
  - The Kost2 comes from the cost assignments of the debitor invoices.
  - The invoice can be found via `beleg` (`BookedInvoices.tokens`).

The original Kost2 of an accounting record is kept. ProjectForge computes with a reallocated assignment instead.

**Decisions:**
- **Shares table per accounting record** instead of a single second field, because one record often has to be split.
- **Salary shares from a move log** of the timesheets.
- **Additional sources** besides salaries and revenues:
  - incoming invoices, found via `beleg`
  - flat rules "Kost2 A → B"

## Data model
Flyway: `V9.0.16.2__RELEASE-Kost2Reallocation.sql`.
- `V9.0.16.1` is already taken on `claude-dev2` (`V9.0.16.1__RELEASE-Chat.sql`).
- Check the open branches again before merging.

1. **`Kost2ReallocationDO`** (`t_fibu_kost2_reallocation`), the reallocation run:
   - Fields: description, target project, period (from/to month), status `DRAFT|APPLIED|REVERTED`, comment.
   - `@WithHistory`.
   - Access: FINANCE only. The DAO overrides both `hasAccess` and `hasInsertAccess` (see the security rules).
2. **`Kost2ReallocationRuleDO`**: run, source Kost2, target Kost2, optional account range.
   - These are the rules A → B.
   - Default rules are created from the target project by matching the Kost2 type, like `TimesheetMassUpdateRest`
     (l.330-347).
3. **`Kost2ReallocationTimesheetDO`**, the move log:
   - Fields: run, timesheet, user, year, month, old task, new task, old Kost2, new Kost2, millis.
   - The millis are computed like `MonthlyEmployeeReportEntry.getMillis`, so they match the salary export.
4. **`BuchungssatzKost2ShareDO`** (`t_fibu_buchungssatz_kost2_share`):
   - Fields: `buchungssatz_fk`, `kost2_fk`, `betrag` (same sign as the record's `betrag`),
     source `SALARY|INVOICE|INCOMING_INVOICE|RULE`, `run_fk`.
   - Invariant: the shares add up to the record's `betrag`.
   - The last share takes the rounding difference, like the correction row of the salary export.
   - A record without shares counts on its own `kost2`.
   - **No cascading collection in `BuchungssatzDO`.** The DATEV import rebuilds the DO and calls `insertOrUpdate`
     (`DatevRecordImportJob.run`), which would wipe the shares.
   - The shares have their own DAO. They are loaded in bulk via `buchungssatz.id IN (...)`, chunked to stay below
     the bind parameter limit.

## Central reuse: the effective view
`BuchungssatzKost2ShareService.expand(records): List<BuchungssatzDO>`:
- For each share it returns a transient copy with `kost2` = the share's Kost2, `betrag` = the share, and a reference
  to the original record.
- Records without shares are returned unchanged.
- `calculate()`/`isIgnore` stay with the original record (the 1.0.0.0 rule).

Consumers that switch to this view:

| Consumer | Change |
|---|---|
| `ContributionMarginService.selectRecords` (l.688-711) | Besides `isIn("kost2.id")`, also loads the records whose *shares* point to the Kost2, then expands them. `ContributionMarginCalculator.recordEntries` stays as it is. |
| `ContributionMarginCache` | Gets a listener on the share DAO. |
| `ReportDao.loadReport` / `Report.java` (l.388-408) | The Kost2 regex filter runs on the expanded records. |
| `AccountingRecordEntityRest.reportRecords` (drill-down) | Shows the original record with its shares. |
| Scripting: `AccountingRecord.kt`, `BuchungssatzImpl` | Also offer `effectiveKost2` and the shares. |
| BWA | No change. It works on accounts only. |

Display in next (`components/features/accounting-record/`):
- List: column "reallocated".
- Edit form: the shares read-only, with a link to the run.
- Excel export (`AccountingRecordEntityRest` l.249-294): columns for the shares.

## DATEV re-import
Reconcile in `DatevRecordImportStorage` and in the job:
- If `kost2` and `betrag` of a matched record are unchanged, the shares stay. They are attached to the record's id.
- If either changed, the shares are dropped.
  - The preview shows "reallocation dropped". The DTO diff gets a flag for it.
  - Runs are idempotent and can be applied again.

## Reallocation run (hand-built next wizard, FINANCE only)

1. **Define the run:**
   - Source: collective Kost2 and/or tasks.
   - Target: project, target task, period.
   - Kost2 mapping per Kost2 type: default rules, editable.

2. **Move timesheets:**
   - Selected by task subtree, period and employee.
   - Moved with the task and Kost2 mapping. The logic of `TimesheetMassUpdateRest` (taskAndKost2 / KOST2_REMAP)
     moves into a service that both use.
   - One log entry is written per timesheet.
   - Explicit privileged path for FINANCE, tested both ways:
     - It bypasses `checkTimesheetProtection` (`protectTimesheetsUntil`), also for FINANCE's own timesheets.
     - `validateKost2` accepts an ended source project.
   - The timesheet history is kept, because the moves go through `timesheetDao.update`.

3. **Salaries:**
   - For each combination of (employee's Kost1, year, month, old Kost2 A) that has log entries, take the salary
     records whose (effective) Kost2 is A.
     - Salary records are those on account 6000 / counter account 3791.
     - Move these constants out of `EmployeeSalaryExportDao` into one shared place.
   - share B = betrag × moved millis (A → B) / original millis (A)
   - original millis (A) = current millis on A + millis moved out of A, via `MonthlyEmployeeReportDao.getReport`.
   - The rest stays on A.

4. **Revenues (debitor invoices):**
   - Applies to records on revenue accounts (`ContributionMarginConfig.isRevenueAccount`) with a Kost2 in the source.
   - `beleg` is matched to a `RechnungDO` via `BookedInvoices.tokens`.
   - Target Kost2 per position:
     - its cost assignment, if that already points to the target project;
     - otherwise the target project's Kost2 of the same type, if `rechnung.projekt`,
       `auftragsPosition.auftrag.projekt` or `auftragsPosition.task` (in the moved subtree) point to the target.
   - The record is split by the net amounts of the positions.
   - Optional: update the invoices' cost assignments as well.

5. **Incoming invoices:** the same way, via `beleg` ↔ `EingangsrechnungDO` and its cost assignments.
   - **Open question:** what does `beleg` hold for creditor records (`referenz`? a document number?)? Check this
     against real data first.

6. **Rules A → B:** all remaining records of the source Kost2 in the period (optionally one account range) go
   completely to the target Kost2.

7. **Preview, apply, revert:**
   - Preview: a diff per record (original, old shares, new shares) and totals per Kost2.
   - Apply: in one transaction.
   - Revert: deletes the run's shares and moves the timesheets back according to the log. This is only allowed if no
     later run changed the same records or timesheets.
   - A later run works on the effective shares. For example, it can split the remaining share on A further.

## Phases
1. Data model, share DAO, `expand`, the consumers (contribution margin, reporting, scripting), display and Excel
   export, keeping the shares on DATEV re-import.
2. Run entity and wizard with rules A → B (preview, apply, revert).
3. Timesheet move with log and salary shares.
4. Revenues via debitor invoices.
5. Incoming invoices, once the `beleg` question is settled.

Every phase also includes:
- i18n (German texts without "Sie")
- `changelog/unreleased/yyyyMMdd-kost2-reallocation.json` (EN + DE)
- `RestEndpointAccessCheckTest` (`@AccessChecked`)
- state changes via POST only

## Critical files
- `projectforge-business/.../fibu/kost/BuchungssatzDO.kt`, `BuchungssatzDao.kt`
- `projectforge-rest/.../fibu/importer/DatevRecordImportJob.kt`, `DatevRecordImportStorage.kt`, `DatevRecordImportDTO.kt`
- `projectforge-business/.../fibu/contributionmargin/ContributionMarginService.kt`, `ContributionMarginCache.kt`
- `projectforge-business/.../fibu/kost/reporting/ReportDao.java`, `Report.java`
- `projectforge-business/.../fibu/datev/EmployeeSalaryExportDao.java`, `MonthlyEmployeeReportDao.java`
- `projectforge-rest/.../TimesheetMassUpdateRest.kt`, `projectforge-business/.../timesheet/TimesheetDao.kt`
- `projectforge-business/.../scripting/support/BookedInvoices.kt`
- `projectforge-rest/.../orga/AccountingRecordEntityRest.kt`, `projectforge-next/components/features/accounting-record/`

## Verification
- Unit tests:
  - `expand`: totals, rounding, sign for debit/credit.
  - Salary shares against a hand-computed `EmployeeSalaryExportDao` example (2 employees, one partial move).
  - Matching invoices via `beleg`.
  - Rules.
  - Re-import: shares kept or dropped.
- Access tests:
  - TEST_USER is refused, FINANCE is allowed.
  - Only FINANCE can bypass the timesheet protection.
- Contribution margin of a test project before and after a run:
  - The total over all projects is unchanged.
  - Costs and revenues have moved from the collective project to the target project.
  - After reverting, everything is identical to the state before.
- e2e on a slot server (`bin/pfDev.sh e2e <n>`): a wizard run including the preview.
