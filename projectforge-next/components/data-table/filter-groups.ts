import type { FilterElement } from "@/lib/rs/types";
import type { FilterValues } from "./filter-value";

/** Ids of the groups the client makes up; every other group id is `group:${label}`. */
export const ACTIVE_GROUP_ID = "active";
export const PLAIN_GROUP_ID = "plain";
export const TECHNICAL_GROUP_ID = "technical";

export interface FilterFieldGroup {
  id: string;
  /** null for the three groups above — the caller translates those headings. */
  groupLabel: string | null;
  elements: FilterElement[];
}

/**
 * The flag every list can be filtered by, whatever the entity is (`LayoutListFilterUtils` adds it to
 * every one of them).
 */
export const DELETED_FILTER_ID = "deleted";

/**
 * The same fields with `deleted` moved to the front.
 *
 * The backend hands its filter fields over sorted by label, which buries this one under G („gelöscht")
 * or D — somewhere in the middle of the entity's own properties. It belongs at the top: „is this entry
 * deleted" is asked of every list and far more often than any single field of one entity, and it is the
 * only way to see an entry that the default filter hides.
 */
export function hoistDeletedFilter(elements: FilterElement[]): FilterElement[] {
  const index = elements.findIndex(
    (element) => element.id === DELETED_FILTER_ID
  );
  if (index <= 0) return elements;
  const hoisted = elements[index];
  return [hoisted, ...elements.filter((_, i) => i !== index)];
}

/** The parents of a field, from the backend or — for an older one — off its label. */
export function groupLabelOf(element: FilterElement): string | null {
  if (element.group) return element.group;
  const label = element.label;
  if (!label) return null;
  const separator = label.lastIndexOf(" - ");
  return separator > 0 ? label.slice(0, separator) : null;
}

/**
 * Whether the field is index plumbing rather than a question a user asks (`attachmentsIds`).
 *
 * The fallback is exact for a backend that doesn't send `technical`: LayoutListFilterUtils presets
 * `label = id` and only overwrites it from a translation, so label === id means "no @PropertyInfo".
 */
export function isTechnicalField(element: FilterElement): boolean {
  if (element.technical !== undefined) return element.technical;
  return !element.label || element.label === element.id;
}

/** What a field is called inside its group's heading: "Name", not "Kunde - Name". */
export function fieldLabelInGroup(element: FilterElement): string {
  if (element.shortLabel) return element.shortLabel;
  const label = element.label;
  if (!label) return element.id;
  const group = groupLabelOf(element);
  return group && label.startsWith(`${group} - `)
    ? label.slice(group.length + 3)
    : label;
}

/**
 * How tall the field's input is, so a group can put the flat ones first and leave its ragged
 * bottom edge to the last row (see [FilterFieldGroup]'s grid).
 */
export function controlRankOf(element: FilterElement): number {
  switch (element.filterType) {
    case "BOOLEAN":
      return 0;
    case "DATE":
      return 2;
    case "TIMESTAMP":
      return 3;
    default:
      return 1;
  }
}

/**
 * The filter fields of a list as the groups the "all filters" dialog shows, in order: the filters
 * in play, the entity's own fields, one group per nested entity, and the technical ones last.
 *
 * A field appears in exactly one group. The active ones are taken out of their own group rather than
 * repeated there: the same field twice, once filled and once empty, reads as if an entry didn't take.
 *
 * Expects the fields in backend order (sorted by their full label), which keeps the members and the
 * groups themselves in a stable order without sorting again here.
 */
export function buildFilterGroups(
  elements: FilterElement[],
  values: FilterValues
): FilterFieldGroup[] {
  const active: FilterElement[] = [];
  const plain: FilterElement[] = [];
  const technical: FilterElement[] = [];
  const named = new Map<string, FilterElement[]>();

  for (const element of elements) {
    if (element.defaultFilter || element.id in values) {
      active.push(element);
      continue;
    }
    if (isTechnicalField(element)) {
      technical.push(element);
      continue;
    }
    const group = groupLabelOf(element);
    if (!group) {
      plain.push(element);
      continue;
    }
    const members = named.get(group);
    if (members) members.push(element);
    else named.set(group, [element]);
  }

  const groups: FilterFieldGroup[] = [];
  const add = (
    id: string,
    groupLabel: string | null,
    members: FilterElement[]
  ) => {
    if (members.length) groups.push({ id, groupLabel, elements: members });
  };

  add(ACTIVE_GROUP_ID, null, active);
  // Leading the entity's own fields, i.e. directly under the filters in play — see hoistDeletedFilter.
  add(PLAIN_GROUP_ID, null, hoistDeletedFilter(plain));
  for (const [group, members] of named) {
    add(`group:${group}`, group, members);
  }
  add(TECHNICAL_GROUP_ID, null, technical);
  return groups;
}

/** Whether any of the texts contains the search term; an empty term matches everything. */
export function matchesSearchTerm(
  term: string,
  ...texts: (string | undefined)[]
): boolean {
  const needle = term.trim().toLowerCase();
  if (!needle) return true;
  return texts.some((text) => text?.toLowerCase().includes(needle));
}

/**
 * The texts a field can be found by. Its raw id is one of them, which is what keeps a field like
 * `attachmentsIds` findable at all — the same reason [FilterFieldList] matches on it.
 */
export function searchTextsOf(element: FilterElement): (string | undefined)[] {
  return [element.label, element.shortLabel, element.group, element.id];
}

/**
 * The groups reduced to what matches the search term: the whole group where its heading matches,
 * otherwise the fields that do. Empty groups drop out.
 */
export function filterGroupsBySearch(
  groups: FilterFieldGroup[],
  term: string
): FilterFieldGroup[] {
  if (!term.trim()) return groups;
  const result: FilterFieldGroup[] = [];
  for (const group of groups) {
    if (matchesSearchTerm(term, group.groupLabel ?? undefined)) {
      result.push(group);
      continue;
    }
    const elements = group.elements.filter((element) =>
      matchesSearchTerm(term, ...searchTextsOf(element))
    );
    if (elements.length) result.push({ ...group, elements });
  }
  return result;
}
