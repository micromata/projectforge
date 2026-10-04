import type { APIRequestContext } from "@playwright/test";
import { insert, markAsDeleted, MARKER, uniqueSuffix } from "./seed";

/**
 * A team calendar of the tests' own, owned by the logged-in user, and the ics files the import spec
 * uploads into it.
 *
 * A fresh calendar per run keeps the import's reconcile (by uid within the target calendar) away from
 * every real event, and its deletion hides whatever the import put into it: the events of a deleted
 * calendar are shown nowhere.
 */
export interface SeededTeamCal {
  id: number;
  title: string;
  suffix: string;
}

/**
 * Inserts the calendar with the logged-in user as owner. The owner has to be given: without one the
 * calendar is not among the user's writable calendars (`TeamCalDao.writableCalendars`).
 */
export async function createTeamCal(
  request: APIRequestContext,
  suffix = uniqueSuffix()
): Promise<SeededTeamCal> {
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { userData } = (await status.json()) as {
    userData: { userId: number };
  };
  const title = `${MARKER} calendar ${suffix}`;
  const id = await insert(request, "teamCal", {
    title,
    owner: { id: userData.userId },
  });
  return { id, title, suffix };
}

export async function removeTeamCal(
  request: APIRequestContext,
  id: number
): Promise<void> {
  await markAsDeleted(request, "teamCal", id);
}

/** The day after [date] (yyyyMMdd): the exclusive DTEND of a one-day all-day event. */
function nextDay(date: string): string {
  const day = new Date(
    Date.UTC(+date.slice(0, 4), +date.slice(4, 6) - 1, +date.slice(6, 8) + 1)
  );
  return day.toISOString().slice(0, 10).replaceAll("-", "");
}

/** One event of an ics file: a timed event, or an all-day one on [date] (yyyyMMdd). */
export interface IcsEvent {
  uid: string;
  subject: string;
  date: string;
  allDay?: boolean;
}

/**
 * An ics file of [events], dated far in the future so they never show up in anybody's current calendar
 * view, as a Playwright file payload for `setInputFiles` on the drop area's hidden input.
 */
export function icsFile(events: IcsEvent[]) {
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//e2e//teamcal-import//EN",
  ];
  for (const event of events) {
    lines.push(
      "BEGIN:VEVENT",
      `UID:${event.uid}`,
      "DTSTAMP:20260101T000000Z",
      ...(event.allDay
        ? [
            `DTSTART;VALUE=DATE:${event.date}`,
            `DTEND;VALUE=DATE:${nextDay(event.date)}`,
          ]
        : [`DTSTART:${event.date}T090000Z`, `DTEND:${event.date}T100000Z`]),
      `SUMMARY:${event.subject}`,
      "END:VEVENT"
    );
  }
  lines.push("END:VCALENDAR");
  return {
    name: "events.ics",
    mimeType: "text/calendar",
    buffer: Buffer.from(lines.join("\r\n"), "utf-8"),
  };
}
