import {
  compareText,
  formatDate,
  formatMonthRange,
  formatTimestampMinutes,
  type FormatContext,
} from "@/lib/format";
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
 *
 * Date bounds travel as ISO strings and are shown in the user's layout and time zone, so a pill reads
 * like the column beside it — the same reason [describeHistoryFilter] formats its interval.
 */
export function describeFilterValue(
  value: MagicFilterEntryValue | undefined,
  element: FilterElement | undefined,
  ctx: FormatContext
): string {
  if (!value) return "";
  const labels = filterValueLabels(value, element);
  if (labels) return labels.join(", ");
  if ((value.from || value.to) && element?.filterType === "MONTH") {
    return formatMonthRange(value.from, value.to, ctx);
  }
  if (value.from || value.to) {
    const bound = (iso: string | undefined) =>
      element?.filterType === "TIMESTAMP"
        ? formatTimestampMinutes(iso, ctx)
        : formatDate(iso, ctx);
    // A half-open range reads as "from …" / "… to", the ellipsis standing for the open end.
    const from = bound(value.from);
    const to = bound(value.to);
    return from && to ? `${from} – ${to}` : from ? `${from} – …` : `… – ${to}`;
  }
  if (value.displayName) return value.displayName;
  if (value.value == null) return "";
  // BOOLEAN filters carry "true"; the label alone already says what is meant.
  if (element?.filterType === "BOOLEAN") return "";
  return fromLikeTerm(value.value);
}

/**
 * The display names of a LIST value's picks, or null for any other kind of value. A key the element
 * doesn't offer (any more, or not yet fetched) is shown as it is rather than vanishing.
 */
export function filterValueLabels(
  value: MagicFilterEntryValue | undefined,
  element: FilterElement | undefined
): string[] | null {
  if (!value?.values?.length) return null;
  return value.values.map(
    (id) => element?.values?.find((v) => v.id === id)?.displayName ?? id
  );
}

/** What a filter pill says: on its face, and in full in its tooltip. */
export interface FilterPillContent {
  /** After the label: "Kunde: <text>". Empty for a boolean, whose label alone says it. */
  text: string;
  /** Picks left off [text], shown as "+n" beside it so a truncated text never hides the count. */
  more: number;
  tooltip?: string;
  /** Show [tooltip] verbatim, line by line — a list of user data, no markdown. */
  tooltipPlain: boolean;
}

/** How many picks of a multi-value filter a pill names before it counts the rest. */
const PILL_NAMED_PICKS = 3;

/**
 * What the pill of [element] says about [value], the same in the list's filter row ([FilterPill]) and in a
 * chart's summary of it ([AppliedFilterSummary]):
 * - several picks, alphabetically: the first few named, the rest counted ("A, B, C" +12), all of them listed in the
 *   tooltip, one per line;
 * - a task: only the task itself, since its ancestors would truncate away the one segment that identifies
 *   it — the full path stays in the tooltip;
 * - anything else: [describeFilterValue], repeated in the tooltip, as the pill truncates it.
 *
 * Without a value the tooltip is the field's description.
 */
export function filterPillContent(
  value: MagicFilterEntryValue | undefined,
  element: FilterElement | undefined,
  label: string,
  ctx: FormatContext
): FilterPillContent {
  // Alphabetical, not in the order picked, so a long list can be scanned for a name.
  const labels = filterValueLabels(value, element)?.sort((a, b) =>
    compareText(a, b, ctx)
  );
  if (labels && labels.length > 1) {
    return {
      text: labels.slice(0, PILL_NAMED_PICKS).join(", "),
      more: Math.max(0, labels.length - PILL_NAMED_PICKS),
      tooltip: `${label}:\n${labels.join("\n")}`,
      tooltipPlain: true,
    };
  }
  const valueText = describeFilterValue(value, element, ctx);
  const isTask =
    element?.filterType === "OBJECT" && element.autoCompletion?.type === "TASK";
  return {
    text: isTask ? taskLeafOf(valueText) : valueText,
    more: 0,
    tooltip: valueText ? `${label}: ${valueText}` : element?.tooltip,
    tooltipPlain: false,
  };
}

/** The last segment of a " | "-joined task path — the task itself, without its ancestors. */
function taskLeafOf(path: string): string {
  const segments = path.split(" | ");
  return segments[segments.length - 1] || path;
}
