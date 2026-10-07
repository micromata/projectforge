# Lanes & Planes interface

ProjectForge pushes the active employees with their cost centers (Kost1), the cost units they may book on (Kost2)
and their creditor account to the travel management tool Lanes & Planes (L&P). This file collects the findings about the L&P API and the current state.

**No confidential data in this file:** no names of users or companies, no e-mail addresses, ids (invoice profiles,
users), cost center names or numbers of a real installation. Describe findings generically.

## Status

- Implemented: package `org.projectforge.business.lanesandplanes` (projectforge-business), disabled by default.
- Tested: unit tests (bookable Kost2 per user, patterns, general Kost1/Kost2, settings validation, JSON format, gzip) and
  a dry run against an anonymized database (see [Dry run findings](#dry-run-findings)). Not yet tested: a dry run
  against real data and a real push to L&P.
- Open questions to the L&P support: see [Open questions](#open-questions). Don't switch off the dry run before
  they are answered and the comparison with the user export (see [Before the first real push](#before-the-first-real-push))
  looks as expected.

## The L&P ExtAPI (user import)

Documentation: https://support.lanes-planes.com/hc/de/articles/31245986692370-Documentation-User-Import-via-ExtAPI

- `POST https://api.lanes-planes.com/ext/users`, JSON body `{"users": [...]}`, header
  `Authorization: Token token=<APIKEY>`; gzip (`Content-Encoding: gzip`) recommended. No GET/PUT/DELETE.
- API key: created by an L&P user in *User account → Personal settings → Create*. Shown only once, doesn't
  expire, revoked by deleting it. It belongs to that L&P account, so use an admin account that stays active (and
  is part of the push, see below).
- **Full replace:** every push contains the complete user list. **Users missing from the payload are
  deactivated** in L&P. The e-mail address is the unique key of a user.
- Processed asynchronously: the HTTP answer only says, whether the request was accepted (200, 400 malformed, 401
  token, 403 not allowed, 422 invalid data, 429 too many requests, 500). The result per user is shown in the
  *user import log* of the L&P company account.
- If a user with responsibilities (admin, manager, accountant, travel manager of others) is missing, the whole
  push fails and nothing is changed. A single invalid user without responsibilities fails alone ("failed" in the
  import log), the others are imported.
- Required per user: `first_name`, `last_name`, `email`, `accounting_invoice_profile_ids` (ids provided by L&P).
  Depending on the company settings `managers_emails`, `cost_centers` and `cost_units` may be required as well.
- Cost centers (`cost_centers`) and cost units (`cost_units`) only exist per user: lists of
  `{ident, name, valid_from?, valid_until?}`. They needn't be created in L&P beforehand. A cost center that was
  created manually in L&P must be sent with its L&P name in both `ident` and `name`, otherwise a second one is
  created.
- Fields sent empty (`[]`/`null`) clear the values maintained in L&P. The documentation doesn't say what happens
  with fields that aren't sent at all; the implementation assumes they are kept (to be verified, see open
  questions).
- The API can't read users. The L&P company account offers a CSV download of the users instead.
- There is no API for the company-wide cost center list (*Company account → Settings & policies → Manage cost
  centers*): it can't be read, uploaded or deleted. Cost centers sent in the user import are created there
  implicitly. The list can only be maintained by hand (new, rename, delete, "assign all to all users", "remove all
  from all users"). Cost centers can have at most 102 characters.
- Numbers must be sent without dots, otherwise the DATEV export of L&P fails.

## What ProjectForge sends

| L&P field | Source |
|---|---|
| `ident` | `PFUserDO.id` |
| `first_name`, `last_name`, `email` | `PFUserDO`; employees without one of them are skipped (warning in the log), as are duplicate e-mails (case-insensitive) |
| `middle_name` | Always sent empty (clears the middle names maintained in L&P; there is no middle name in ProjectForge) |
| `abbreviation` | `PFUserDO.nickname`, sent empty if not set (clears the value in L&P) |
| `personnel_number` | `EmployeeDO.staffNumber` (not sent if empty) |
| `creditor_account` | Number of the account of the employee (`EmployeeDO.konto`, *creditor account* in the employee edit page), not sent if empty |
| `reference_cost_center` | Kost1 of the employee, number without dots (`########`), not sent if empty |
| `cost_centers` | The general Kost1 (settings), the same for all users, `ident` = number without dots (`########`), `name` = number and description (`<number> <description>`). Not sent if no general Kost1 are configured (the cost centers maintained in L&P are kept then) |
| `cost_units` | The general Kost2 plus the bookable Kost2 matching the patterns, `ident` = number without dots (`########`), `name` = number, project and Kost2 type (`<number> <project> - <type>`, L&P convention; the description for a Kost2 without project) |
| `accounting_invoice_profile_ids` | Settings, the same for all users |

Names have at most 102 characters.

Not sent at all (so kept as maintained in L&P, if the assumption above holds): `roles`, `group_ids`,
`managers_emails`, `delegation`, `company_creditor_account`.

Users: all active employees (`EmployeeService.selectAllActive`) whose user isn't deactivated or deleted. Users
without any bookable Kost2 are sent nevertheless (otherwise they would be deactivated).

Additional users (settings): L&P users who aren't employees, e.g. an external accountant or the service admin of
L&P. They are sent with `first_name`, `last_name`, `email` and `accounting_invoice_profile_ids` only (no `ident`,
no cost centers etc.), so everything else is kept as maintained in L&P. Without them, the full replace would
deactivate them, and a missing user with responsibilities lets the whole push fail. An additional user with the
e-mail of an employee is skipped (warning in the log).

Bookable Kost2 of a user: the active Kost2 (`effectiveKostentraegerStatus` active, not deleted) of all tasks,
- for which one of the user's groups has the right to insert own time sheets (`AccessType.OWN_TIMESHEETS`),
- which are bookable (`TimesheetDao.checkTaskBookable(taskNode, …)`: not closed/deleted, no `TREE_CLOSED`/
  `NO_BOOKING`, `ONLY_LEAFS` respected),
- with the Kost2 of the task (`TaskTree.getKost2List`, including the black/white list of the task),
- restricted to the configured patterns (glob on the formatted number: `*` any characters including dots, `?` a
  single one, the whole number must match, e.g. `5.*.02`). Without patterns, all bookable Kost2 are sent.

General Kost1 and Kost2: configured lists of formatted numbers sent to every user, independent of the booking
rights (the Kost1 as cost centers, e.g. the internal 1.* range; the Kost2 as cost units in addition to the bookable
ones). Unknown or inactive ones are skipped with a warning; they are validated on saving the settings. A Kost2 that
is general and bookable is sent once.

Unlike the time sheet access check, membership in the admin group doesn't grant all tasks (otherwise admins would
get every Kost2).

## Safety

- `dryRun=true` by default: the payload is only written to `<work directory>/lanes-and-planes-users.json`, nothing
  is sent (the API key isn't needed). The file contains personal data (names, e-mails).
- No push, if the API key or the invoice profile ids are missing, if fewer than `minUsers` users are found, or if
  the number of users dropped by more than half since the last push (in memory; a restart resets it).
- Only pushed, if the payload changed since the last successful push (in memory; the first run after a restart
  always pushes).
- Errors: log events `lanesAndPlanes.push.failed` / `lanesAndPlanes.push.refused`, tile "Lanes & Planes" on the
  problem dashboard, sync statistics `lanes-and-planes`.

## Configuration

Technical settings in `projectforge.properties` (defaults in
`projectforge-business/src/main/resources/application.properties`, read on start only). The API key stays out of
the database and its history, the dry run switch stays with operations:

```properties
projectforge.lanesandplanes.enabled=true
projectforge.lanesandplanes.apiKey=<API key of L&P>
projectforge.lanesandplanes.dryRun=true
projectforge.lanesandplanes.minUsers=1
projectforge.lanesandplanes.syncIntervalMs=3600000
projectforge.lanesandplanes.initialDelayMs=300000
```

Content settings in the configuration parameter `lanesAndPlanes` (JSON, *Administration → Configuration*, edited
by finance/controlling with its own editor, validated on saving, changes traced by the history). Read on every
run, so a change is sent by the next run (hourly by default):

```json
{
  "accountingInvoiceProfileIds": [0],
  "generalKost1": ["1.xxx.xx.xx"],
  "generalKost2": ["5.xxx.xx.xx"],
  "kost2Patterns": ["5.*.02", "5.*.11"],
  "additionalUsers": [{"email": "accountant@example.org", "firstName": "First", "lastName": "Last"}]
}
```

- `accountingInvoiceProfileIds`: required, provided by L&P (also needed for the dry run).
- `generalKost1`: sent to all users as cost centers. Empty: `cost_centers` isn't sent.
- `generalKost2`: sent to all users as cost units.
- `kost2Patterns`: restricts the bookable Kost2 per user (cost units). Empty: all.
- `additionalUsers`: L&P users who aren't employees, see above.

The creditor account is maintained per employee (*HR → Employees*, field *creditor account*, a DATEV account).

## Findings from an L&P user export (CSV)

The CSV download of the company account (separator `;`) has the columns `user_id`, `email`, `first_name`,
`middle_name`, `last_name`, `personnel_number`, `abbreviation`, `creditor_account`, `company_creditor_account`,
`reference_cost_center`, `role`, `booking_status`, `travel_managers`, `managed_users`, `cost_centers`,
`invoice_profiles`.

- `booking_status` looks like `active: true, signup_completion: true, invoice_profiles: true, cost_centers: true`.
- `invoice_profiles` (`ID: <id> Name: <name>`) gives the id needed for `accountingInvoiceProfileIds`. In the
  analyzed export all users had the same single profile.
- **There is no column for cost units.** The exported assignments are all cost centers: the same list for every
  user, numbers without dots followed by the name in one string (`1xxxxxxx <name>`), mostly Kost1 of the internal
  1.* range plus a few general 5.* entries (Kost2).
- **The export is incomplete:** the project Kost2 (4.* and 5.*, Kost2 types for miscellaneous and travel only)
  aren't in the export. They are cost centers of the company-wide list as well, and every entry of that list is
  assigned to every user ("assign all to all users"). So today every user may pick every cost center, not only
  the Kost2 the user may book on. Their names were typed by hand and differ slightly
  (`<number> <project> - <type>`, partly `<number> - <project> - <type>`).
- Lists in the CSV are comma separated, but names may contain commas: split before `, <8 digits> `.
- `reference_cost_center` was empty for all users, `creditor_account` and `personnel_number` were filled for most.
  `creditor_account` is the DATEV account of the employee, which wasn't stored in ProjectForge before (now
  `EmployeeDO.konto`); it has to be set once per employee, e.g. by comparing the names with the export.
- Nearly all users have a travel manager (`travel_managers`): managers are probably required by the company
  settings.
- Role `guest`: guest profiles created by L&P (e-mail `tickets-guest-<number>@lanes-planes.com`), partly generic
  placeholders, partly named after real persons who also have their regular L&P account. `guest` isn't one of
  the roles of the import documentation.

Consequences:

1. Cost centers are Kost1, cost units are Kost2. ProjectForge sends the general Kost1 as `cost_centers` and the
   general plus the bookable Kost2 as `cost_units`. The full replace of `cost_centers` removes the project Kost2
   (and the general 5.* entries) entered by hand as cost centers from every user; they stay in the company-wide
   list, unassigned, and can be deleted there afterwards (to be confirmed, see open questions).
2. Numbers are sent without dots (needed by the DATEV export of L&P), the name of a Kost2 follows the L&P
   convention (`<number> <project> - <type>`). A cost center whose name differs from the existing one (by hand) is
   created anew, the old one stays unassigned in the company-wide list. Don't use "assign all to all users" after
   the first push: it would undo the restriction until the next push.
3. Guest profiles aren't employees in ProjectForge and are deliberately not sent (decided: guests are out of
   scope). If L&P doesn't leave guest profiles alone, the push deactivates them.
4. New employees are sent without a travel manager: if managers are required, they fail in the import log (the
   others are imported).
5. Personnel numbers in L&P are overwritten by the ones of ProjectForge (if set in ProjectForge).

## Open questions

To the L&P support:

- Are guest profiles (role `guest`) deactivated by the user import, if they are missing in the payload? (Only to
  know the effect: guests won't be sent.)
- Are fields that are not sent at all (`roles`, `managers_emails`, `group_ids`, `delegation`,
  `company_creditor_account`) really kept? Especially: do users keep their role, or is a default role set?
- Are managers required by the company settings, i.e. do new users fail without `managers_emails`?
- May the service admin of L&P (role `admin`, e-mail at lanes-planes.com) be sent as additional user, or is it
  ignored by the import anyway? It is the travel manager of other users, so missing it would let the push fail.
- Can the cost centers no longer assigned after the first push be deleted from the company-wide list safely?
- Can an existing user be matched by `ident`, so that a changed e-mail address can be updated by the import?
  Otherwise the user is deactivated and created anew; change the e-mail by hand in L&P before the push.
- Is there a test environment or a way to validate a payload without importing it?

To decide in ProjectForge:

- Travel managers: derive `managers_emails` from ProjectForge (e.g. a configured manager per department or
  group), if they are required for new users.
- General Kost1/Kost2 and patterns after reviewing the number of cost centers and cost units per user in the dry
  run.

## Dry run findings

- A dry run against an anonymized database (all users with the same e-mail address) sends a single user: the
  others are skipped as duplicate e-mails. A meaningful comparison with the L&P export needs the real e-mail
  addresses. Skipped users would be deactivated in L&P by a real push, so the warnings in the log matter.
- Without a filter, a user got well over a hundred cost units of many Kost2 types: hence the patterns and the
  general list instead of all bookable Kost2.
- The first run starts `initialDelayMs` (default 5 minutes) after the start; for a quick dry run set e.g.
  `initialDelayMs=30000`.

## Before the first real push

1. Set the invoice profile ids (from the CSV export), the general Kost1 and Kost2 and the patterns in the
   configuration parameter, set the creditor accounts of the employees, and run a dry run.
2. Compare the dry-run file with a fresh CSV export of the L&P users (by e-mail, case-insensitive):
   - active in L&P but missing in ProjectForge → would be deactivated; if one of them has responsibilities, the
     whole push fails,
   - inactive in L&P but sent → would be reactivated,
   - sent but unknown in L&P → new (without travel manager),
   - differing personnel numbers, creditor accounts and names,
   - number of cost centers and cost units per user.
3. Make sure the L&P account owning the API key is part of the payload, and that every active L&P user with
   responsibilities who isn't an employee (accountants, admins, travel managers) is entered as additional user.
4. Set `dryRun=false`, restart, check the user import log in the L&P company account (including whether roles,
   managers and cost centers were kept).
