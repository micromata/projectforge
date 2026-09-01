import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, label } from "./fixtures/format";
import { markAsDeleted, MARKER, uniqueSuffix } from "./fixtures/seed";
import { CALENDAR_GRID_SIZES } from "../lib/rs/calendar-types";
import type {
  CalendarInit,
  StyledTeamCalendar,
} from "../lib/rs/calendar-types";

/**
 * The hand-built calendar (`/next/calendar`, the default page after login), against the live backend.
 * The cases below cover the eight regressions from MIGRATION-calendar.md: it loads and honours a
 * `?gotoDate`; the view, grid size and calendar visibility survive a reload (they are persisted server
 * side); an event's hover-card shows its data; a filter favourite can be created and deleted; and the
 * two navigation paths a click produces resolve to the migrated timesheet routes.
 *
 * Nothing is hard coded to a locale: labels come through `userFormat(page).t` (the same catalogs the
 * app renders from) and the only literals asserted are the tests' own data (the `MARKER`) or
 * locale-independent tokens (a year as digits, a FullCalendar CSS class, a route).
 *
 * A live backend, and the first navigation to a route additionally waits for the dev server to compile
 * it — hence the raised timeout.
 */
test.describe.configure({ timeout: 120_000 });

/** All eight view keys the header offers; a button carries the class `fc-<key>-button`. */
const VIEW_KEYS = [
  "dayGridMonth",
  "dayGridWorkingMonth",
  "timeGridWeek",
  "timeGridWorkingWeek",
  "timeGridDay",
  "dayGridWeek",
  "listWeek",
  "listMonth",
] as const;

/** Under this name the favourite case saves, so the sweep afterwards can tell it from the account's own. */
const FAVORITE_NAME = `${MARKER} calendar favorite`;

