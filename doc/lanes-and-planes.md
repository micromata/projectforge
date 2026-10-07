# Lanes & Planes interface

ProjectForge pushes the active employees with the cost units they may book on to the travel management tool
Lanes & Planes (L&P). This file collects the findings about the L&P API and the current state.

**No confidential data in this file:** no names of users or companies, no e-mail addresses, ids (invoice profiles,
users), cost center names or numbers of a real installation. Describe findings generically.

## Status

- Implemented: package `org.projectforge.business.lanesandplanes` (projectforge-business), disabled by default.
- Tested: unit tests (bookable Kost2 per user, JSON format, gzip). Not yet tested: a dry run against a real
  database and a real push to L&P.
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

## What ProjectForge sends

| L&P field | Source |
|---|---|
| `ident` | `PFUserDO.id` |
| `first_name`, `last_name`, `email` | `PFUserDO`; employees without one of them are skipped (warning in the log), as are duplicate e-mails (case-insensitive) |
| `personnel_number` | `EmployeeDO.staffNumber` (not sent if empty) |
| `reference_cost_center` | Kost1 of the employee (formatted number), not sent if empty |
| `cost_units` | The bookable Kost2, `ident` = formatted number (`#.###.##.##`), `name` = `KostFormatter` TEXT format |
| `accounting_invoice_profile_ids` | Configuration, the same for all users |

Not sent at all (so kept as maintained in L&P, if the assumption above holds): `roles`, `group_ids`,
`managers_emails`, `delegation`, `abbreviation`, `creditor_account`, `company_creditor_account`, `cost_centers`,
`middle_name`.

Users: all active employees (`EmployeeService.selectAllActive`) whose user isn't deactivated or deleted. Users
without any bookable Kost2 are sent nevertheless (otherwise they would be deactivated).

Bookable Kost2 of a user: the active Kost2 (`effectiveKostentraegerStatus` active, not deleted) of all tasks,
- for which one of the user's groups has the right to insert own time sheets (`AccessType.OWN_TIMESHEETS`),
- which are bookable (`TimesheetDao.checkTaskBookable(taskNode, …)`: not closed/deleted, no `TREE_CLOSED`/
  `NO_BOOKING`, `ONLY_LEAFS` respected),
- with the Kost2 of the task (`TaskTree.getKost2List`, including the black/white list of the task),
- optionally restricted to the configured Kost2Art ids.

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

In `projectforge.properties` (defaults in `projectforge-business/src/main/resources/application.properties`, read
on start only):

```properties
projectforge.lanesandplanes.enabled=true
projectforge.lanesandplanes.apiKey=<API key of L&P>
# Required, comma separated, provided by L&P (also needed for the dry run):
projectforge.lanesandplanes.accountingInvoiceProfileIds=<id>
# Optional, only Kost2 of these types (comma separated). Empty: all types.
projectforge.lanesandplanes.kost2ArtIds=
projectforge.lanesandplanes.dryRun=true
projectforge.lanesandplanes.minUsers=1
projectforge.lanesandplanes.syncIntervalMs=3600000
projectforge.lanesandplanes.initialDelayMs=300000
```

The id lists are bound as `List<Long>`: a non-numeric value (e.g. a copied placeholder like `…`) stops the start of
ProjectForge with a `ConfigurationPropertiesBindException`.

## Findings from an L&P user export (CSV)

The CSV download of the company account (separator `;`) has the columns `user_id`, `email`, `first_name`,
`middle_name`, `last_name`, `personnel_number`, `abbreviation`, `creditor_account`, `company_creditor_account`,
`reference_cost_center`, `role`, `booking_status`, `travel_managers`, `managed_users`, `cost_centers`,
`invoice_profiles`.

- `booking_status` looks like `active: true, signup_completion: true, invoice_profiles: true, cost_centers: true`.
- `invoice_profiles` (`ID: <id> Name: <name>`) gives the id needed for `accountingInvoiceProfileIds`. In the
  analyzed export all users had the same single profile.
- **There is no column for cost units.** The existing assignments are all cost centers: the same list for every
  user, mostly cost types (8 digits without dots, the number followed by the name in one string, e.g.
  `<number> Flight`), plus a few entries in the format of a Kost2 without dots (`5xxxxxxx <name>`).
- Lists in the CSV are comma separated, but names may contain commas: the CSV can't be split reliably.
- `reference_cost_center` was empty for all users, `creditor_account` and `personnel_number` were filled for most.
- Nearly all users have a travel manager (`travel_managers`): managers are probably required by the company
  settings.
- Role `guest`: guest profiles created by L&P (e-mail `tickets-guest-<number>@lanes-planes.com`), partly generic
  placeholders, partly named after real persons who also have their regular L&P account. `guest` isn't one of
  the roles of the import documentation.

Consequences:

1. ProjectForge sends the Kost2 as `cost_units`, a field that isn't used in L&P yet. The existing cost center list
   isn't touched (not sent).
2. The `ident` format differs from the L&P convention (number without dots plus name). If a Kost2 already exists
   in L&P (e.g. as a cost center), it must be sent exactly with the L&P name, otherwise it's created a second time.
3. Guest profiles aren't employees in ProjectForge and are deliberately not sent (decided: guests are out of
   scope). If L&P doesn't leave guest profiles alone, the push deactivates them.
4. New employees are sent without a travel manager: if managers are required, they fail in the import log (the
   others are imported).
5. Personnel numbers in L&P are overwritten by the ones of ProjectForge (if set in ProjectForge).

## Open questions

To the L&P support:

- Are guest profiles (role `guest`) deactivated by the user import, if they are missing in the payload? (Only to
  know the effect: guests won't be sent.)
- Are fields that are not sent at all (e.g. `roles`, `managers_emails`, `cost_centers`) really kept?
- Cost units vs. cost centers: which one is shown/selectable when booking, and which one appears in the invoice
  data? Should the Kost2 rather be cost centers?
- Are managers required by the company settings, i.e. do new users fail without `managers_emails`?

To decide in ProjectForge:

- Target field (`cost_units` or `cost_centers`) and `ident` format (formatted `#.###.##.##` or digits only like
  the L&P convention, optionally with the name).
- Travel managers: derive `managers_emails` from ProjectForge (e.g. a configured manager per department or
  group), if they are required for new users.
- Kost2Art filter (`kost2ArtIds`) after reviewing the number of cost units per user in the dry run.

## Before the first real push

1. Set `accountingInvoiceProfileIds` (from the CSV export) and run a dry run.
2. Compare the dry-run file with a fresh CSV export of the L&P users (by e-mail, case-insensitive):
   - active in L&P but missing in ProjectForge → would be deactivated; if one of them has responsibilities, the
     whole push fails,
   - inactive in L&P but sent → would be reactivated,
   - sent but unknown in L&P → new (without travel manager),
   - differing personnel numbers and names,
   - number of cost units per user.
3. Make sure the L&P account owning the API key is part of the payload.
4. Set `dryRun=false`, restart, check the user import log in the L&P company account (including whether roles,
   managers and cost centers were kept).
