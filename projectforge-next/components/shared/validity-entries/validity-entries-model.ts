/**
 * Entries with a validity period: each one holds from its date on until the next entry's date — an
 * employee's weekly hours since a date, a currency pair's rate from a date. Only the start is stored, the
 * end is the start of the following entry.
 *
 * Dates are ISO `yyyy-MM-dd`, so comparing them as strings compares them as days.
 */

/** How an entry stands relative to today. */
export type Validity = "past" | "current" | "future";

/** Newest first — the order the backend sends and the legacy table showed. Entries without a date go last. */
export function sortByValidity<T>(
  entries: readonly T[],
  dateOf: (entry: T) => string | null | undefined
): T[] {
  return [...entries].sort((a, b) => {
    const da = dateOf(a) ?? "";
    const db = dateOf(b) ?? "";
    return da < db ? 1 : da > db ? -1 : 0;
  });
}

/**
 * The entry in force on [today]: the latest one starting on or before it. Undefined if every entry starts
 * later (or there is none).
 */
export function currentEntry<T>(
  entries: readonly T[],
  dateOf: (entry: T) => string | null | undefined,
  today: string
): T | undefined {
  let best: T | undefined;
  let bestDate: string | undefined;
  for (const entry of entries) {
    const date = dateOf(entry);
    if (!date || date > today) continue;
    if (bestDate === undefined || date > bestDate) {
      best = entry;
      bestDate = date;
    }
  }
  return best;
}

/** Classifies every entry: the [currentEntry] is current, later ones future, the earlier ones past. */
export function validityOf<T>(
  entry: T,
  current: T | undefined,
  dateOf: (entry: T) => string | null | undefined,
  today: string
): Validity {
  if (entry === current) return "current";
  const date = dateOf(entry);
  return date && date > today ? "future" : "past";
}
