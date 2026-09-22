import { canonicalFormatter, type FormatterName } from "./format-names";
import type { UserData } from "./rs/types";

/**
 * Value formatting based on the user's locale, time zone and currency, which the
 * backend sends in userData.
 *
 * Uses Intl rather than the `jsDateFormat`/`jsTimestampFormat*` patterns from
 * userData: those are moment.js syntax ("DD.MM.YYYY HH:mm") and moment is not a
 * dependency here. Intl derives the same layout from the locale.
 */

export interface FormatContext {
  locale: string;
  timeZone?: string;
  currency?: string;
  /**
   * Index of the first day of the week, 0 = Sunday — the form react-day-picker takes
   * (`weekStartsOn`), which is why the backend sends it that way (userData.firstDayOfWeekSunday0).
   * It is the user's setting, not the locale's default: a German account may well start on Sunday.
   */
  weekStartsOn?: 0 | 1 | 2 | 3 | 4 | 5 | 6;
  /**
   * The date layout as a mask the user can read ("dd.MM.yyyy"), used as the placeholder of a date
   * input. Taken from userData.dateFormat, i.e. the same pattern the backend formats with; only
   * [formatDate] derives its output from Intl instead.
   */
  datePattern?: string;
  /**
   * Whether times are written with AM/PM. The user's own setting (userData.timeNotation, H12/H24),
   * not the locale's habit: an English speaking account in Germany may well have picked 24h, and
   * `Intl` would otherwise decide by locale alone.
   */
  hour12?: boolean;
}

/** The hour options every formatter here shares, so time notation is applied in exactly one place. */
function hourOptions(ctx: FormatContext): Intl.DateTimeFormatOptions {
  if (ctx.hour12 == null) return { hour: "2-digit", minute: "2-digit" };
  return {
    hour: "2-digit",
    minute: "2-digit",
    // hourCycle, not just hour12: `hour12: false` alone yields "24:30" in some locales.
    hourCycle: ctx.hour12 ? "h12" : "h23",
  };
}

/** Maps a backend locale ("de_DE") onto a BCP-47 tag Intl understands. */
function toBcp47(locale: string | undefined): string {
  return locale ? locale.replace("_", "-") : "de-DE";
}

/**
 * Translates a Java-format date pattern into the user's language for display as a placeholder.
 *
 * The backend stores patterns in Java/ISO letter conventions ("dd.MM.yyyy"), which are English
 * abbreviations. German users should see "TT.MM.JJJJ" (Tag / Monat / Jahr).
 */
function localisePattern(pattern: string, locale: string): string {
  const lang = locale.split("-")[0].toLowerCase();
  if (lang === "de") {
    // d→T (Tag), y→J (Jahr); M stays — "Monat" also starts with M
    return pattern.replace(/d/g, "T").replace(/y/g, "J");
  }
  return pattern;
}

function toWeekStartsOn(
  value: number | undefined
): FormatContext["weekStartsOn"] {
  if (value == null || value < 0 || value > 6) return undefined;
  return value as NonNullable<FormatContext["weekStartsOn"]>;
}

export function formatContextFrom(
  user: UserData | null | undefined
): FormatContext {
  return {
    locale: toBcp47(user?.locale),
    timeZone: user?.timeZone,
    currency: user?.currency,
    weekStartsOn: toWeekStartsOn(user?.firstDayOfWeekSunday0),
    datePattern: user?.dateFormat
      ? localisePattern(user.dateFormat, toBcp47(user?.locale))
      : undefined,
    // Unset when the backend sends neither, so Intl keeps deciding by locale.
    hour12: user?.timeNotation
      ? user.timeNotation.toUpperCase() === "H12"
      : undefined,
  };
}

function toDate(value: unknown): Date | null {
  if (value == null || value === "") return null;
  const date =
    typeof value === "number" ? new Date(value) : new Date(String(value));
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Date only, e.g. 17.06.2016. */
export function formatDate(value: unknown, ctx: FormatContext): string {
  const date = toDate(value);
  if (!date) return "";
  return new Intl.DateTimeFormat(ctx.locale, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    timeZone: ctx.timeZone,
  }).format(date);
}

/**
 * Both ends of a period as the one value it is, e.g. `01.01.2026 – 31.12.2026`.
 *
 * A half-open period reads with an ellipsis for its open end ("01.01.2026 – …"), so it is visibly a
 * period and not a single date; empty on both ends it is the empty string, and a caller renders
 * nothing at all rather than a dash.
 *
 * The one place a period becomes text: a filter pill, a list column and a position's header all show
 * the same window and must show it the same way.
 */
export function formatDateRange(
  begin: unknown,
  end: unknown,
  ctx: FormatContext
): string {
  return joinRange(formatDate(begin, ctx), formatDate(end, ctx));
}

/** [formatDateRange] for a period whose ends carry a time as well. */
export function formatTimestampRange(
  begin: unknown,
  end: unknown,
  ctx: FormatContext
): string {
  return joinRange(
    formatTimestampMinutes(begin, ctx),
    formatTimestampMinutes(end, ctx)
  );
}

/** Time only, to the minute, e.g. 14:33 — the day is context the caller already shows. */
export function formatTime(value: unknown, ctx: FormatContext): string {
  const date = toDate(value);
  if (!date) return "";
  return new Intl.DateTimeFormat(ctx.locale, {
    ...hourOptions(ctx),
    timeZone: ctx.timeZone,
  }).format(date);
}

