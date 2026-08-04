import type {
  DynamicLayoutNode,
  DynamicUIResponse,
  FilterElement,
} from "./types";

/** Container id the backend uses for a list page's filter fields. */
const SEARCH_FILTER_CONTAINER = "searchFilter";

/**
 * The filter fields a list page offers, read out of the layout's `searchFilter`
 * container.
 *
 * The set is derived per entity from the DAO's search fields
 * (LayoutListFilterUtils.createNamedSearchFilterContainer), so it can't be
 * hard-coded in the frontend.
 */
export function filterElementsOf(ui: DynamicUIResponse | undefined): FilterElement[] {
  const container = ui?.namedContainers?.find(
    (c) => c.id === SEARCH_FILTER_CONTAINER
  );
  return (container?.content ?? []).filter(isFilterElement);
}

function isFilterElement(node: DynamicLayoutNode): node is DynamicLayoutNode &
  FilterElement {
  return node.type === "FILTER_ELEMENT" && typeof node.id === "string";
}
