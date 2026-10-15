import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { writeHeaders, MARKER } from "./fixtures/seed";
import {
  createTeamCal,
  icsFile,
  removeTeamCal,
  type IcsEvent,
  type SeededTeamCal,
} from "./fixtures/teamcal";

/**
 * The ICS import of team events (`/next/teamCalImport`, `TeamEventImportRest` + components/shared/import)
 * against the live backend: reached from the calendar's more menu, an ics file with two events goes into
 * a throwaway calendar of the test's own; uploaded again with one subject changed, the reconcile by uid
 * finds one MODIFIED and one UNMODIFIED event — proof the commit stored both under their uids. The
 * attendees of the first event are stored with it and shown, read-only, in its editor.
 */

// A live upload, a reconcile and a background commit job, and the dev server compiles each route once.
test.describe.configure({ timeout: 120_000 });

interface ImportStateView {
  meta?: { teamCalId?: number };
  entries: { status: string; read?: { subject?: string } }[];
}

async function importState(page: Page): Promise<ImportStateView> {
  const res = await page.request.get("/rs/teamCalImport/state", {
    headers: { "X-PF-Frontend": "next" },
  });
  return (await res.json()) as ImportStateView;
}

/** The id of the imported event [subject] in [calendarId], found via the calendar's event feed. */
async function importedEventId(
  page: Page,
  calendarId: number,
  subject: string
): Promise<number> {
  const res = await page.request.post("/rs/calendar/events", {
    headers: await writeHeaders(page.request),
    data: {
      start: "2099-11-30T00:00:00.000Z",
      end: "2099-12-04T00:00:00.000Z",
      activeCalendarIds: [calendarId],
      timeZone: "UTC",
    },
  });
  const { events } = (await res.json()) as {
    events?: { title?: string; extendedProps?: { dbId?: number } }[];
  };
  const id = events?.find((e) => e.title === subject)?.extendedProps?.dbId;
  expect(id, `event "${subject}" in calendar ${calendarId}`).toBeDefined();
  return id!;
}

function statuses(view: ImportStateView): string[] {
  return view.entries.map((entry) => entry.status).sort();
}

test.describe("team calendar ics import", { tag: "@lane-calendar" }, () => {
  test("imports an ics file and reconciles it by uid", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let calendar: SeededTeamCal | undefined;
    try {
      calendar = await createTeamCal(page.request);
      const events: IcsEvent[] = [
        {
          uid: `zz-e2e-${calendar.suffix}-1@projectforge`,
          subject: `${MARKER} ics one ${calendar.suffix}`,
          date: "20991201",
          attendees: [
            {
              name: `Jane ${calendar.suffix}`,
              email: `jane-${calendar.suffix}@example.org`,
              partStat: "ACCEPTED",
            },
          ],
        },
        {
          uid: `zz-e2e-${calendar.suffix}-2@projectforge`,
          subject: `${MARKER} ics two ${calendar.suffix}`,
          date: "20991202",
          allDay: true,
        },
      ];

      // The way a user gets there: the calendar's more menu.
      await goto(page, "/calendar");
      await page
        .getByRole("button", { name: format.t("more"), exact: true })
        .click();
      await page
        .getByRole("menuitem", {
          name: format.t("plugins.teamcal.import.ics.title"),
        })
        .click();
      await expect(page).toHaveURL(/\/teamCalImport/);
      await expect(
        page.getByRole("heading", {
          name: format.t("plugins.teamcal.import.ics.title"),
        })
      ).toBeVisible({ timeout: 60_000 });

      // Pick the test's calendar, then drop the file.
      await page
        .getByRole("combobox", {
          name: label(format, "plugins.teamcal.calendar"),
        })
        .click();
      await page.getByRole("option", { name: calendar.title }).click();
      const uploaded = page.waitForResponse(
        (r) => r.url().includes("/rs/teamCalImport/upload") && r.ok()
      );
      await page.locator('input[type="file"]').setInputFiles(icsFile(events));
      const firstView = (await (await uploaded).json()) as ImportStateView;
      expect(firstView.meta?.teamCalId).toBe(calendar.id);
      expect(statuses(firstView)).toEqual(["NEW", "NEW"]);
      await expect(page.getByText(events[0].subject)).toBeVisible();

      // Commit both; the page stays and reloads the preview once the job is over.
      await page
        .getByRole("button", {
          name: format.t("common.import.action.selectAll"),
        })
        .click();
      const committed = page.waitForResponse(
        (r) => r.url().includes("/rs/teamCalImport/commit") && r.ok()
      );
      await page
        .getByRole("button", { name: format.t("common.import.action.commit") })
        .click();
      await committed;
      await expect
        .poll(async () => statuses(await importState(page)), {
          timeout: 30_000,
        })
        .toEqual(["UNMODIFIED", "UNMODIFIED"]);

      // The attendee is shown in the event's editor, with its status.
      const eventId = await importedEventId(
        page,
        calendar.id,
        events[0].subject
      );
      await goto(page, `/teamEvent/${eventId}`);
      const attendee = events[0].attendees![0];
      // The section's anchor tab, which exists only while the section does — its title alone is
      // ambiguous, the section header repeats it.
      await expect(
        page.getByRole("tab", { name: format.t("plugins.teamcal.attendees") })
      ).toBeVisible({ timeout: 60_000 });
      await expect(page.getByText(attendee.name)).toBeVisible();
      await expect(page.getByText(attendee.email)).toBeVisible();
      await expect(
        page.getByText(format.t("plugins.teamcal.attendee.status.accepted"), {
          exact: true,
        })
      ).toBeVisible();
      // Back with the calendar preselected, as the calendar page links it: the choice lives in the
      // page's state only, and the second upload below must target the same calendar again.
      await goto(page, `/teamCalImport?teamCalId=${calendar.id}`);

      // Again with the first subject changed: matched by uid, so MODIFIED rather than NEW.
      await page
        .getByRole("button", { name: format.t("common.import.clearStorage") })
        .click();
      const changed = { ...events[0], subject: `${events[0].subject} changed` };
      const reuploaded = page.waitForResponse(
        (r) => r.url().includes("/rs/teamCalImport/upload") && r.ok()
      );
      await page
        .locator('input[type="file"]')
        .setInputFiles(icsFile([changed, events[1]]));
      const secondView = (await (await reuploaded).json()) as ImportStateView;
      expect(statuses(secondView)).toEqual(["MODIFIED", "UNMODIFIED"]);
    } finally {
      await page.request.post("/rs/teamCalImport/cancel", {
        headers: await writeHeaders(page.request),
      });
      if (calendar) await removeTeamCal(page.request, calendar.id);
    }
  });
});
