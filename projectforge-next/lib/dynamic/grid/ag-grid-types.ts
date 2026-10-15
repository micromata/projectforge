/**
 * The wire shape of a table node in a UILayout, i.e. `UIAgGrid` and
 * `UIAgGridColumnDef` of projectforge-rest.
 *
 * These types live here rather than in `lib/rs/types.ts` because they describe
 * the layout protocol, not a REST resource — and because the AG-Grid vocabulary
 * they carry ("agTextColumnFilter", "cellRenderer", `cellRendererParams`) must
 * not leak past this folder. Everything downstream sees TanStack's ColumnDef and
 * our own CellSpec.
 */

/** UIAgGridColumnDef. Only the fields the adapter actually reads. */
export interface AgGridColumnDef {
  field?: string;
  headerName?: string;
  headerTooltip?: string;
  /** Defaults to false server-side, unlike AG-Grid's own default. */
  sortable?: boolean;
  /** true/false, or a filter name like "agNumberColumnFilter". */
  filter?: boolean | string;
  /** A dot path in practice, never JavaScript — see parseValuePath. */
  valueGetter?: string;
  /** "numericColumn" | "rightAligned" — both mean right-aligned. */
  type?: string;
  minWidth?: number;
  maxWidth?: number;
  width?: number;
  hide?: boolean;
  resizable?: boolean;
  /** Names a sibling field holding the pre-rendered text (again a dot path). */
  valueFormatter?: string;
  /** "formatter" for the formatter dispatch, or a custom renderer's name. */
  cellRenderer?: string;
  cellRendererParams?: {
    /** The Formatter enum's name, e.g. "CURRENCY". */
    dataType?: string;
    /** value → UIIconType, e.g. `{"true": ["far", "star"]}`. */
    valueIconMap?: Record<string, unknown>;
    [param: string]: unknown;
  };
  pinned?: "left" | "right" | null;
  tooltipField?: string;
  /** Locks the column against being moved or unpinned. */
  lockPosition?: "left" | "right";
  wrapText?: boolean;
}

/** UIAgGrid's sortModel entry (rest/core/aggrid/SortModelEntry.kt). */
export interface AgGridSortModelEntry {
  colId: string;
  sort?: string;
  sortIndex?: number;
}

/** UIAgGrid. Again only what the adapter reads. */
export interface AgGridNode {
  id?: string;
  columnDefs?: AgGridColumnDef[];
  sortModel?: AgGridSortModelEntry[];
  rowClickPostUrl?: string;
  rowClickRedirectUrl?: string;
  rowClickOpenModal?: boolean;
  /** A JavaScript function name — deliberately ignored, see rowClickTargetFor. */
  rowClickFunction?: string;
  /** A JavaScript source string — translated by row-class.ts, never executed. */
  getRowClass?: string;
  onColumnStatesChangedUrl?: string;
  resetGridStateUrl?: string;
  paginationPageSize?: number;
  /** The page sizes this grid offers (UIAgGrid.paginationPageSizeSelector). */
  paginationPageSizeSelector?: number[];
}
