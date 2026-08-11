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
  let normalized = trimmed.replaceAll(group, "");
  if (!trimmed.includes(decimal) && trimmed.includes(other)) {
    // The locale's decimal separator is nowhere to be seen, so the other one is what was meant. Only
    // the last occurrence: "1.234.5" was typed as a group separator plus a decimal point.
    const at = normalized.lastIndexOf(other);
    normalized =
      normalized.slice(0, at).replaceAll(other, "") +
      "." +
      normalized.slice(at + 1);
  } else {
    normalized = normalized.replaceAll(decimal, ".");
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
 * The text an input shows for a number: the locale's layout, without a group separator.
 *
 * Grouping is left out on purpose — it is what a reader wants in a table, not what someone editing a
 * value wants under the caret, and it would have to be re-parsed on every keystroke.
 *
 * @param fractionDigits Digits after the separator, e.g. 2 for an amount. Undefined keeps what the
 *   value has, which is what a quantity like person days wants.
 */
export function formatNumberInput(
  value: number | null | undefined,
  ctx: FormatContext,
  fractionDigits?: number
): string {
  if (value == null || !Number.isFinite(value)) return "";
  return new Intl.NumberFormat(ctx.locale, {
    useGrouping: false,
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits ?? 6,
  }).format(value);
}
