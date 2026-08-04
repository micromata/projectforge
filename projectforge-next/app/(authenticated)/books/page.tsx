"use client";

import { PageShell } from "@/components/shared/page-shell";
import { ListPageShell } from "@/components/shared/list-page-shell";
import {
  DataTable,
  DataTableColumnPanel,
  useMagicFilterQuery,
  useTableState,
} from "@/components/data-table";
import { useBooksColumns } from "@/components/features/books/books-columns";
import { BookRowActions } from "@/components/features/books/book-row-actions";
import { BooksToolbar } from "@/components/features/books/books-toolbar";
import type { BookListRow } from "@/components/features/books/types";
import type { Table } from "@tanstack/react-table";
import { useState } from "react";

export default function BooksPage() {
  const columns = useBooksColumns();
  const [table, setTable] = useState<Table<BookListRow> | null>(null);
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

  return (
    <PageShell>
      <ListPageShell
        toolbar={
          <BooksToolbar
            search={globalFilter}
            onSearch={setGlobalFilter}
            columnPanel={
              table ? <DataTableColumnPanel table={table} /> : undefined
            }
          />
        }
      >
        <DataTable<BookListRow>
          columns={columns}
          data={data}
          rowCount={rowCount}
          sorting={sorting}
          onSortingChange={setSorting}
          pagination={pagination}
          onPaginationChange={setPagination}
          columnFilters={columnState.columnFilters}
          onColumnFiltersChange={columnState.setColumnFilters}
          columnVisibility={columnState.columnVisibility}
          onColumnVisibilityChange={columnState.setColumnVisibility}
          columnPinning={columnState.columnPinning}
          onColumnPinningChange={columnState.setColumnPinning}
          columnSizing={columnState.columnSizing}
          onColumnSizingChange={columnState.setColumnSizing}
          columnOrder={columnState.columnOrder}
          onColumnOrderChange={columnState.setColumnOrder}
          enableColumnFilters
          enableColumnResizing
          manualSorting
          manualPagination
          manualFiltering
          isLoading={isLoading}
          isFetching={isFetching}
          getRowId={(row) => String(row.id)}
          rowActions={(row) => <BookRowActions row={row} />}
          tableRef={setTable}
          className="flex-1"
        />
      </ListPageShell>
    </PageShell>
  );
}