test.describe("calendar", () => {
  test.afterEach(async ({ loggedInPage: page }) => {
    await dropOwnFavorites(page);
  });

  test("loads, renders the grid and honours a gotoDate", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/calendar");
    await expect(page.locator(".pf-calendar")).toBeVisible();
    await expect(page.locator(".fc-toolbar-title")).not.toBeEmpty();

    // A date well outside any range the page opens at: useGotoDate then moves the calendar to it, so
    // its year (locale-independent digits) shows in the title.
    const future = new Date();
    future.setFullYear(future.getFullYear() + 3);
    const year = String(future.getFullYear());
    await goto(page, `/calendar?gotoDate=${year}-06-15`);
    await expect(page.locator(".fc-toolbar-title")).toContainText(year);
  });

  test("remembers the view across a reload", async ({ loggedInPage: page }) => {
    await goto(page, "/calendar");
    await expect(page.locator(".pf-calendar")).toBeVisible();

    const current = await activeViewKey(page);
    // Switch to a view that is not the current one, so the assertion cannot pass by accident.
    const target = current === "timeGridDay" ? "dayGridMonth" : "timeGridDay";
    await page.locator(`.fc-${target}-button`).click();
    await expect(page.locator(`.fc-${target}-button`)).toHaveClass(
      /fc-button-active/
    );

    // storeState is debounced (300 ms); give it room to reach the backend before the reload.
    await page.waitForTimeout(700);
    await page.reload();
    await expect(page.locator(`.fc-${target}-button`)).toHaveClass(
      /fc-button-active/
    );
  });

  test("remembers a changed grid size across a reload", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/calendar");
    const panel = page.locator(".pf-calendar");
    await expect(panel).toBeVisible();

    const original = Number(await panel.getAttribute("data-grid-size"));
    const next = CALENDAR_GRID_SIZES.find((size) => size !== original)!;

    const format = await userFormat(page);
    await page
      .getByRole("button", { name: format.t("calendar.view.settings.tooltip") })
      .click();
    const dialog = page.getByRole("dialog");
    // The dialog holds several Selects and a Radix trigger takes its accessible name from neither its
    // label nor its value, so target the grid-size one through its own label: the trigger is the button
    // that follows the "grid size" label inside the same field wrapper.
    await dialog
      .getByText(format.t("calendar.option.gridSize"), { exact: true })
      .locator("xpath=following-sibling::button")
      .click();
    // Grid-size options are bare numbers; the first-hour options are "HH:00", so the exact match picks
    // the grid-size one even when the number would otherwise be a prefix of a time.
    await page.getByRole("option", { name: String(next), exact: true }).click();

    await expect(panel).toHaveAttribute("data-grid-size", String(next));
    await page.keyboard.press("Escape");

    await page.reload();
    await expect(page.locator(".pf-calendar")).toHaveAttribute(
      "data-grid-size",
      String(next)
    );

    // Leave the account's setting as it was found.
    await calendarGet(page, `changeGridSize?size=${original}`);
  });

  test("remembers a hidden calendar across a reload", async ({
    loggedInPage: page,
  }) => {
    const init = await readInit(page);
    const calendar = (init.activeCalendars ?? []).find(
      (c): c is StyledTeamCalendar & { id: number } => c.id != null
    );
    test.skip(!calendar, "The account has no active calendar to hide.");
    const { id, title, visible } = calendar!;

    const format = await userFormat(page);
    await goto(page, "/calendar");
    const pill = page.getByRole("button", { name: title ?? "", exact: true });
    await expect(pill).toBeVisible();

    await pill.click();
    const toggle = page.getByLabel(format.t("calendar.filter.visible"));
    // End up hidden regardless of where it started, so the strike-through is what we assert.
    if (visible) await toggle.click();
    else {
      await toggle.click(); // show
      await toggle.click(); // hide again
    }
    await page.keyboard.press("Escape");
    await expect(pill).toHaveClass(/line-through/);

    await page.reload();
    await expect(
      page.getByRole("button", { name: title ?? "", exact: true })
    ).toHaveClass(/line-through/);

    // Restore the calendar's original visibility.
    await calendarGet(
      page,
      `setVisibility?calendarId=${id}&visible=${visible}`
    );
  });

  test("creates and deletes a filter favourite", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/calendar");
    await expect(page.locator(".pf-calendar")).toBeVisible();

    await page
      .getByRole("button", { name: label(format, "favorites") })
      .click();
    await page
      .getByPlaceholder(format.t("favorite.addNew"))
      .fill(FAVORITE_NAME);
    await page
      .getByRole("button", { name: format.t("favorite.addNew") })
      .click();

    const entry = page.getByRole("button", {
      name: FAVORITE_NAME,
      exact: true,
    });
    await expect(entry).toBeVisible();

    await page
      .getByRole("button", { name: `${format.t("delete")}: ${FAVORITE_NAME}` })
      .click();
    await expect(entry).toHaveCount(0);
  });

  test("opens a timesheet event's tooltip on hover and its edit page on click", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    // The heaviest case of the file: it seeds a timesheet and then compiles two routes on demand
    // (`/calendar` and, on the click, the nested `/calendar/[...edit]`), on top of the up-to-15s
    // tooltip retry below. Late in a serial full run against a loaded dev server that does not fit
    // the describe's 120s, so this one case is given the same 180s ceiling the task tree's are.
    test.setTimeout(180_000);
    const userId = await ownUserId(page);
    // The account may not show its own timesheets by default; make sure it does for this case.
    const init = await readInit(page);
    const originalTimesheetUser = init.filter?.timesheetUserId ?? null;
    await calendarGet(page, `changeTimesheetUser?userId=${userId}`);

    const timesheet = await seedTimesheet(page, seededTask.child.id, userId);
    try {
      await goto(page, "/calendar");
      await expect(page.locator(".pf-calendar")).toBeVisible();
      // Today, in the time grid; the event is found by its unique description, so leftover timesheets
      // from an earlier run (which cannot always be cleaned up) cannot be picked by mistake.
      await page.locator(".fc-timeGridDay-button").click();
      await page.locator(".fc-today-button").click();

      const event = page.locator(".fc-timegrid-event", {
        hasText: timesheet.description,
      });
      await expect(event).toBeVisible();

      // Hover → the cursor-pinned tooltip shows the event's data; its task path carries the MARKER. It is
      // a custom `role="tooltip"` portal now (see [CalendarEventTooltip]), not the old shadcn hover-card.
      // Re-enter each try: the card opens 200ms after the pointer enters, and any document scroll in that
      // window cancels it (CalendarEventContent closes on scroll), so a grid still settling can swallow a
      // single hover. Leaving and re-entering reschedules the open until one lands on a quiet grid.
      const card = page.getByRole("tooltip");
      await expect(async () => {
        await page.mouse.move(0, 0);
        await event.hover();
        await expect(card).toBeVisible({ timeout: 1_500 });
      }).toPass({ timeout: 15_000 });
      await expect(card).toContainText(MARKER);

      // Click → the timesheet edits in place as a nested route of the calendar (`/calendar/timesheet/<id>`,
      // see useCalendarAction/[...edit]), so save and cancel come back to the still-mounted calendar.
      await event.click();
      await expect(page).toHaveURL(
        new RegExp(`/next/calendar/timesheet/${timesheet.id}(\\?|$)`)
      );
    } finally {
      await markAsDeleted(page.request, "timesheet", timesheet.id);
      await calendarGet(
        page,
        originalTimesheetUser != null
          ? `changeTimesheetUser?userId=${originalTimesheetUser}`
          : `changeTimesheetUser?userId=-1`
      );
    }
  });

  test("opens a prefilled edit route from the create button", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/calendar");
    await expect(page.locator(".pf-calendar")).toBeVisible();

    // The create button and an empty-slot select share the /action endpoint; the button is the
    // deterministic way to reach the nested add route with a preset start time (new-route regression).
    // It is the shared AddEntryButton in the page header now, not a FullCalendar toolbar button (see
    // CalendarPage / view-config.ts) — reached by its accessible name.
    await page
      .getByRole("button", { name: format.t("menu.addNewEntry") })
      .click();
    await expect(page).toHaveURL(
      /\/next\/calendar\/(timesheet|teamEvent)\/new\?.*startDate=/
    );
  });
});

