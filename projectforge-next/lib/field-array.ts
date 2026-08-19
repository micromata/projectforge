/**
 * The pure mechanics behind `useFieldArray` — reading an array out of the form values by its path, and
 * the three row transitions of a soft-deleting collection.
 *
 * Separate from the hook so they can be tested at all: the vitest run is `environment: "node"` and knows
 * no React, and these are exactly the rules worth pinning down (a row that vanishes instead of being
 * marked deleted costs data and history in the backend — see `RechnungsPosition` in projectforge-rest).
 */

/** The least a row of a nested collection has: its identity and whether it is deleted. */
export interface ArrayRow {
  /** null for a row that has not been saved yet — the backend assigns the id. */
  id?: number | null;
  /**
   * Soft delete. A removed row is **kept** in the values with this set, never spliced out: the
   * backend's `CollectionHandler` physically deletes (history and all) whatever is missing from the
   * posted collection, so "deleted" has to be said explicitly.
   */
  deleted?: boolean;
}

/**
 * The array at `path` inside the form values, or `[]` where any hop is missing.
 *
 * `path` is TanStack's own field path syntax: a dotted name with bracketed indices, e.g. `positionen` or
 * `positionen[0].kostZuweisungen` — the second nesting level of the invoice form. A flat
 * `values[path]` lookup answers nothing for the latter, which is why this exists.
 */
export function readArrayAtPath<Row>(values: unknown, path: string): Row[] {
  // `a[0].b` -> ["a", "0", "b"]: brackets become separators, so an index is just another hop.
  const hops = path.replace(/\[(\w+)\]/g, ".$1").split(".");
  let current: unknown = values;
  for (const hop of hops) {
    if (current == null || typeof current !== "object") return [];
    current = (current as Record<string, unknown>)[hop];
  }
  return Array.isArray(current) ? (current as Row[]) : [];
}

/**
 * Marks the row at `index` deleted, or drops it outright when it was never saved: an unsaved row has
 * nothing in the database to soft-delete, and keeping it would post an empty row.
 */
export function removeRow<Row extends ArrayRow>(
  rows: Row[],
  index: number
): Row[] {
  const row = rows[index];
  if (!row) return rows;
  if (row.id == null) {
    return rows.filter((_, at) => at !== index);
  }
  return rows.map((entry, at) =>
    at === index ? { ...entry, deleted: true } : entry
  );
}

/** Takes a deleted row back — the counterpart of [removeRow] for a row that still exists in the list. */
export function restoreRow<Row extends ArrayRow>(
  rows: Row[],
  index: number
): Row[] {
  return rows.map((entry, at) =>
    at === index ? { ...entry, deleted: false } : entry
  );
}

export function updateRow<Row extends ArrayRow>(
  rows: Row[],
  index: number,
  changes: Partial<Row>
): Row[] {
  return rows.map((entry, at) =>
    at === index ? { ...entry, ...changes } : entry
  );
}