/** [formatTimeRange] for a same-day period, e.g. `10:30 – 12:00`. */
export function formatTimeRange(
  begin: unknown,
  end: unknown,
  ctx: FormatContext
): string {
  return joinRange(formatTime(begin, ctx), formatTime(end, ctx));
}

function joinRange(from: string, to: string): string {
  if (!from && !to) return "";
  if (from && to) return `${from} – ${to}`;
  return from ? `${from} – …` : `… – ${to}`;
}

/** Date and time to the minute, e.g. 17.06.2016, 14:33. */
export function formatTimestampMinutes(
  value: unknown,
  ctx: FormatContext
): string {
  const date = toDate(value);
  if (!date) return "";
  return new Intl.DateTimeFormat(ctx.locale, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    ...hourOptions(ctx),
    timeZone: ctx.timeZone,
  }).format(date);
}

/** Date and time to the second. */
export function formatTimestampSeconds(
  value: unknown,
  ctx: FormatContext
): string {
  const date = toDate(value);
  if (!date) return "";
  return new Intl.DateTimeFormat(ctx.locale, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    ...hourOptions(ctx),
    second: "2-digit",
    timeZone: ctx.timeZone,
  }).format(date);
}

export function formatNumber(
  value: unknown,
  ctx: FormatContext,
  fractionDigits?: number
): string {
  if (value == null || value === "") return "";
  const numeric = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(numeric)) return "";
  return new Intl.NumberFormat(ctx.locale, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(numeric);
}

export function formatCurrency(value: unknown, ctx: FormatContext): string {
  if (value == null || value === "") return "";
  const numeric = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(numeric)) return "";
  return new Intl.NumberFormat(ctx.locale, {
    style: "currency",
    // The backend may send a symbol ("€"); Intl needs an ISO code.
    currency:
      ctx.currency && /^[A-Z]{3}$/.test(ctx.currency) ? ctx.currency : "EUR",
  }).format(numeric);
}

/** Integer percentage, e.g. 19 → "19 %". */
export function formatPercentage(value: unknown, ctx: FormatContext): string {
  if (value == null || value === "") return "";
  const numeric = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(numeric)) return "";
  return new Intl.NumberFormat(ctx.locale, {
    style: "percent",
    maximumFractionDigits: 1,
  }).format(numeric / 100);
}

/**
 * Decimal fraction as a percentage, e.g. 0.19 → "19 %".
 *
 * @param fractionDigits Digits behind the separator, at most; one by default. Zero where the percentage
 *   is a share read at a glance rather than a rate — the share a cost assignment carries of its
 *   position, which Wicket rounds to whole percent as well (`RechnungCostEditTablePanel`).
 */
export function formatPercentageDecimal(
  value: unknown,
  ctx: FormatContext,
  fractionDigits = 1
): string {
  if (value == null || value === "") return "";
  const numeric = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(numeric)) return "";
  return new Intl.NumberFormat(ctx.locale, {
    style: "percent",
    maximumFractionDigits: fractionDigits,
  }).format(numeric);
}

/** Resolves the display text of a referenced entity, or a list of them. */
export function formatDisplayName(value: unknown): string {
  if (value == null) return "";
  if (Array.isArray(value)) return value.map(formatDisplayName).join(", ");
  if (typeof value === "object") {
    const record = value as Record<string, unknown>;
    const name =
      record.displayName ??
      record.fullname ??
      record.username ??
      record.title ??
      // Cost/account references carry their number, orders their position.
      record.formattedNumber ??
      record.number ??
      record.name;
    return name == null ? "" : String(name);
  }
  return String(value);
}

/**
 * Formats a cell value the way the backend's formatter name asks for. Unknown
 * names fall back to the plain value so a new backend formatter degrades to
 * readable text instead of an empty cell — the legacy webapp rendered a literal
 * '???' there, which leaked into the UI.
 *
 * BOOLEAN, RATING, CONSUMPTION and TREE_NAVIGATION are handled by a cell
 * component instead (see formatterToCellKind) and end up in the default branch.
 */
export function formatValue(
  value: unknown,
  formatter: FormatterName | string | undefined,
  ctx: FormatContext
): string {
  switch (canonicalFormatter(formatter)) {
    case "DATE":
      return formatDate(value, ctx);
    case "TIMESTAMP_MINUTES":
      return formatTimestampMinutes(value, ctx);
    case "TIMESTAMP_SECONDS":
      return formatTimestampSeconds(value, ctx);
    case "CURRENCY":
      return formatCurrency(value, ctx);
    case "CURRENCY_PLAIN":
      return formatNumber(value, ctx, 2);
    case "NUMBER":
      return formatNumber(value, ctx);
    case "PERCENTAGE":
      return formatPercentage(value, ctx);
    case "PERCENTAGE_DECIMAL":
      return formatPercentageDecimal(value, ctx);
    // All of these resolve a referenced entity to its display text.
    case "SHOW_DISPLAYNAME":
    case "SHOW_LIST_OF_DISPLAYNAMES":
    case "ADDRESS_BOOK":
    case "AUFTRAG_POSITION":
    case "EMPLOYEE":
    case "COST1":
    case "COST2":
    case "CUSTOMER":
    case "GROUP":
    case "KONTO":
    case "PROJECT":
    case "TASK_PATH":
    case "USER":
      return formatDisplayName(value);
    default:
      return formatDisplayName(value);
  }
}
