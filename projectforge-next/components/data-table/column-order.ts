import type {
  ColumnDef,
  ColumnOrderState,
  ColumnPinningState,
} from "@tanstack/react-table";

/**
 * The id TanStack gives a column def — its `id`, or the accessor key it derives one from. The same
 * rule the table itself applies, needed here because the ids are read before the table exists.
 */
export function columnIdOfDef<TData>(def: ColumnDef<TData, unknown>): string {
  if (def.id) return def.id;
  return "accessorKey" in def ? String(def.accessorKey) : "";
}

/**
 * The column order with the locked columns in front of everything, in the order they are given.
 *
 * A locked column is one whose position is not the user's to change — the selection checkbox, which
 * leads the row or is useless. It cannot be expressed as pinning alone: TanStack appends a column the
 * `columnOrder` does not name to the *end* (ColumnOrdering's "if there are any columns left, add them
 * to the end"), so for every user with a stored order from before the column existed it would render
 * last and off-screen. The legacy grid called this `UIAgGridColumnDef.lockPosition`.
 *
 * Applied at render time over the stored order, and the locked ids are stripped from it first — so a
 * stored order that wrongly holds one (written before it was locked) is corrected, and nothing has to
 * be migrated in what the user persisted.
 */
export function withLockedFirst(
  order: ColumnOrderState,
  lockedIds: string[],
  allIds: string[]
): ColumnOrderState {
  if (!lockedIds.length) return order;
  const base = order.length ? order : allIds;
  return [...lockedIds, ...base.filter((id) => !lockedIds.includes(id))];
}

/**
 * The column order with the pinned columns where they have to be: the left-pinned ones first, the
 * right-pinned ones last, each group in its *pinning* order.
 *
 * A pinned cell takes its sticky offset from the pinning order (`column.getStart("left")` sums the
 * widths of the left-pinned columns before it) while the table renders by `columnOrder`. Where the two
 * disagree, the pinned columns overlap — which is exactly what a stored order from before a column was
 * pinned does, and what the column panel keeps in step for the user's own pinning (see
 * DataTableColumnPanel.togglePin).
 *
 * @param order The order to fix up; empty means "as the columns are declared", hence `allIds`.
 */
export function withPinnedFirst(
  order: ColumnOrderState,
  pinning: ColumnPinningState,
  allIds: string[]
): ColumnOrderState {
  const left = pinning.left ?? [];
  const right = pinning.right ?? [];
  if (!left.length && !right.length) return order;
  const base = order.length ? order : allIds;
  const known = new Set(base);
  return [
    ...left.filter((id) => known.has(id)),
    ...base.filter((id) => !left.includes(id) && !right.includes(id)),
    ...right.filter((id) => known.has(id)),
  ];
}

/**
 * The stored column order reconciled with the columns the table actually declares: ids it doesn't
 * declare are dropped, and a declared column the order lacks is put after its declared predecessor
 * instead of at the end, where TanStack would append it.
 *
 * Both cases come from a stored order written for another set of columns: a column added later, or
 * the layout of the legacy React grid, which kept its state under the same category and with its own
 * column ids (`statusAsString` where the page now has `status`) — see PageRoutes. Applied at render
 * time like withLockedFirst, so nothing has to be migrated in what the user persisted.
 *
 * @param order The stored order; empty means "as the columns are declared" and is returned as is.
 */
export function withDeclaredColumns(
  order: ColumnOrderState,
  allIds: string[]
): ColumnOrderState {
  if (!order.length) return order;
  const declared = new Set(allIds);
  const result = order.filter((id) => declared.has(id));
  allIds.forEach((id, index) => {
    if (result.includes(id)) return;
    let at = 0;
    for (let i = index - 1; i >= 0; i--) {
      const position = result.indexOf(allIds[i]);
      if (position >= 0) {
        at = position + 1;
        break;
      }
    }
    result.splice(at, 0, id);
  });
  return result;
}
