"use client";

import { PageShell } from "@/components/shared/page-shell";
import { ListPageShell } from "@/components/shared/list-page-shell";
import {
  DataTable,
  DataTableColumnPanel,
  useColumnStatePersistence,
  useDataTable,
  useMagicFilterQuery,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";
import { useBooksColumns } from "@/components/features/books/books-columns";
import { BookRowActions } from "@/components/features/books/book-row-actions";
import { BooksToolbar } from "@/components/features/books/books-toolbar";
import type { BookListRow } from "@/components/features/books/types";

const ENTITY = "book";

export default function BooksPage() {
  // Column layout is stored server-side per entity, so it follows the user across
  // devices (see AbstractPagesRest.columnStates). The table only mounts once it
  // has arrived: the state seeds TanStack's initial state, which can't be
  // replaced afterwards without fighting the user's own edits.
  const stored = useStoredColumnState(ENTITY);

  if (stored.isPending) {
    return (
      <PageShell>
        <div className="flex flex-1 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
        </div>
      </PageShell>
    );
  }

  // A failed read is not worth blocking the page for — start from the defaults.
  return <BooksList storedState={stored.data ?? {}} />;
}

function BooksList({ storedState }: { storedState: ColumnState }) {
  const columns = useBooksColumns();
  const columnState = useTableState({
    restoredState: storedState,
    initialSorting: storedState.sorting,
  });

  const {
    data,
    rowCount,
    isLoading,
    isFetching,
    sorting,
    setSorting,
    pagination,
    setPagination,
    globalFilter,
    setGlobalFilter,
  } = useMagicFilterQuery<BookListRow>({
    entity: ENTITY,
    queryKey: ["books"],
    initialPageSize: 50,
    // Sorting drives the backend query, so it lives with the query, not in the
    // column state — the stored order seeds it here.
    initialSorting: storedState.sorting,
  });

  // Owned here so the toolbar's column panel and the table share one instance.
  const table = useDataTable<BookListRow>({
    columns,
    data,
    rowCount,
    sorting,
    onSortingChange: setSorting,
    pagination,
    onPaginationChange: setPagination,
    columnFilters: columnState.columnFilters,
    onColumnFiltersChange: columnState.setColumnFilters,
    columnVisibility: columnState.columnVisibility,
    onColumnVisibilityChange: columnState.setColumnVisibility,
    columnPinning: columnState.columnPinning,
    onColumnPinningChange: columnState.setColumnPinning,
    columnSizing: columnState.columnSizing,
    onColumnSizingChange: columnState.setColumnSizing,
    columnOrder: columnState.columnOrder,
    onColumnOrderChange: columnState.setColumnOrder,
    enableColumnFilters: true,
    enableColumnResizing: true,
    manualSorting: true,
    manualPagination: true,
    manualFiltering: true,
    getRowId: (row: BookListRow) => String(row.id),
  });

  useColumnStatePersistence(ENTITY, {
    sorting,
    columnVisibility: columnState.columnVisibility,
    columnPinning: columnState.columnPinning,
    columnSizing: columnState.columnSizing,
    columnOrder: columnState.columnOrder,
  });

  /** Back to the column defs' defaults; the next write stores the empty state. */
  function resetColumns() {
    setSorting([]);
    columnState.setColumnVisibility({});
    columnState.setColumnPinning({});
    columnState.setColumnSizing({});
    columnState.setColumnOrder([]);
    columnState.setColumnFilters([]);
  }

  return (
    <PageShell>
      <ListPageShell
        toolbar={
          <BooksToolbar
            search={globalFilter}
            onSearch={setGlobalFilter}
            columnPanel={
              <DataTableColumnPanel table={table} onReset={resetColumns} />
            }
          />
        }
      >
        <DataTable<BookListRow>
          table={table}
          columns={columns}
          data={data}
          isLoading={isLoading}
          isFetching={isFetching}
          rowActions={(row) => <BookRowActions row={row} />}
          className="flex-1"
        />
      </ListPageShell>
    </PageShell>
  );
}
