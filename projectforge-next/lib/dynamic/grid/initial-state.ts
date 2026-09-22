import type { ColumnState } from "@/components/data-table/use-table-state";
import type { AgGridNode } from "./ag-grid-types";

/**
 * Derives the table's starting state from the layout response.
 *
 * There is no extra request for the stored column state here: the backend already
 * folded it into the response. `AGGridSupport.restoreColumnsFromUserPref` reorders
 * `columnDefs` and applies the stored `hide`, `width` and `pinned` before sending,
 * and puts the stored sort into `sortModel`. So the layout answer *is* the restored
 * state, and it is available synchronously — no spinner, no second fetch.
 */
export function initialStateFrom(grid: AgGridNode): ColumnState {
  const columns = grid.columnDefs ?? [];
  const visibility: Record<string, boolean> = {};
  const sizing: Record<string, number> = {};
  const pinning: { left: string[]; right: string[] } = { left: [], right: [] };
  const order: string[] = [];

  for (const col of columns) {
    const id = col.field;
    if (!id) continue;
    order.push(id);
    // Only the exceptions are recorded: a `columnVisibility` entry per column
    // would make every column look explicitly configured to the column panel.
    if (col.hide) visibility[id] = false;
    if (col.width) sizing[id] = col.width;
    if (col.pinned === "left" || col.pinned === "right") {
      pinning[col.pinned].push(id);
    }
  }

  return {
    // "Everything visible" has to be {} rather than undefined: useTableState
    // falls back to its own initial value for a missing slice.
    columnVisibility: visibility,
    columnSizing: sizing,
    columnPinning: pinning,
    columnOrder: order,
    sorting: sortingFrom(grid),
  };
}

/**
 * `sortModel` → TanStack's `sorting`. Sorted by `sortIndex` on a copy: sorting the
 * response's own array in place (as the legacy app did) mutates the query cache.
 */
function sortingFrom(grid: AgGridNode) {
  return [...(grid.sortModel ?? [])]
    .sort((a, b) => (a.sortIndex ?? 0) - (b.sortIndex ?? 0))
    .map((entry) => ({
      id: entry.colId,
      desc: entry.sort?.toLowerCase() === "desc",
    }));
}
