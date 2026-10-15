import type { FilterElement, ListMetaData } from "./types";

/**
 * The filter fields a list page offers, taken from its `listMeta`.
 *
 * The set is derived per entity from the DAO's search fields
 * (LayoutListFilterUtils.createNamedSearchFilterContainer), so it can't be
 * hard-coded in the frontend.
 *
 * The backend types the list as `UILabelledElement`, so an entry that isn't a
 * filter field is possible on the wire and is dropped here.
 */
export function filterElementsOf(
  meta: ListMetaData | undefined
): FilterElement[] {
  return (meta?.filterElements ?? []).filter(isFilterElement);
}

function isFilterElement(element: FilterElement): boolean {
  return element.type === "FILTER_ELEMENT" && typeof element.id === "string";
}
