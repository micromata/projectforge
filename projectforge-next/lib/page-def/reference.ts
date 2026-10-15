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
