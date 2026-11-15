/**
 * The csv of Merlin's value lists (`mappingValues` of a dependent variable), as the backend reads it with
 * `de.micromata.merlin.csv.CSVStringUtils.parseStringList`: values separated by `,`, `;` or `:`, trimmed,
 * optionally quoted with double (`"…"`, `„…“`) or single quotes (`'…'`, `‚…‘`). There is no escaping: a
 * quoted value ends at the first closing quote of its kind.
 */

const SINGLE_START_QUOTES = "‚'’`´";
const SINGLE_END_QUOTES = "‘'’`´";
const DOUBLE_START_QUOTES = '„"“”';
const DOUBLE_END_QUOTES = '“”"';
const SEPARATORS = ",;:";

/** The values of `csv`, as the backend parses them. */
export function parseMerlinCsv(csv: string | null | undefined): string[] {
  if (!csv) return [];
  const result: string[] = [];
  let value: string | null = null;
  let endQuotes: string | null = null; // null: an unquoted value
  for (const ch of csv) {
    if (value === null) {
      if (/\s/.test(ch)) continue;
      if (SEPARATORS.includes(ch)) {
        // A separator after a quoted value; another one (an empty unquoted value) is skipped, as by the backend.
        continue;
      }
      value = "";
      if (SINGLE_START_QUOTES.includes(ch)) {
        endQuotes = SINGLE_END_QUOTES;
      } else if (DOUBLE_START_QUOTES.includes(ch)) {
        endQuotes = DOUBLE_END_QUOTES;
      } else {
        endQuotes = null;
        value = ch;
      }
      continue;
    }
    if (endQuotes?.includes(ch)) {
      result.push(value.trim());
      value = null;
      continue;
    }
    if (endQuotes === null && SEPARATORS.includes(ch)) {
      result.push(value.trim());
      value = null;
      continue;
    }
    value += ch;
  }
  if (value !== null) result.push(value.trim());
  return result;
}

/** A value as written into the csv, or null if it can't be written (it needs quotes and contains both kinds). */
function writeValue(raw: string): string | null {
  const value = raw.trim();
  const needsQuotes =
    value === "" ||
    [...value].some((ch) => SEPARATORS.includes(ch)) ||
    SINGLE_START_QUOTES.includes(value[0]) ||
    DOUBLE_START_QUOTES.includes(value[0]);
  if (!needsQuotes) return value;
  if (![...value].some((ch) => DOUBLE_END_QUOTES.includes(ch))) {
    return `"${value}"`;
  }
  if (![...value].some((ch) => SINGLE_END_QUOTES.includes(ch))) {
    return `'${value}'`;
  }
  return null;
}

/** Whether `value` can be written into the csv (see `writeMerlinCsv`). */
export function isMerlinCsvWritable(value: string): boolean {
  return writeValue(value) !== null;
}

/**
 * `values` as csv, each value read back as given (but trimmed): a value with a separator, an empty one or
 * one starting with a quote is quoted. A value that can't be written (`isMerlinCsvWritable`) is left out
 * of its quotes as it is, the dialog refuses it before.
 */
export function writeMerlinCsv(values: readonly string[]): string {
  return values.map((it) => writeValue(it) ?? it.trim()).join(", ");
}
