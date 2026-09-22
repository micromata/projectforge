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
