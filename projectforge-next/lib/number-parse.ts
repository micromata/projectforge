import type { FormatContext } from "./format";

/**
 * Reading a number the user typed — the counterpart to `formatNumber`/`formatCurrency` in ./format.ts,
 * and the same split as ./date-parse.ts: the value lives as a JS `number` (that is how a `BigDecimal`
 * travels over the wire), only the text in an input is localized.
 *
 * The separators come from `Intl.NumberFormat.formatToParts` for the user's locale, so what can be
 * typed and what is displayed cannot drift apart. `userData.decimalSeparator` says the same thing, but
 * the writing side already derives from Intl and one source is enough.
 */

interface NumberLayout {
  group: string;
  decimal: string;
}

/** A number whose parts are all distinct, so the separators can be read off the output. */
const PROBE = 1234.5;

const layoutCache = new Map<string, NumberLayout>();

function layoutOf(locale: string): NumberLayout {
  const cached = layoutCache.get(locale);
  if (cached) return cached;
  const parts = new Intl.NumberFormat(locale, {
    minimumFractionDigits: 1,
  }).formatToParts(PROBE);
  const layout: NumberLayout = {
    group: parts.find((p) => p.type === "group")?.value ?? ",",
    decimal: parts.find((p) => p.type === "decimal")?.value ?? ".",
  };
  layoutCache.set(locale, layout);
  return layout;
}

/**
 * The number a text stands for, or null when it is not one.
 *
 * Both separators are accepted as the decimal point when the locale's other one is absent: a "." typed
 * on the numeric keypad of a German keyboard means a decimal point, and rejecting it would be an input
 * that refuses the obvious. A text holding both ("1.234,50") is read by the locale's own rules.
 */
export function parseNumberInput(
  text: string,
  ctx: FormatContext
): number | null {
  const trimmed = text.trim();
  if (trimmed === "") return null;
  const { group, decimal } = layoutOf(ctx.locale);
  const other = decimal === "," ? "." : ",";
  let normalized: string;
  if (!trimmed.includes(decimal) && trimmed.includes(other)) {
    // The locale's decimal separator is nowhere to be seen, so the other one is what was meant. Only
    // the last occurrence: "1.234.5" was typed as a group separator plus a decimal point.
    //
    // Read off `trimmed`, not off a text the group separator was already stripped from: in German the
    // group separator *is* the other one, so stripping it first would delete the very character this
    // branch exists to interpret.
    const at = trimmed.lastIndexOf(other);
    normalized =
      trimmed.slice(0, at).replaceAll(other, "") + "." + trimmed.slice(at + 1);
  } else {
    normalized = trimmed.replaceAll(group, "").replaceAll(decimal, ".");
  }
  // Everything else the locale writes (a currency symbol pasted along, spaces) is dropped; a text that
  // is not a number at all still ends up NaN below.
  normalized = normalized.replace(/[^\d.+-]/g, "");
  if (normalized === "" || !/^[+-]?(\d+\.?\d*|\.\d+)$/.test(normalized)) {
    return null;
  }
  const value = Number(normalized);
  return Number.isFinite(value) ? value : null;
}

/** The group and decimal separator of the user's locale — the same source [parseNumberInput] reads. */
export function numberLayout(ctx: FormatContext): {
  group: string;
  decimal: string;
} {
  return layoutOf(ctx.locale);
}

/**
 * The number a grouped box's text stands for, or null when it is not one yet — the reading mirror of
 * [groupNumberInput], for a box that groups its thousands while it is being edited.
 *
 * Unlike [parseNumberInput], the locale's group separator is grouping and its decimal separator the only
 * decimal point: there is no keypad-"." fallback, because a grouping box inserts the group separator itself
 * ("1.234.567" in German is over a million, not 1234.567). Parsing the same way the box groups keeps the
 * text on screen and the value it stands for from ever disagreeing.
 */
