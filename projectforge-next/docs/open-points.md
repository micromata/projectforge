# Open points

The migration to projectforge-next is complete: every page is served here, Wicket, the React app and the
backend UILayout are removed. This is the one list of what is left. Remove an entry once it is done.

## Tests

- **E2E specs for refusals** that so far are only covered as successes (the roles exist, see
  `../CLAUDE.md`, "Testing against the running system"):
  - 403 of the e-invoice checker for a user without FIBU/ORGA or order book right;
  - `useReadAccessGuard` (a 403 on reading an entity);
  - the `rejected` write path of `postEntityAction`;
  - the English locale (`normalo-user` has `locale=en`);
  - the counterpart of `book-list-gear.spec.ts`: the full reindex only as admin;
  - renaming and deleting a filter favorite (applying, saving and the modified marker are covered).
- **Manual checks, need real hardware or a real mailbox:**
  - second factor with a real token: WebAuthn, OTP/SMS/mail after login, the reset-mail link,
    "stay logged in", the in-session 2FA dialog;
  - a click-through of the calendar (all views with persistence, grid size, first hour, drag/resize,
    favorites, vacations, dark mode, mobile). `e2e/calendar.spec.ts` covers the regressions.

## List paging (performance, see [list-paging.md](list-paging.md))

- **Column filters on the server.** An operator on `MagicFilterEntry` (`DBPredicate` already covers all
  operators the column filters offer), a `distinctValues` endpoint for the selection mode, and the column
  funnel writing into the same filter state as the pills — then a column filter narrows the whole result,
  shows as a pill and survives in a favorite.
- **Faster first page:** `SELECT id` projection in the id pass (criteria mode only), and a real
  `OFFSET/LIMIT` + `COUNT` fast path when the user's row access is uniform (e.g. finance users on orders)
  and no Kotlin-side filters apply.
- Remove the unused `limitResultSize` of `QueryFilter`/`DBFilter`.
- Roll out to further lists only when they grow large enough to need it.

## Features

- **Vacation: half day on a non-working day.** If the first or last day of a vacation is marked as a half
  day but isn't a working day, `VacationService.getVacationDays` silently ignores the flag; the form
  should point that out.

## Deferred on purpose

- **Calendar event attendees** — not editable in the event form; the schema carries existing attendees
  through unchanged (`z.array(z.unknown())`), so none are lost on save.
- **A list page for calendar events** (`teamEvent`) — events are reached via the calendar; the columns
  are already declared in `teamEvent.page.tsx`, only the route is missing.
- **`calendar.useNewCalendarEvents`** — with the flag set, the calendar's `action` answers URLs to
  `calEvent`, for which there is no page. Look at this before switching the flag (currently `false`).
- **`NEXT_APP_PATH`** stays `/next` (decided).
