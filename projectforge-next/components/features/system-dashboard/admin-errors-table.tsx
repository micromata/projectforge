"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import {
  DataTable,
  DataTableColumnPanel,
  useColumnStatePersistence,
  useDataTable,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";
import { SearchInput } from "@/components/shared/list/search-input";
import { useFormatContext } from "@/hooks/use-format";
import type { LogGroupEntry } from "@/lib/rs/admin-errors";
import { adminErrorsColumns } from "./admin-errors-columns";
import { searchAdminErrors } from "./admin-errors-search";

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
 * has arrived (or failed), so the columns don't jump from the default layout to the user's one.
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
  const columns = useMemo(() => adminErrorsColumns(t, ctx), [t, ctx]);
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
    enableColumnFilters: true,
    enableColumnResizing: true,
    // Never paged: the server caps the list (see the "more" hint above the table).
    manualPagination: true,
    getRowId: (row) => String(row.id),
    highlight: search,
  });
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
      </div>
      <DataTable<LogGroupEntry>
        table={table}
        columns={[]}
        data={[]}
        isFetching={isFetching}
        showPagination={false}
        dense
        emptyState={t("nothingFound")}
        onRowClick={onOpen}
      />
    </>
  );
}
