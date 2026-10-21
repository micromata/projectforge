/**
 * The row class of a deleted entity — tinted and struck through, wherever a table
 * shows one (`globals.css`).
 *
 * Here rather than at the call sites so no table spells the class name itself;
 * tables pass this as their `rowClassName`.
 */
export function deletedRowClass(row: {
  deleted?: boolean;
}): string | undefined {
  return row.deleted ? "row-deleted" : undefined;
}

/**
 * The row class of an entry no longer live but not deleted — an ended project, a cancelled invoice, an
 * inactive rule: struck through thin and grey, set apart from a deleted one (`globals.css`). A page returns
 * it from its `rowClassName`; a deleted row still reads as deleted, since [deletedRowClass] is asked first.
 */
export const INACTIVE_ROW_CLASS = "row-inactive";
