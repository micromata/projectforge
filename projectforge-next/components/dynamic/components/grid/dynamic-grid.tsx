"use client";

import { useMemo, useState } from "react";
import { usePathname } from "next/navigation";
import type { PaginationState } from "@tanstack/react-table";
import {
  DataTable,
  DataTableColumnPanel,
  DEFAULT_PAGE_SIZE,
  PAGE_SIZE_OPTIONS,
  useColumnStatePersistenceByUrl,
  useDataTable,
  useTableState,
} from "@/components/data-table";
import type { DataObject } from "@/lib/dynamic/path";
import type { AgGridNode } from "@/lib/dynamic/grid/ag-grid-types";
import { initialStateFrom } from "@/lib/dynamic/grid/initial-state";
import { rowClassNameFor } from "@/lib/dynamic/grid/row-class";
import { rowClickTargetFor } from "@/lib/dynamic/grid/row-click";
import { resolveRestUrl } from "@/lib/dynamic/response-action";
import { useDynamicLayout } from "../../dynamic-context";
import type { DynamicComponentProps } from "../../dynamic-renderer";
import { DynamicGridFallback } from "./dynamic-grid-fallback";
import { useDynamicGridColumns } from "./use-dynamic-grid-columns";
import { useGridStateReset } from "@/components/data-table";

/**
 * Renders an AG_GRID / TABLE node of a UILayout with the app's DataTable.
 *
 * Everything the legacy grid did through AG-Grid options is translated in
 * `lib/dynamic/grid/`: the column defs, the initial (already restored) column
 * state, the row highlight classes and the row-click target. A TABLE node without
 * `columnDefs` has none of that information and falls back to a plain table.
 */
export function DynamicGrid({ node }: DynamicComponentProps) {
  const grid = node as AgGridNode;
  if (!grid.columnDefs?.length) return <DynamicGridFallback node={node} />;
  return <Grid grid={grid} />;
}

function Grid({ grid }: { grid: AgGridNode }) {
  const { data, variables, callAction } = useDynamicLayout();
  const columns = useDynamicGridColumns(grid);
  const pathname = usePathname();

  // The rows live under the node's own id ("resultSet" for a list page), in data
  // or - for the pages that recompute their table - in variables.
  const rows = useMemo(() => {
    const key = grid.id ?? "resultSet";
    const slice = data[key] ?? variables[key];
    return Array.isArray(slice) ? (slice as DataObject[]) : [];
  }, [data, variables, grid.id]);

  // No extra request: the layout response already carries the user's restored
  // column state (see initialStateFrom), so it is available on the first render.
  const restoredState = useMemo(() => initialStateFrom(grid), [grid]);
  const state = useTableState({ restoredState });
  // Held here rather than inside useDataTable, so the selected size can go into the stored state below.
  // The layout already carries the user's size (AGGridSupport.prepareUIGrid4ListPage).
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: grid.paginationPageSize ?? DEFAULT_PAGE_SIZE,
  });
  const table = useDataTable<DataObject>({
    columns,
    data: rows,
    sorting: state.sorting,
    onSortingChange: state.setSorting,
    columnFilters: state.columnFilters,
    onColumnFiltersChange: state.setColumnFilters,
    columnVisibility: state.columnVisibility,
    onColumnVisibilityChange: state.setColumnVisibility,
    columnPinning: state.columnPinning,
    onColumnPinningChange: state.setColumnPinning,
    columnSizing: state.columnSizing,
    onColumnSizingChange: state.setColumnSizing,
    columnOrder: state.columnOrder,
    onColumnOrderChange: state.setColumnOrder,
    enableColumnFilters: true,
    enableColumnResizing: true,
    // The list endpoints answer with the whole result set, so sorting, filtering
    // and paging all happen on the client - as on the hand-built book list.
    manualSorting: false,
    pagination,
    onPaginationChange: setPagination,
    getRowId: (row, index) => String(row.id ?? index),
  });

  const suspendPersistence = useColumnStatePersistenceByUrl(
    grid.onColumnStatesChangedUrl &&
      resolveRestUrl(grid.onColumnStatesChangedUrl),
    {
      sorting: state.sorting,
      columnVisibility: state.columnVisibility,
      columnPinning: state.columnPinning,
      columnSizing: state.columnSizing,
      columnOrder: state.columnOrder,
      paginationPageSize: pagination.pageSize,
    }
  );

  const resetColumns = useGridStateReset(
    grid.resetGridStateUrl && resolveRestUrl(grid.resetGridStateUrl),
    state,
    suspendPersistence
  );

  const rowClassName = useMemo(
    () => rowClassNameFor(grid.getRowClass, grid.id),
    [grid.getRowClass, grid.id]
  );
  const clickable = !!(grid.rowClickRedirectUrl || grid.rowClickPostUrl);

  // The layout response *is* the ResultSet for these pages, so the row the user edited last sits
  // beside the rows — the same place the legacy React grid read it from. Scoped by the page's path,
  // which is the category for a list page, so the scroll happens once per session and not on every
  // visit (see useHighlightedRow).
  const highlightRowId =
    typeof data.highlightRowId === "number" ? data.highlightRowId : undefined;

  return (
    <div className="flex min-h-0 flex-1 flex-col gap-1">
      <div className="flex justify-end">
        <DataTableColumnPanel
          table={table}
          onReset={resetColumns}
          className="h-6 rounded-full px-2.5 text-xs"
        />
      </div>
      <DataTable<DataObject>
        table={table}
        columns={columns}
        data={rows}
        rowClassName={rowClassName}
        highlightRowId={highlightRowId}
        highlightScope={pathname}
        pageSizeOptions={grid.paginationPageSizeSelector ?? PAGE_SIZE_OPTIONS}
        onRowClick={
          clickable
            ? (row) => {
                const action = rowClickTargetFor(grid, row);
                if (action) void callAction(action);
              }
            : undefined
        }
        className="flex-1"
      />
    </div>
  );
}
