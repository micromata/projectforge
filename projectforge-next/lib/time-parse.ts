import type { FormatContext } from "./format";

/**
 * Reading and writing a time of day the user typed — the sibling of ./date-parse.ts.
 *
 * A time lives here as `HH:mm` on a 24h clock, which is what `<input type="time">` used to hold and
 * what lib/user-zone.ts converts into an instant. Only the text shown in an input is localized.
 *
 * Whether that text carries AM/PM is `ctx.hour12`, i.e. the account's `timeNotation` setting — not
 * the locale's habit and not the platform's. A native time input cannot honour it: its presentation
 * comes from the operating system, so an English machine would show "2:30 PM" to a user who has
 * chosen H24 and reads "14:33" in the table beside it.
 */

/** The 24h `HH:mm` of a text, or null when it is not a time. */
export function parseTimeInput(
  text: string,
  ctx: FormatContext
): string | null {
  const trimmed = text.trim().toLowerCase();
  if (trimmed === "") return null;

  // The day period, in the locale's own words as well as in English: `Intl` writes "vorm." in
  // German, but someone typing "pm" on a German account still means the afternoon.
  const period = periodOf(trimmed, ctx);
  const digits = trimmed.replace(/[^\d]/g, "");
  if (digits === "") return null;

  const [hour, minute] = splitDigits(digits) ?? [];
  if (hour == null || minute == null) return null;
  if (minute > 59) return null;

  const shifted = applyPeriod(hour, period);
  if (shifted == null || shifted > 23) return null;
  return `${pad(shifted)}:${pad(minute)}`;
}

/** Which half of the day the text names, if it names one at all. */
function periodOf(text: string, ctx: FormatContext): "am" | "pm" | undefined {
  for (const half of ["am", "pm"] as const) {
    const localized = dayPeriodOf(half, ctx.locale);
    if (
      // "a"/"p" alone, as the legacy input and most desktop software accept.
      new RegExp(`(^|[^a-z])${half[0]}m?([^a-z]|$)`).test(text) ||
      (localized && text.includes(localized.toLowerCase()))
    ) {
      return half;
    }
  }
  return undefined;
}

/** 12h → 24h: 12 am is midnight, 12 pm is noon. */
function applyPeriod(
  hour: number,
  period: "am" | "pm" | undefined
): number | null {
  if (!period) return hour;
  if (hour < 1 || hour > 12) return null;
  if (period === "am") return hour === 12 ? 0 : hour;
  return hour === 12 ? 12 : hour + 12;
}

/**
 * The hour and minute of a digit run: "9" is 9 o'clock, "930" half past nine, "0930" and "1430" the
 * same with a leading zero. Anything longer is not guessed at.
 */
function splitDigits(digits: string): [number, number] | null {
  switch (digits.length) {
    case 1:
    case 2:
      return [Number(digits), 0];
    case 3:
      return [Number(digits.slice(0, 1)), Number(digits.slice(1))];
    case 4:
      return [Number(digits.slice(0, 2)), Number(digits.slice(2))];
    default:
      return null;
  }
}

/** The text an input shows for a `HH:mm`, in the user's notation. */
export function formatTimeInput(
  time: string | null | undefined,
  ctx: FormatContext
): string {
  const parts = timePartsOf(time);
  if (!parts) return "";
  if (!ctx.hour12) return `${pad(parts[0])}:${pad(parts[1])}`;

  const half = parts[0] < 12 ? "am" : "pm";
  const hour = parts[0] % 12 === 0 ? 12 : parts[0] % 12;
  return `${hour}:${pad(parts[1])} ${dayPeriodOf(half, ctx.locale) ?? half.toUpperCase()}`;
}

/**
 * How one hour of the day is written in a picker column: "13" for an H24 account, "1 nachm." for an
 * H12 one. The value behind it is always the 24h hour, so the two columns stay 0-11 and 12-23.
 */
export function hourLabelOf(hour: number, ctx: FormatContext): string {
  if (!ctx.hour12) return pad(hour);
  const half = hour < 12 ? "am" : "pm";
  const shown = hour % 12 === 0 ? 12 : hour % 12;
  return `${shown} ${dayPeriodOf(half, ctx.locale) ?? half.toUpperCase()}`;
}

/** The `HH:mm` an hour and a minute stand for — what the picker hands back. */
export function timeOf(hour: number, minute: number): string {
  return `${pad(hour)}:${pad(minute)}`;
}

/** The mask to show in an empty field, so the expected notation is visible before typing. */
export function timePatternOf(ctx: FormatContext): string {
  return ctx.hour12
    ? `hh:mm ${dayPeriodOf("am", ctx.locale) ?? "AM"}`
    : "HH:mm";
}

/** The `[hour, minute]` of a `HH:mm`, or null when it is not one. */
export function timePartsOf(
  time: string | null | undefined
): [number, number] | null {
  const match = /^(\d{1,2}):(\d{2})/.exec(time?.trim() ?? "");
  if (!match) return null;
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (hour > 23 || minute > 59) return null;
  return [hour, minute];
}

const dayPeriodCache = new Map<string, string | undefined>();

/** How the locale writes "AM"/"PM" — "vorm."/"nachm." in German. */
function dayPeriodOf(half: "am" | "pm", locale: string): string | undefined {
  const key = `${locale}:${half}`;
  if (dayPeriodCache.has(key)) return dayPeriodCache.get(key);

  const probe = new Date(2033, 0, 1, half === "am" ? 9 : 21);
  const value = new Intl.DateTimeFormat(locale, {
    hour: "numeric",
    hourCycle: "h12",
  })
    .formatToParts(probe)
    .find((part) => part.type === "dayPeriod")?.value;
  const normalized = value?.toLowerCase();
  dayPeriodCache.set(key, normalized);
  return normalized;
}

function pad(value: number): string {
  return String(value).padStart(2, "0");
}
