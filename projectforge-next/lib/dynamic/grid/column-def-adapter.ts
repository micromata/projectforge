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
export type AdaptedColumn<TRow extends DataObject = DataObject> = Omit<
  AccessorFnColumnDef<TRow, unknown>,
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
 * and `autoHeight` (implied by `wrapText`, which is kept — see ColumnMeta.wrap), and
 * the grid-wide locale/format block (those values come from `useFormatContext()`,
 * so there is one source rather than two).
 */
/**
 * @param TRow the row type of the table the columns are for. Generic because a hand-built page knows
 *   its rows (the structure tree's `TaskNode`) while a dynamic layout doesn't — the accessors read by
 *   path either way, so nothing but the type varies.
 */
export function adaptColumnDefs<TRow extends DataObject = DataObject>(
  grid: AgGridNode
): AdaptedColumn<TRow>[] {
  return (grid.columnDefs ?? [])
    .map((col) => adaptColumn<TRow>(col))
    .filter((column): column is AdaptedColumn<TRow> => column !== null);
}

function adaptColumn<TRow extends DataObject>(
  col: AgGridColumnDef
): AdaptedColumn<TRow> | null {
  const id = col.field;
  if (!id) return null;

  // valueGetter overrides the field, valueFormatter names a sibling field holding
  // the pre-rendered text (e.g. "sizeHumanReadable" next to "size").
  const path = parseValuePath(col.valueGetter ?? col.valueFormatter) ?? id;
  const filterKind = filterKindOf(col);
  const spec = cellSpecFor(col);

  return {
    id,
    accessorFn: (row: TRow) => getByPath(row, path),
    size: col.width,
    minSize: col.minWidth,
    maxSize: col.maxWidth,
    enableSorting: col.sortable ?? false,
    enableResizing: col.resizable ?? true,
    enableColumnFilter: !!filterKind,
    // `lockPosition` (UIAgGridColumnDef.pinnedAndLocked) means the column holds its
    // place: the backend pins it and skips it when restoring order and pinning from
    // the user's prefs. So neither may the user move it — the structure tree's
    // "title" column is the case this exists for.
    enableHiding: !col.lockPosition,
    enablePinning: !col.lockPosition,
    meta: {
      // The label is translated in the hook; the raw key would be unreadable here.
      label: col.headerName,
      align: spec.align,
      cellSpec: spec,
      filterKind,
      headerTooltip: col.headerTooltip,
      pinnedLocked: !!col.lockPosition,
      // Intent, not layout engine: the backend says this value is a sentence or a list and belongs on
      // several lines (a group's description and its members, see UIAgGrid.add).
      wrap: col.wrapText,
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
