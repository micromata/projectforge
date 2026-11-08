import type { FilterElement } from "@/lib/rs/types";
import { isEmptyFilterValue, type FilterValues } from "./filter-value";

/**
 * The pinned checklists of the lists about orders, invoices, projects, cost units and time sheets:
 * business unit, customer and project (`BusinessUnitChecklistFilter`, `CustomerChecklistFilter`,
 * `ProjectChecklistFilter`, all `defaultFilter`, placed first by `addLeading`).
 *
 * Pinned, they would take three empty pills of room on every visit. Empty, they share one pill instead
 * ("Business Unit / Kunde / Projekt", see [PinnedFilterGroupPill]); a field gets a pill of its own once it
 * has a value or is being edited.
 *
 * Only the *presentation* is grouped, as with the change history (see [HISTORY_FILTER_FIELDS]): the values
 * stay keyed by these backend ids, so the request body and the saved favorites are unchanged by this.
 */
export const PINNED_FILTER_GROUP_IDS: readonly string[] = [
  "businessUnits",
  "customers",
  "projects",
];

/**
 * The members of the group that fold into the shared pill, in backend order: the ones without a value,
 * except those in `keepIds` (the pill being picked or edited, which has to stay on the row while its
 * popover is open).
 *
 * Folds only two or more: a single empty one is just as wide as the shared pill, and its own pill opens
 * the checklist at once instead of a menu with one entry. A list without business units (the element is
 * left out when none is configured) thus folds customer and project only.
 */
export function foldedPinnedFilters(
  elements: FilterElement[],
  values: FilterValues,
  keepIds: readonly (string | null)[]
): FilterElement[] {
  const folded = elements.filter(
    (element) =>
      PINNED_FILTER_GROUP_IDS.includes(element.id) &&
      isEmptyFilterValue(values[element.id]) &&
      !keepIds.includes(element.id)
  );
  return folded.length >= 2 ? folded : [];
}

/** Whether the field is one of the group, which may be removed (emptied, i.e. folded back) by its ×. */
export function isPinnedFilterGroupMember(element: FilterElement): boolean {
  return PINNED_FILTER_GROUP_IDS.includes(element.id);
}

/** What the shared pill reads: the labels of the folded fields, "Business Unit / Kunde / Projekt". */
export function foldedPinnedFiltersLabel(folded: FilterElement[]): string {
  return folded.map((element) => element.label ?? element.id).join(" / ");
}
