import type { FormatContext } from "./format";

/**
 * Reading a date the user typed — the counterpart to the writing side in ./format.ts.
 *
 * A date lives here as an ISO string `yyyy-MM-dd`, never as a `Date`: that is how a `LocalDate`
 * travels over the wire (LocalDateConverter in projectforge-business), and it is what the column
 * filters compare lexicographically (components/data-table/filter-fns.ts). Only the text shown in an
 * input is localized.
 *
 * Field order and separators come from `Intl.DateTimeFormat.formatToParts` for the user's locale, so
 * what can be typed and what is displayed cannot drift apart. Unlike [formatDate] nothing here
 * applies `ctx.timeZone`: a date has no time, so converting it into a zone could only move it by a
 * day.
 */

type DateField = "day" | "month" | "year";

interface DateLayout {
  /** The three fields in the order the locale writes them. */
  order: DateField[];
  /** Matches exactly the locale's layout — used while typing, so nothing is rewritten too early. */
  strict: RegExp;
}

/** A date whose parts are all distinct and two-digit, so the layout can be read off the output. */
const PROBE = new Date(2033, 10, 22);

const layoutCache = new Map<string, DateLayout>();

function layoutOf(locale: string): DateLayout {
  const cached = layoutCache.get(locale);
  if (cached) return cached;

  const parts = new Intl.DateTimeFormat(locale, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).formatToParts(PROBE);

  const order: DateField[] = [];
  const pattern = parts
    .map((part) => {
      switch (part.type) {
        case "day":
        case "month":
          order.push(part.type);
          return "\\d{1,2}";
        case "year":
          order.push("year");
          return "(?:\\d{4}|\\d{2})";
        default:
          // The separator as the locale writes it, tolerant about surrounding spaces ("9. 8. 2033").
          return `\\s*${escape(part.value.trim())}\\s*`;
      }
    })
    .join("");
  const layout: DateLayout = { order, strict: new RegExp(`^${pattern}$`) };
  layoutCache.set(locale, layout);
  return layout;
}

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * The ISO date of a text the user typed, or null when it is not a date.
 *
 * @param strict Only accept the locale's full layout. Used on every keystroke: a lenient parse would
 *   turn a half-typed "1" into a date and correct the field under the user's fingers. Blur and Enter
 *   parse leniently, which is what makes "9.8.26" or "090826" work.
 */
export function parseDateInput(
  text: string,
  ctx: FormatContext,
  { strict = false }: { strict?: boolean } = {}
): string | null {
  const trimmed = text.trim();
  if (trimmed === "") return null;

  // An ISO date is always understood — it is the format this module speaks, and pasting one in is
  // the obvious thing to try.
  const iso = /^(\d{4})-(\d{2})-(\d{2})$/.exec(trimmed);
  if (iso) return toIso(+iso[1], +iso[2], +iso[3]);

  const { order, strict: strictPattern } = layoutOf(ctx.locale);
  if (strict && !strictPattern.test(trimmed)) return null;

  const groups = trimmed.match(/\d+/g);
  if (!groups) return null;

  const fields = groups.length === 1 ? splitDigits(groups[0], order) : groups;
  if (!fields || fields.length !== 3) return null;

  const values: Record<DateField, number> = { day: 0, month: 0, year: 0 };
  order.forEach((field, index) => {
    values[field] = Number(fields[index]);
    if (field === "year" && fields[index].length <= 2) {
      values.year = expandTwoDigitYear(values.year);
    }
  });
  return toIso(values.year, values.month, values.day);
}

/**
 * A date typed without separators ("22112033"), split by the widths its fields have when written out
 * — which is why the order matters: the four digits belong to the year wherever the locale puts it.
 * Any other number of digits stays unparsed rather than being guessed at.
 */
function splitDigits(digits: string, order: DateField[]): string[] | null {
  const yearWidth = digits.length === 8 ? 4 : digits.length === 6 ? 2 : 0;
  if (!yearWidth) return null;

  const fields: string[] = [];
  let at = 0;
  for (const field of order) {
    const width = field === "year" ? yearWidth : 2;
    fields.push(digits.slice(at, at + width));
    at += width;
  }
  return fields;
}

/**
 * Which century a two-digit year means, with the pivot moment.js used and the legacy input therefore
 * behaved by: 69-99 is the 20th century, 00-68 the 21st.
 */
function expandTwoDigitYear(year: number): number {
  return year >= 69 ? 1900 + year : 2000 + year;
}

/** The ISO date of the three numbers, or null if they are not a real date (31.02.). */
function toIso(year: number, month: number, day: number): string | null {
  if (!year || !month || !day) return null;
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  const date = new Date(year, month - 1, day);
  // A day past the end of its month rolls over in the constructor; that is not what was typed.
  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return null;
  }
  return isoOf(date);
}

/** The text an input shows for an ISO date: the locale's layout, no time zone applied. */
export function formatDateInput(
  iso: string | null | undefined,
  ctx: FormatContext
): string {
  const date = dateOf(iso);
  if (!date) return "";
  return new Intl.DateTimeFormat(ctx.locale, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

/** ±n days on an ISO date, for the arrow keys of a date input. */
export function shiftDateByDays(
  iso: string | null | undefined,
  days: number
): string | null {
  const date = dateOf(iso);
  if (!date) return null;
  date.setDate(date.getDate() + days);
  return isoOf(date);
}

/**
 * Whole days from one ISO date to the other, `null` if either is missing — the other direction of
 * [shiftDateByDays], and what a payment target in days is: the distance between the invoice date and
 * the due date (`AbstractRechnungDO.recalculate` computes it the same way on the backend).
 *
 * Rounded because the two local midnights are 23 or 25 hours apart across a DST switch.
 */
export function daysBetweenDates(
  from: string | null | undefined,
  to: string | null | undefined
): number | null {
  const start = dateOf(from);
  const end = dateOf(to);
  if (!start || !end) return null;
  return Math.round((end.getTime() - start.getTime()) / 86_400_000);
}

/**
 * The `Date` a calendar needs, built from the parts in the local zone (so it stands for that very
 * day, whereas `new Date("2026-08-09")` would be UTC midnight and can fall on the day before).
 */
export function dateOf(iso: string | null | undefined): Date | null {
  if (!iso) return null;
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!match) return null;
  const date = new Date(+match[1], +match[2] - 1, +match[3]);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Back the other way: the ISO date of a `Date`, read in the local zone for the same reason. */
export function isoOf(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Today as an ISO date. */
export function todayIso(): string {
  return isoOf(new Date());
}
