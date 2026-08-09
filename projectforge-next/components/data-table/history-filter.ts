import type { FilterElement } from "@/lib/rs/types";
import { isEmptyFilterValue, type FilterValues } from "./filter-value";

/**
 * The three filter fields every historizable entity carries, grouped into one.
 *
 * `LayoutListFilterUtils.createNamedSearchFilterContainer` adds them to every list layout: who
 * changed the entity, when, and which value the change history holds. They are three fields on the
 * wire but one question to ask — "what was modified, by whom, when" — so the UI shows one pill for
 * them, as Wicket's "Änderungszeitraum" fieldset does.
 *
 * Only the *presentation* is grouped. `FilterValues` stays keyed by the real backend ids below, so
 * the request body and the saved favorites are unchanged by this.
 */
export const HISTORY_FILTER_FIELDS = {
  /** OBJECT, autocompletes against `user/autosearch`; the backend reads `value.id`. */
  user: "modifiedByUser",
  /** TIMESTAMP, half-open; the backend reads `value.from` / `value.to`. */
  interval: "modifiedInterval",
  /** STRING, matched against the history's `oldValue`; the backend reads `value.value`. */
  value: "historySearch",
} as const;

export const HISTORY_FILTER_IDS: string[] = Object.values(
  HISTORY_FILTER_FIELDS
);

/**
 * Id of the group in the picker and in the open/pending pill state. Not a backend field — no
 * `FilterValues` key is ever named this.
 */
export const HISTORY_FILTER_GROUP_ID = "historyFilter";

/** The elements of the group that this layout actually carries. */
export interface HistoryFilterGroup {
  user?: FilterElement;
  interval?: FilterElement;
  value?: FilterElement;
}

/**
 * Picks the three history elements out of a layout's filter fields, or null when it has none of
 * them — a non-historizable entity, in which case nothing of this is shown at all.
 *
 * A layout carrying only some of the three yields a group with only those, so a backend that stops
 * sending one degrades to a smaller pill instead of an empty one.
 */
export function historyFilterGroupOf(
  elements: FilterElement[]
): HistoryFilterGroup | null {
  const group: HistoryFilterGroup = {};
  for (const [part, id] of Object.entries(HISTORY_FILTER_FIELDS)) {
    const element = elements.find((candidate) => candidate.id === id);
    if (element) group[part as keyof HistoryFilterGroup] = element;
  }
  return Object.keys(group).length ? group : null;
}

/** The other filter fields, which keep their own pill. */
export function withoutHistoryFilters(
  elements: FilterElement[]
): FilterElement[] {
  return elements.filter((element) => !HISTORY_FILTER_IDS.includes(element.id));
}

/** Whether any of the three narrows the list, i.e. whether the group pill counts as filled. */
export function historyFilterActive(values: FilterValues): boolean {
  return HISTORY_FILTER_IDS.some((id) => !isEmptyFilterValue(values[id]));
}

/** Just the three, as the group pill's draft starts out. */
export function pickHistoryFilters(values: FilterValues): FilterValues {
  const draft: FilterValues = {};
  for (const id of HISTORY_FILTER_IDS) {
    if (!isEmptyFilterValue(values[id])) draft[id] = values[id];
  }
  return draft;
}

/** Replaces exactly the three, leaving every other filter untouched. */
export function mergeHistoryFilters(
  values: FilterValues,
  draft: FilterValues
): FilterValues {
  const next = clearHistoryFilters(values);
  for (const id of HISTORY_FILTER_IDS) {
    if (!isEmptyFilterValue(draft[id])) next[id] = draft[id];
  }
  return next;
}

/** Drops all three at once — what the group pill's remove button does. */
export function clearHistoryFilters(values: FilterValues): FilterValues {
  const next = { ...values };
  for (const id of HISTORY_FILTER_IDS) delete next[id];
  return next;
}
