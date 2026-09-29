"use client";

import { useCallback, useEffect, useMemo, useRef } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef, RowSelectionState } from "@tanstack/react-table";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  DataTable,
  selectionColumn,
  SELECTION_COLUMN_ID,
  useRowSelection,
} from "@/components/data-table";
import { ImportDiffCell } from "./import-diff-cell";
import { ImportStatusCell } from "./import-status-cell";
import {
  isSelectable,
  rowClassForStatus,
  visibleColumns,
} from "./import-model";
import type { ImportColumn, ImportConfig, ImportEntry } from "./import-types";

/** Large enough to hold any import on one page, so the preview never paginates away rows. */
const ALL_ROWS_PAGE_SIZE = 100_000;

interface Props {
  config: ImportConfig;
  entries: ImportEntry[];
  meta: Record<string, unknown>;
  selection: RowSelectionState;
  /**
   * A React state setter, not a plain sink: [useRowSelection] toggles ranges with functional updates,
   * so the caller must pass its `setSelection` (see useImport), not a `(next) => void` wrapper.
   */
  onSelectionChange: React.Dispatch<React.SetStateAction<RowSelectionState>>;
  isFetching?: boolean;
}

/**
 * The preview of an import, driven entirely by [ImportConfig]: a status column, a ticking column for the
 * importable rows and one column per configured field, each tinted by its row's reconciliation state.
 * Because the columns are the config's, this same table serves the incoming-invoice import today and the
 * address/banking imports later — nothing here knows the entity.
 */
export function ImportPreviewTable({
  config,
  entries,
  meta,
  selection,
  onSelectionChange,
  isFetching,
}: Props) {
  const t = useTranslations();
  const shown = useMemo(
    () => visibleColumns(config.columns, meta),
    [config.columns, meta]
  );

  // The same interaction the list's mass update uses (see useRowSelection): a click selects the row,
  // Ctrl/Cmd+click toggles it, Shift+click and Shift+Arrow extend a range, Space toggles the focused
  // row and the arrow keys walk them. All rows show on one page and nothing is sorted or filtered here,
  // so the displayed order is exactly `entries` — no need to read it back off the table instance.
  const selectableIdSet = useMemo(
    () =>
      new Set(
        entries
          .filter((entry) =>
            isSelectable(entry.status, config.selectableStatuses)
          )
          .map((entry) => String(entry.id))
      ),
    [entries, config.selectableStatuses]
  );
  // Kept stable (reads the latest entries off a ref) so DataTable's focus-first-row effect runs once,
  // not on every reconcile/refetch — which would re-steal focus and reset the keyboard cursor.
  const entriesRef = useRef(entries);
  useEffect(() => {
    entriesRef.current = entries;
  }, [entries]);
  const displayedRowIds = useCallback(
    () => entriesRef.current.map((entry) => String(entry.id)),
    []
  );
  const rowSelection = useRowSelection(
    displayedRowIds,
    { state: selection, setState: onSelectionChange },
    { canSelect: (rowId) => selectableIdSet.has(rowId) }
  );

  const columns = useMemo<ColumnDef<ImportEntry, unknown>[]>(() => {
    const statusColumn: ColumnDef<ImportEntry, unknown> = {
      id: "status",
      header: t("status"),
      size: 150,
      enableSorting: false,
      meta: { label: t("status"), wrap: true },
      cell: ({ row }) => <ImportStatusCell entry={row.original} />,
    };
    const fieldColumns = shown.map<ColumnDef<ImportEntry, unknown>>(
      (column: ImportColumn) => {
        // A backend key that is both a text and a namespace (e.g. calendar.month, parent of
        // calendar.month.april) is exported as `<key>._`; ask next-intl for the leaf, not the object.
        const label = t(leafKeyOf(column.headerKey, t.has));
        return {
          id: column.field,
          header: label,
          size: column.width ?? 140,
          enableSorting: false,
          meta: { label, wrap: true },
          cell: ({ row }) => (
            <ImportDiffCell entry={row.original} column={column} />
          ),
        };
      }
    );
    return [selectionColumn<ImportEntry>(), statusColumn, ...fieldColumns];
  }, [shown, t]);

  return (
    <DataTable<ImportEntry>
      columns={columns}
      data={entries}
      isFetching={isFetching}
      getRowId={(row) => String(row.id)}
      enableColumnFilters={false}
      manualSorting={false}
      showPagination={false}
      // No pagination bar here, so the table must show every parsed row rather than the DataTable's
      // default 50 (which would silently hide the rest, e.g. the aggregate row at the foot of a payroll
      // file). A fixed large page size keeps every row on the single page as the data grows after upload.
      initialPageSize={ALL_ROWS_PAGE_SIZE}
      lockedColumnIds={[SELECTION_COLUMN_ID]}
      enableRowSelection={(row) =>
        isSelectable(row.original.status, config.selectableStatuses)
      }
      // The checkbox column and `row.getIsSelected()` read the table's own selection state, while the
      // click/keyboard interaction is driven by the shared RowSelection — both point at the one state
      // the caller holds, so a tick from either shows in the other (as the list's mass update wires it).
      selection={rowSelection}
      rowSelection={rowSelection.state}
      onRowSelectionChange={rowSelection.setState}
      rowClassName={(row) => rowClassForStatus(row.status)}
      className="flex-1"
    />
  );
}
