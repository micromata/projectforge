import {
  PAGINATION_PAGE_SIZE_FIELD,
  type FilterElement,
  type MagicFilterEntry,
  type MagicFilterEntryValue,
} from "@/lib/rs/types";

/** The filter values of one list, keyed by the backend field id. */
export type FilterValues = Record<string, MagicFilterEntryValue>;

/** The values as the entries MagicFilter expects, in a stable (sorted) order. */
export function filterEntriesOf(values: FilterValues): MagicFilterEntry[] {
  return Object.keys(values)
    .sort()
    .map((field) => ({ field, value: values[field] }));
}

/**
 * The reverse: the field entries of a MagicFilter as filter values.
 *
 * Used when the backend hands a whole filter back (a saved filter that was
 * applied). Entries without a field are dropped — MagicFilter.init does the same —
 * and so is the page size, which travels as an entry but is not a filter.
 */
export function filterValuesFromEntries(
  entries: MagicFilterEntry[] | undefined
): FilterValues {
  const values: FilterValues = {};
  entries?.forEach((entry) => {
    if (!entry.field || entry.field === PAGINATION_PAGE_SIZE_FIELD) return;
    if (isEmptyFilterValue(entry.value)) return;
    values[entry.field] = entry.value as MagicFilterEntryValue;
  });
  return values;
}

/**
 * A comparable form of everything the filter row holds: the field values and the
 * search string, normalised (empty values dropped, fields sorted) so only real
 * differences show up.
 *
 * Used to tell whether the current filter still matches the saved favorite it came
 * from. `MagicFilter.isModified` does the same server-side, but that comparison
 * isn't exposed for list pages — the legacy frontend therefore hardcodes
 * "modified" (`SearchFilter.jsx`).
 */
export function filterFingerprint(filter: {
  entries?: MagicFilterEntry[];
  searchString?: string;
}): string {
  return JSON.stringify({
    entries: filterEntriesOf(filterValuesFromEntries(filter.entries)),
    searchString: filter.searchString ?? "",
  });
}

/**
 * Wraps the term in wildcards: the backend turns a STRING entry into a LIKE
 * predicate that matches the whole field otherwise ("Larkin" finds nothing when
 * the value is "Peter J. Larkin"). Terms that already carry a wildcard are left
 * alone so users can anchor a search themselves.
 */
export function toLikeTerm(input: string): string {
  const term = input.trim();
  if (term === "") return "";
  return term.includes("*") ? term : `*${term}*`;
}

/** Strips the wildcards again so the input shows what the user typed. */
export function fromLikeTerm(stored: string | undefined): string {
  if (!stored) return "";
  const match = /^\*(.*)\*$/.exec(stored);
  return match ? match[1] : stored;
}

/** True when the value would not narrow the list, so it should not be stored. */
export function isEmptyFilterValue(
  value: MagicFilterEntryValue | undefined
): boolean {
  if (!value) return true;
  if (value.values?.length) return false;
  return !value.value && !value.from && !value.to && !value.id;
}

/** Sets or — for an empty value — removes one field, always returning a new object. */
export function withFilterValue(
  values: FilterValues,
  field: string,
  value: MagicFilterEntryValue | undefined
): FilterValues {
  const next = { ...values };
  if (isEmptyFilterValue(value)) delete next[field];
  else next[field] = value as MagicFilterEntryValue;
  return next;
}

/**
 * Renders a filter value the way it was entered: LIST ids resolve to their
 * display names, ranges read as "from – to", and the wildcards a STRING filter
 * needs for its LIKE query are stripped again.
 */
export function describeFilterValue(
  value: MagicFilterEntryValue | undefined,
  element: FilterElement | undefined
): string {
  if (!value) return "";
  if (value.values?.length) {
    return value.values
      .map((id) => element?.values?.find((v) => v.id === id)?.displayName ?? id)
      .join(", ");
  }
  if (value.from || value.to) {
    return [value.from, value.to].filter(Boolean).join(" – ");
  }
  if (value.displayName) return value.displayName;
  if (value.value == null) return "";
  // BOOLEAN filters carry "true"; the label alone already says what is meant.
  if (element?.filterType === "BOOLEAN") return "";
  return fromLikeTerm(value.value);
}
