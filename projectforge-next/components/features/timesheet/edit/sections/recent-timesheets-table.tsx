"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import {
  DataTable,
  DataTableColumnPanel,
  useColumnStatePersistence,
  useDataTable,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";
import { Input } from "@/components/ui/input";
import type { TimesheetDetail } from "../../types";
import { recentColumns, searchText } from "./recent-timesheets-columns";

/**
 * The user prefs of the table's layout (`TimesheetEntityRest.getRecentColumnStates` /
 * `updateRecentColumnStates`), apart from the time sheet list's own.
 */
const GRID = "timesheet/recentList";

interface RecentTableProps {
  entries: TimesheetDetail[];
  /** Whether the cost unit, its customer and project are shown (see recentColumns). */
  cost2Visible: boolean;
  onSelect: (entry: TimesheetDetail) => void;
}

/**
 * The content of RecentTimesheetsPopover: the search, the column panel and the grid of the recent entries.
 * Rendered once the stored layout has arrived (or failed), so the columns don't jump from the default
 * layout to the user's one.
 */
export function RecentTimesheetsTable(props: RecentTableProps) {
  const stored = useStoredColumnState(GRID);
  if (stored.isPending) return null;
  return <LoadedTable {...props} storedState={stored.data ?? {}} />;
}

function LoadedTable({
  entries,
  cost2Visible,
  onSelect,
  storedState,
}: RecentTableProps & { storedState: ColumnState }) {
  const t = useTranslations();
  const [query, setQuery] = useState("");
  const columns = useMemo(
    () => recentColumns(t, cost2Visible),
    [t, cost2Visible]
  );

  // Filtered over the same fields the row shows, so what a reader types matches what they read.
  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return entries;
    return entries.filter((entry) => searchText(entry).includes(needle));
  }, [entries, query]);

  const state = useTableState({ restoredState: storedState });
  const table = useDataTable<TimesheetDetail>({
    columns,
    data: filtered,
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
    manualPagination: true,
    getRowId: (entry) => String(entry.counter ?? searchText(entry)),
    highlight: query,
  });
  useColumnStatePersistence(GRID, {
    sorting: state.sorting,
    columnVisibility: state.columnVisibility,
    columnPinning: state.columnPinning,
    columnSizing: state.columnSizing,
    columnOrder: state.columnOrder,
  });
  // Back to the columns as declared — the recency order, unsorted; the persistence then stores the empty state.
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
      <div className="flex shrink-0 items-center gap-2 border-b px-3 py-2">
        <HugeiconsIcon
          icon={Search01Icon}
          size={14}
          aria-hidden
          className="shrink-0 text-muted-foreground"
        />
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder={t("search._")}
          aria-label={t("search._")}
          className="h-7 border-0 p-0 shadow-none focus-visible:ring-0"
        />
        <DataTableColumnPanel table={table} onReset={resetColumns} />
      </div>
      <div className="flex min-h-0 flex-1 flex-col">
        <DataTable<TimesheetDetail>
          table={table}
          columns={[]}
          data={[]}
          showPagination={false}
          dense
          emptyState={t("nothingFound")}
          onRowClick={onSelect}
        />
      </div>
    </>
  );
}
