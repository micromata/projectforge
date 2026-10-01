import {
  describeFilterValue,
  filterValuesFromEntries,
} from "@/components/data-table/filter-value";
import { HISTORY_FILTER_IDS } from "@/components/data-table/history-filter";
import { describeHistoryFilter } from "@/components/data-table/history-filter-summary";
import type { FormatContext } from "@/lib/format";
import type { FilterElement, MagicFilter } from "@/lib/rs/types";

/**
 * What a chart did with the list's filter, by field id — as the backend reports it where it can't apply
 * every criterion (the order book's forecast, see `OrderEntityRest.forecastFilterUsage`). A field in none
 * of the lists was applied as it is.
 */
export interface AppliedFilterUsage {
  /** Not applied at all. */
  ignored?: readonly string[];
  /** Replaced by a parameter of the chart; the summary's notes say by what. */
  replaced?: readonly string[];
  /** Applied only in part (the first of several values). */
  partial?: readonly string[];
}

/** One entry of the summary: a pill of the list, or the search string. */
export interface AppliedFilterItem {
  /** The field id, `history` for the three history fields, `searchString` for the search box. */
  key: string;
  label: string;
  /** Empty for a boolean pill, whose label alone says what is meant. */
  value: string;
  status: "applied" | "ignored" | "replaced" | "partial";
}

/** The key of the search string's entry; the three history fields share {@link HISTORY_ITEM_KEY}. */
export const SEARCH_ITEM_KEY = "searchString";
export const HISTORY_ITEM_KEY = "history";

/**
 * The entries of [filter] as the list's pills read them, in the order of the list's filter fields: the
 * label of the field and [describeFilterValue] for the value, the three history fields as one entry like
 * their combined pill. A stored entry the list no longer offers as a field is still part of the query,
 * so it is listed too (under its id). The search string comes last.
 */
export function appliedFilterItems(
  filter: MagicFilter | undefined,
  elements: readonly FilterElement[],
  usage: AppliedFilterUsage | undefined,
  labels: { history: string; search: string },
  ctx: FormatContext
): AppliedFilterItem[] {
  const values = filterValuesFromEntries(filter?.entries);
  const statusOf = (fields: readonly string[]): AppliedFilterItem["status"] => {
    const any = (list?: readonly string[]) =>
      fields.some((field) => list?.includes(field));
    if (any(usage?.ignored)) return "ignored";
    if (any(usage?.replaced)) return "replaced";
    if (any(usage?.partial)) return "partial";
    return "applied";
  };
  const items: AppliedFilterItem[] = [];
  const historyFields = HISTORY_FILTER_IDS.filter((id) => id in values);
  if (historyFields.length > 0) {
    items.push({
      key: HISTORY_ITEM_KEY,
      label: labels.history,
      value: describeHistoryFilter(values, ctx),
      status: statusOf(historyFields),
    });
  }
  const known = new Set(elements.map((element) => element.id));
  elements.forEach((element) => {
    if (!(element.id in values) || HISTORY_FILTER_IDS.includes(element.id))
      return;
    items.push({
      key: element.id,
      label: element.label ?? element.id,
      value: describeFilterValue(values[element.id], element, ctx),
      status: statusOf([element.id]),
    });
  });
  Object.keys(values)
    .filter((field) => !known.has(field) && !HISTORY_FILTER_IDS.includes(field))
    .forEach((field) =>
      items.push({
        key: field,
        label: field,
        value: describeFilterValue(values[field], undefined, ctx),
        status: statusOf([field]),
      })
    );
  if (filter?.searchString) {
    items.push({
      key: SEARCH_ITEM_KEY,
      label: labels.search,
      value: filter.searchString,
      status: "applied",
    });
  }
  return items;
}
