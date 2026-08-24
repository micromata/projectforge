/**
 * How the two ends of a time sheet follow each other while one of them is edited — pure, so the rules
 * are testable without a form.
 *
 * A time sheet is one span of one working day, and that is the whole reason this exists: the two boxes
 * hold *instants*, but what a user changes is a time of day, and moving the start must not silently
 * leave the stop behind on the day before. Wicket's `TimesheetEditForm` keeps the same invariants
 * (`setStopDate`/`setStartDate` adjust the other end); here they are one function per direction.
 *
 * Everything is ISO 8601 in UTC, the wire format both ends travel in (see DateTimeInput). Arithmetic on
 * instants only — no zone is applied and none is needed: a duration is the same number of minutes in
 * every zone, and the *display* in the user's zone is the input's business.
 */

const MINUTE = 60_000;
const DAY_MINUTES = 24 * 60;

/** Default length of a sheet whose stop is not known yet — one hour, as the legacy form assumed. */
export const DEFAULT_DURATION_MINUTES = 60;

/** Minutes between the two ends, or null while either is missing. Negative if they are inverted. */
export function durationMinutesOf(
  startTime: string | null,
  stopTime: string | null
): number | null {
  if (!startTime || !stopTime) return null;
  const start = Date.parse(startTime);
  const stop = Date.parse(stopTime);
  if (!Number.isFinite(start) || !Number.isFinite(stop)) return null;
  return Math.round((stop - start) / MINUTE);
}

function shift(iso: string, minutes: number): string {
  return new Date(Date.parse(iso) + minutes * MINUTE).toISOString();
}

/**
 * The stop that goes with a new start: the sheet keeps the length it had.
 *
 * Keeping the length rather than the stop is what a user means by moving a sheet — they are correcting
 * when the work began, not shortening it. A sheet that had no valid length yet (no stop, or a stop
 * before its start) gets [DEFAULT_DURATION_MINUTES], which is also what an empty form starts from.
 */
export function stopTimeForNewStart(
  startTime: string | null,
  previous: { startTime: string | null; stopTime: string | null }
): string | null {
  if (!startTime) return previous.stopTime;
  const held = durationMinutesOf(previous.startTime, previous.stopTime);
  const minutes = held != null && held > 0 ? held : DEFAULT_DURATION_MINUTES;
  return shift(startTime, minutes);
}

/**
 * The stop that goes with what the user typed into the stop box — the same instant, or a day later.
 *
 * The box says a *time of day*, and 08:00–00:30 is a sheet that ends after midnight, not one that ends
 * eight and a half hours before it began. So a stop that lands at or before the start is read as the
 * next day's, which is what Wicket does with the same two boxes. Only ever by one day: a longer sheet
 * is a date the user has to state, and the backend refuses it anyway
 * (`timesheet.error.maximumDurationExceeded`).
 */
export function normalizedStopTime(
  stopTime: string | null,
  startTime: string | null
): string | null {
  if (!stopTime || !startTime) return stopTime;
  const duration = durationMinutesOf(startTime, stopTime);
  if (duration == null || duration > 0) return stopTime;
  return shift(stopTime, DAY_MINUTES);
}
