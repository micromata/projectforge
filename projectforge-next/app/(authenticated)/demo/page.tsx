"use client";

import { useState } from "react";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/shared/app-sidebar";
import { BrandStripe } from "@/components/shared/brand-stripe";
import { ListPageShell } from "@/components/shared/list-page-shell";
import { DataTable, useMagicFilterQuery } from "@/components/data-table";
import { booksColumns } from "@/components/features/books/books-columns";
import { BookRowActions } from "@/components/features/books/book-row-actions";
import { BooksToolbar } from "@/components/features/books/books-toolbar";
import { BooksFilterPanel } from "@/components/features/books/books-filter-panel";
import type { BookListRow } from "@/components/features/books/types";

export default function DemoPage() {
  const [filters, setFilters] = useState([
    { key: "status", label: "Status: Aktiv" },
    { key: "autor", label: "Autor: Larkin" },
  ]);

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
    queryKey: ["demo-books"],
    initialPageSize: 50,
  });

  return (
    <div className="flex h-screen flex-col overflow-hidden">
      <BrandStripe />
      <SidebarProvider className="!min-h-0 flex flex-1 overflow-hidden">
        <AppSidebar />
        <SidebarInset className="flex flex-1 flex-col overflow-hidden">
          <ListPageShell
            toolbar={
              <BooksToolbar
                search={globalFilter}
                onSearch={setGlobalFilter}
                filters={filters}
                onRemove={(k) =>
                  setFilters((f) => f.filter((x) => x.key !== k))
                }
                onClearAll={() => setFilters([])}
              />
            }
            filterPanel={<BooksFilterPanel className="hidden lg:flex" />}
          >
            <DataTable<BookListRow>
              columns={booksColumns}
              data={data}
              rowCount={rowCount}
              sorting={sorting}
              onSortingChange={setSorting}
              pagination={pagination}
              onPaginationChange={setPagination}
              manualSorting
              manualPagination
              manualFiltering
              isLoading={isLoading}
              isFetching={isFetching}
              getRowId={(row) => String(row.id)}
              rowActions={(row) => <BookRowActions row={row} />}
              className="flex-1"
            />
          </ListPageShell>
        </SidebarInset>
      </SidebarProvider>
    </div>
  );
}
