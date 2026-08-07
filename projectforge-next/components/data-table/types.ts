import type { RowData } from "@tanstack/react-table";
import type { FilterKind } from "./column-filter-types";
import type { CellSpec } from "./cells/cell-types";

declare module "@tanstack/react-table" {
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
