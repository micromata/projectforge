"use client";

import { useEffect, useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import {
  DataTable,
  DataTableColumnPanel,
  selectionColumn,
  useColumnStatePersistence,
  useDataTable,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";
import { SearchInput } from "@/components/shared/list/search-input";
import { SelectionModeToggle } from "@/components/shared/list/selection-mode-toggle";
import { useFormatContext } from "@/hooks/use-format";
import type { LogGroupEntry } from "@/lib/rs/admin-errors";
import { adminErrorsColumns } from "./admin-errors-columns";
import { AdminErrorsSelectionBar } from "./admin-errors-selection-bar";
import { searchAdminErrors } from "./admin-errors-search";
import { useProblemSelection } from "./use-problem-selection";

/** The user prefs of the table (`AdminErrorsRest.columnStates` / `setColumnStates`). */
const GRID = "adminErrors";

interface Props {
  entries: LogGroupEntry[];
  isFetching: boolean;
  search: string;
  onSearchChange: (search: string) => void;
  onOpen: (entry: LogGroupEntry) => void;
}

/**
 * The problems as a table like every list: searchable over all its texts, sortable and filterable per column,
 * its columns reorderable, pinnable and hideable, stored in the user's prefs. Rendered once the stored state
 * has arrived (or failed), so the columns don't jump from the default layout to the user's one. In selection mode
 * many problems are picked and changed in their status at once (see AdminErrorsSelectionBar).
 */
export function AdminErrorsTable(props: Props) {
  const stored = useStoredColumnState(GRID);
  if (stored.isPending) return null;
  return <LoadedTable {...props} storedState={stored.data ?? {}} />;
}

function LoadedTable({
  entries,
  isFetching,
  search,
  onSearchChange,
  onOpen,
  storedState,
}: Props & { storedState: ColumnState }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const picking = useProblemSelection();
  const selecting = picking.active;
  const columns = useMemo(() => {
    const declared = adminErrorsColumns(t, ctx);
    return selecting
      ? [
          selectionColumn<LogGroupEntry>() as ColumnDef<LogGroupEntry, unknown>,
          ...declared,
        ]
      : declared;
  }, [t, ctx, selecting]);
  const searched = useMemo(
    () => searchAdminErrors(entries, search, t),
    [entries, search, t]
  );
  const state = useTableState({ restoredState: storedState });
  const table = useDataTable<LogGroupEntry>({
    columns,
    data: searched,
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
    ...picking.tableOptions,
    enableColumnFilters: true,
    enableColumnResizing: true,
    // Never paged: the server caps the list (see the "more" hint above the table).
    manualPagination: true,
    getRowId: (row) => String(row.id),
    highlight: search,
  });
  const { tableRef } = picking;
  useEffect(() => {
    tableRef.current = table;
  }, [tableRef, table]);
  // Only the picked problems still shown: a search, a column filter or a refetch may have dropped some of them.
  const selectedIds = selecting
    ? table.getFilteredSelectedRowModel().rows.map((row) => row.original.id)
    : [];
  useColumnStatePersistence(GRID, {
    sorting: state.sorting,
    columnVisibility: state.columnVisibility,
    columnPinning: state.columnPinning,
    columnSizing: state.columnSizing,
    columnOrder: state.columnOrder,
  });
  // Back to the columns as declared; the persistence then stores the empty state.
  const resetColumns = () => {
    state.setSorting([]);
    state.setColumnVisibility({});
    state.setColumnPinning({});
    state.setColumnSizing({});
    state.setColumnOrder([]);
    state.setColumnFilters([]);
  };

  return (
    <>
      <div className="flex items-center gap-2 pb-2">
        <div className="relative w-full max-w-md">
          <SearchInput value={search} onChange={onSearchChange} />
        </div>
        <DataTableColumnPanel table={table} onReset={resetColumns} />
        <SelectionModeToggle active={selecting} onToggle={picking.toggle} />
      </div>
      {selecting && (
        <AdminErrorsSelectionBar
          ids={selectedIds}
          onSelectAll={picking.selectAll}
          onClear={picking.selection.clear}
          onLeave={picking.leave}
        />
      )}
      <DataTable<LogGroupEntry>
        table={table}
        columns={[]}
        data={[]}
        isFetching={isFetching}
        showPagination={false}
        dense
        emptyState={t("nothingFound")}
        // In selection mode a click picks the row instead of opening it.
        selection={selecting ? picking.selection : undefined}
        onRowClick={onOpen}
      />
    </>
  );
}
