"use client";

import { PageShell } from "@/components/shared/page-shell";
import { ListPageShell } from "@/components/shared/list-page-shell";
import {
  DataTable,
  DataTableColumnPanel,
  useDataTable,
  useMagicFilterQuery,
  useTableState,
} from "@/components/data-table";
import { useBooksColumns } from "@/components/features/books/books-columns";
import { BookRowActions } from "@/components/features/books/book-row-actions";
import { BooksToolbar } from "@/components/features/books/books-toolbar";
import type { BookListRow } from "@/components/features/books/types";

export default function BooksPage() {
  const columns = useBooksColumns();
  const columnState = useTableState();

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
    entity: "book",
    queryKey: ["books"],
    initialPageSize: 50,
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

  return (
    <PageShell>
      <ListPageShell
        toolbar={
          <BooksToolbar
            search={globalFilter}
            onSearch={setGlobalFilter}
            columnPanel={<DataTableColumnPanel table={table} />}
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
