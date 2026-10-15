import type { RowData } from "@tanstack/react-table";
import type { FilterKind } from "./column-filter-types";
import type { CellSpec } from "./cells/cell-types";

declare module "@tanstack/react-table" {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- required by the module's signature
  interface TableMeta<TData extends RowData> {
    /**
     * The active search term, highlighted wherever it matched in a cell's text (see HighlightedText
     * and renderCell). Set once on the table (see useDataTable), so every cell of every DataTable
     * reads the same term without each column builder threading it through.
     */
    highlight?: string;
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- required by the module's signature
  interface ColumnMeta<TData extends RowData, TValue> {
    /**
     * Plain-text column name. `header` renders a component (sort button, filter
     * popover), so it can't be reused where only text works — the column panel,
     * aria labels, an export.
     */
    label?: string;
    /** Right-align numeric columns. */
    align?: "left" | "right";
    /**
     * How the cell renders its value. Set by the dynamic (UILayout) adapter;
     * hand-built columns write their `cell` directly and leave this empty.
     */
    cellSpec?: CellSpec;
    /** Which filter UI the header offers. */
    filterKind?: FilterKind;
    /**
     * The cell wraps its text over several lines instead of clipping it, and the row grows with it
     * (AG-Grid's `wrapText`, which implies its `autoHeight` — see UIAgGrid.add).
     *
     * For a column whose value is a sentence or a list rather than a name: a group's description and
     * its members are what the legacy list wraps. Only where the declaration asks for it, because a
     * wrapped column makes every row as tall as its longest cell.
     */
    wrap?: boolean;
    /** Tooltip of the header itself (UIAgGridColumnDef.headerTooltip). */
    headerTooltip?: string;
    /**
     * The column is pinned by the layout, not by the user, and must not be
     * unpinned or dragged (AG-Grid's `lockPosition`).
     */
    pinnedLocked?: boolean;
  }
}

export {};
