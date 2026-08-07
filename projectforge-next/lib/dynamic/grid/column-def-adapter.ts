import type { AccessorFnColumnDef } from "@tanstack/react-table";
import type { FilterKind } from "@/components/data-table/column-filter-types";
import { getByPath, type DataObject } from "@/lib/dynamic/path";
import type { AgGridColumnDef, AgGridNode } from "./ag-grid-types";
import { cellSpecFor } from "./cell-spec";
import { parseValuePath } from "./value-path";

/**
 * A column def without its rendered parts. `header` and `cell` need JSX and the
 * translation context, so `use-dynamic-grid-columns.tsx` adds them — everything
 * that is pure data mapping happens here.
 */
export type AdaptedColumn = Omit<
  AccessorFnColumnDef<DataObject, unknown>,
  "header" | "cell" | "id"
> & { id: string };

/** AG-Grid filter name → the filter UI our column header offers. */
const FILTER_KINDS: Record<string, FilterKind> = {
  agNumberColumnFilter: "number",
  agDateColumnFilter: "date",
  agTextColumnFilter: "text",
  agSetColumnFilter: "text",
};

/**
 * Maps a layout's `columnDefs` onto TanStack column defs.
 *
 * Deliberately dropped, because each encodes an AG-Grid or FontAwesome
 * implementation detail rather than intent: `headerClass` (a CSS contract for an
 * icon-only header — the adapter uses `headerTooltip` for the label instead),
 * `filterParams` (which buttons the AG-Grid filter panel shows), `suppressSizeToFit`
 * and `autoHeight`/`wrapText` (AG-Grid's own layout engine), and the grid-wide
 * locale/format block (those values come from `useFormatContext()`, so there is
 * one source rather than two).
 */
export function adaptColumnDefs(grid: AgGridNode): AdaptedColumn[] {
  return (grid.columnDefs ?? [])
    .map(adaptColumn)
    .filter((column): column is AdaptedColumn => column !== null);
}

function adaptColumn(col: AgGridColumnDef): AdaptedColumn | null {
  const id = col.field;
  if (!id) return null;

  // valueGetter overrides the field, valueFormatter names a sibling field holding
  // the pre-rendered text (e.g. "sizeHumanReadable" next to "size").
  const path = parseValuePath(col.valueGetter ?? col.valueFormatter) ?? id;
  const filterKind = filterKindOf(col);
  const spec = cellSpecFor(col);

  return {
    id,
    accessorFn: (row: DataObject) => getByPath(row, path),
    size: col.width,
    minSize: col.minWidth,
    maxSize: col.maxWidth,
    enableSorting: col.sortable ?? false,
    enableResizing: col.resizable ?? true,
    enableColumnFilter: !!filterKind,
    meta: {
      // The label is translated in the hook; the raw key would be unreadable here.
      label: col.headerName,
      align: spec.align,
      cellSpec: spec,
      filterKind,
      headerTooltip: col.headerTooltip,
      pinnedLocked: !!col.lockPosition,
    },
  };
}

/**
 * `filter` is either a boolean or an AG-Grid filter name. `true` means "the
 * backend didn't say which kind", and a text filter is the only one that works on
 * any value.
 */
function filterKindOf(col: AgGridColumnDef): FilterKind | undefined {
  if (typeof col.filter === "string") return FILTER_KINDS[col.filter] ?? "text";
  return col.filter ? "text" : undefined;
}
