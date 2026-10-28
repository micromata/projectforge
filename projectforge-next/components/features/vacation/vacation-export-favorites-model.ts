/**
 * The comparisons behind the favorites of the team vacation calendar (useVacationExportFavorites): a favorite and
 * the choice on screen are both sets of users and groups, so order and duplicates mean nothing.
 */

/** The users and groups of a favorite or of the choice on screen. */
export interface VacationExportIds {
  userIds: number[];
  groupIds: number[];
}

/** Whether both hold the same ids, in whatever order and however often. */
export function sameIds(a: number[], b: number[]): boolean {
  const set = new Set(a);
  const other = new Set(b);
  return set.size === other.size && [...other].every((id) => set.has(id));
}

/** Whether the favorite holds exactly the choice. */
export function sameChoice(
  a: VacationExportIds,
  b: VacationExportIds
): boolean {
  return sameIds(a.userIds, b.userIds) && sameIds(a.groupIds, b.groupIds);
}

/** The id of the first favorite holding exactly the choice, null if none does. */
export function favoriteIdHolding<T extends VacationExportIds & { id: number }>(
  favorites: T[],
  choice: VacationExportIds
): number | null {
  return favorites.find((f) => sameChoice(f, choice))?.id ?? null;
}