/** The current page state, read straight from the backend (`GET /rs/calendar/initial`). */
async function readInit(page: Page): Promise<CalendarInit> {
  const res = await page.request.get("/rs/calendar/initial", {
    headers: { "X-PF-Frontend": "next" },
  });
  return (await res.json()) as CalendarInit;
}

/** Fires one of the `change*` / `setVisibility` GET endpoints — used to set up and to restore state. */
async function calendarGet(page: Page, pathAndQuery: string): Promise<void> {
  await page.request.get(`/rs/calendar/${pathAndQuery}`, {
    headers: { "X-PF-Frontend": "next" },
  });
}

/** The logged-in account's own user id, for a timesheet booked in its name. */
async function ownUserId(page: Page): Promise<number> {
  const res = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { userData } = (await res.json()) as { userData: { userId: number } };
  return userData.userId;
}

/**
 * Books a half-hour timesheet on the given task, today around noon, and answers with its database id
 * and the description it was given.
 *
 * The minute is a multiple of five (the backend requires 00, 05, …, 50) derived from the run's suffix,
 * so two runs do not book the exact same span; the description carries the same suffix, so the event is
 * identifiable among any left over from an earlier run.
 *
 * Unlike a plain entity save, the timesheet save answers with a redirect back to the calendar rather
 * than the new id, so `insert`'s `variables.id` is absent — the id is looked up from the calendar feed,
 * where the event exposes it as `extendedProps.dbId`.
 */
async function seedTimesheet(
  page: Page,
  taskId: number,
  userId: number
): Promise<{ id: number; description: string }> {
  const status = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken, userData } = (await status.json()) as {
    csrfToken: string;
    userData: { timeZone?: string };
  };
  const writeHeaders = {
    "X-PF-Frontend": "next",
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };

  const now = new Date();
  const dayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const dayEnd = new Date(dayStart.getTime() + 24 * 60 * 60 * 1000);

  // The backend rejects a timesheet that overlaps another of the same user, so a leftover from an
  // earlier crashed run would block today's slot forever. Sweep our own markers for the day first —
  // runs are serial (workers: 1), so anything matching MARKER is stale debris and safe to remove.
  const todaysTimesheets = async () => {
    const res = await page.request.post("/rs/calendar/events", {
      headers: writeHeaders,
      data: {
        start: dayStart.toISOString(),
        end: dayEnd.toISOString(),
        timesheetUserId: userId,
        activeCalendarIds: [],
        timeZone: userData.timeZone,
      },
    });
    const { events } = (await res.json()) as {
      events?: { description?: string; extendedProps?: { dbId?: number } }[];
    };
    return events ?? [];
  };

  for (const stale of await todaysTimesheets()) {
    if (stale.description?.startsWith(`${MARKER} timesheet`)) {
      const dbId = stale.extendedProps?.dbId;
      if (dbId != null) await markAsDeleted(page.request, "timesheet", dbId);
    }
  }

  const minute = (parseInt(uniqueSuffix(), 36) % 11) * 5;
  const start = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate(),
    12,
    minute
  );
  const stop = new Date(start.getTime() + 30 * 60 * 1000);
  const description = `${MARKER} timesheet ${uniqueSuffix()}-${minute}`;

  const save = await page.request.put("/rs/timesheet/saveorupdate", {
    headers: writeHeaders,
    data: {
      data: {
        task: { id: taskId },
        user: { id: userId },
        description,
        startTime: start.toISOString(),
        stopTime: stop.toISOString(),
      },
    },
  });
  const saveBody = (await save.json()) as {
    validationErrors?: { message?: string }[];
  };
  if (!save.ok() || saveBody.validationErrors) {
    const reason =
      saveBody.validationErrors?.map((e) => e.message).join("; ") ??
      `HTTP ${save.status()}`;
    throw new Error(`Could not create a timesheet for the test: ${reason}`);
  }

  const event = (await todaysTimesheets()).find(
    (e) => e.description === description
  );
  if (event?.extendedProps?.dbId == null) {
    throw new Error(
      `The seeded timesheet "${description}" did not turn up in the calendar feed.`
    );
  }
  return { id: event.extendedProps.dbId, description };
}

/** Which view button carries FullCalendar's active class, or null before the grid has mounted. */
async function activeViewKey(page: Page): Promise<string | null> {
  for (const key of VIEW_KEYS) {
    const button = page.locator(`.fc-${key}-button`);
    if (
      (await button.count()) > 0 &&
      (await button
        .first()
        .evaluate((el) => el.classList.contains("fc-button-active")))
    ) {
      return key;
    }
  }
  return null;
}

/** Removes the favourite this file's case saves, so a run cannot leave one behind. */
async function dropOwnFavorites(page: Page): Promise<void> {
  const init = await readInit(page).catch(() => null);
  for (const favorite of init?.filterFavorites ?? []) {
    if (favorite.name !== FAVORITE_NAME) continue;
    await calendarGet(page, `deleteFilter?id=${favorite.id}`);
  }
}
