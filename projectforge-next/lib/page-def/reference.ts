/**
 * The flags a referenced domain object carries that say it is no longer live. Every DTO carries both
 * (via `BaseDTO.copyFromMinimal`/`copyFrom`): `deleted`, and the uniform `deactivated` each DO defines
 * for itself (a user when its account is deactivated, an employee three months after leaving). Add a
 * further flag here as the concept grows, and every reference column picks it up.
 */
export interface InactiveFlags {
  deleted?: boolean;
  deactivated?: boolean;
}

/**
 * Whether a referenced object should read as "no longer live": deleted, or otherwise deactivated. The
 * one predicate behind the strikethrough a list column shows for a reference — applied automatically in
 * `use-declared-columns` to the object found under the column's key (see `ColumnBase.referenceKey`).
 */
export function isInactiveRef(ref?: InactiveFlags | null): boolean {
  return !!ref && (ref.deleted === true || ref.deactivated === true);
}

/** How a deleted reference reads: the red-pencil strike of a deleted row (`.row-deleted` in globals.css). */
export const DELETED_REF_CLASS =
  "line-through decoration-destructive decoration-2";

/**
 * How a deactivated reference reads (an ended project, a deactivated user): struck through thin and grey on
 * dimmed text, as an inactive row (`.row-inactive` in globals.css) — quieter than a deleted one.
 */
export const DEACTIVATED_REF_CLASS =
  "line-through decoration-muted-foreground decoration-1 text-muted-foreground";

/**
 * The class a no longer live reference is shown with — in a list cell as in a selection box, its chips,
 * its picked value and its hits alike; undefined for a live one. Deleted wins over deactivated.
 */
export function inactiveRefClass(
  ref?: InactiveFlags | null
): string | undefined {
  if (ref?.deleted === true) return DELETED_REF_CLASS;
  if (ref?.deactivated === true) return DEACTIVATED_REF_CLASS;
  return undefined;
}
