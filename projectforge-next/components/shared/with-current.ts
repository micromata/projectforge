import type { EntityRef } from "./entity-autocomplete";

/**
 * The entries found, with the current value on top if they lack it and the term is empty or part of its
 * name: the backend drops it once it is inactive (or it is beyond the first page), but it must stay
 * choosable.
 */
export function withCurrent<T extends EntityRef>(
  entries: T[],
  current: EntityRef | null | undefined,
  search: string
): T[] {
  if (!current || entries.some((entry) => entry.id === current.id)) {
    return entries;
  }
  const term = search.trim().toLowerCase();
  if (term && !current.displayName.toLowerCase().includes(term)) {
    return entries;
  }
  // Only the id and the name of it are known; a caller re-picking it keeps its value (EntityAutocomplete).
  return [current as T, ...entries];
}