export function parseGroupedInput(
  text: string,
  ctx: FormatContext
): number | null {
  const { group, decimal } = layoutOf(ctx.locale);
  const trimmed = text.trim();
  if (trimmed === "") return null;
  const normalized = trimmed
    .replaceAll(group, "")
    .replaceAll(decimal, ".")
    .replace(/[^\d.+-]/g, "");
  if (normalized === "" || !/^[+-]?(\d+\.?\d*|\.\d+)$/.test(normalized)) {
    return null;
  }
  const value = Number(normalized);
  return Number.isFinite(value) ? value : null;
}

/** Groups a run of digits from the right, using `group` as the separator: "1234567" → "1.234.567". */
function groupDigits(digits: string, group: string): string {
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, group);
}

/**
 * The typed text with its integer part grouped, preserving the fractional part exactly as typed — a
 * trailing separator ("1234," → "1.234,") and any decimals ("1234,50" → "1.234,50") are kept, so grouping
 * can stay visible while a box is being edited rather than only at rest.
 *
 * Unlike [parseNumberInput], the locale's group separator is treated as grouping here, not as a possible
 * decimal point: a box that groups its thousands inserts that separator itself, so re-reading it as a
 * decimal would fight the auto-grouping (in German "1.234" would flip to "1,234" on the next keystroke).
 * The decimal point while editing such a box is therefore the locale's decimal separator only.
 *
 * A text holding no digit at all (a lone sign, an emptied box) is returned unchanged.
 */
export function groupNumberInput(text: string, ctx: FormatContext): string {
  const { group, decimal } = layoutOf(ctx.locale);
  const trimmed = text.trim();
  const sign = trimmed.startsWith("-")
    ? "-"
    : trimmed.startsWith("+")
      ? "+"
      : "";
  const rest = sign ? trimmed.slice(1) : trimmed;
  const at = rest.indexOf(decimal); // the first decimal separator; further ones collapse into it
  const hasDecimal = at >= 0;
  const intDigits = (hasDecimal ? rest.slice(0, at) : rest).replace(/\D/g, "");
  const fracDigits = hasDecimal ? rest.slice(at + 1).replace(/\D/g, "") : null;
  if (intDigits === "" && !fracDigits) {
    // Nothing but a sign and/or a bare decimal separator so far — keep it as the user left it.
    return hasDecimal ? `${sign}${decimal}` : sign;
  }
  const grouped = groupDigits(intDigits, group);
  return hasDecimal
    ? `${sign}${grouped}${decimal}${fracDigits}`
    : `${sign}${grouped}`;
}

/**
 * The percentage a text asks for, or null when it asks for none — "50 %" is 50, "50" is null.
 *
 * Only a **trailing** percent sign counts, as in Wicket's `CurrencyConverter`: it is the one place a
 * number can be qualified without becoming ambiguous, and it is what a user has typed there for years.
 * What the percentage is *of* is not this function's business — see [NumberFieldProps.shareOf].
 */
export function parsePercentInput(
  text: string,
  ctx: FormatContext
): number | null {
  const trimmed = text.trim();
  if (!trimmed.endsWith("%")) return null;
  return parseNumberInput(trimmed.slice(0, -1), ctx);
}

/**
 * The text an input shows for a number, in the locale's layout.
 *
 * @param fractionDigits Digits after the separator, e.g. 2 for an amount. Undefined keeps what the
 *   value has, which is what a quantity like person days wants.
 * @param grouping Whether thousands are grouped ("2.394,00" rather than "2394,00"). Off by default,
 *   because that is the form a value is *edited* in: a group separator inserted between keystrokes
 *   moves the caret out from under the fingers typing. A box at rest asks for grouping — that is what a
 *   reader wants, and what Wicket's `CurrencyConverter` writes — so [NumberField] turns it on there and
 *   off again on focus.
 */
export function formatNumberInput(
  value: number | null | undefined,
  ctx: FormatContext,
  fractionDigits?: number,
  grouping = false
): string {
  if (value == null || !Number.isFinite(value)) return "";
  return new Intl.NumberFormat(ctx.locale, {
    useGrouping: grouping,
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits ?? 6,
  }).format(value);
}
