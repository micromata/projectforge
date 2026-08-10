"use client";

import { useTranslations } from "next-intl";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/shared/app-sidebar";
import { BrandStripe } from "@/components/shared/brand-stripe";
import { ListPageShell } from "@/components/shared/list-page-shell";
import {
  DataTable,
  DEFAULT_PAGE_SIZE,
  useMagicFilterQuery,
} from "@/components/data-table";
import { ListToolbar } from "@/components/shared/list/list-toolbar";
import { useBooksColumns } from "@/components/features/books/books-columns";
import type { BookListRow } from "@/components/features/books/types";

export default function DemoPage() {
  const t = useTranslations();
  const columns = useBooksColumns();
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
    initialPageSize: DEFAULT_PAGE_SIZE,
  });

  return (
    <div className="flex h-screen flex-col overflow-hidden">
      <BrandStripe />
      <SidebarProvider className="!min-h-0 flex flex-1 overflow-hidden">
        <AppSidebar />
        <SidebarInset className="flex flex-1 flex-col overflow-hidden">
          <ListPageShell
            toolbar={
              <ListToolbar
                title={t("books.title")}
                category={t("menu.common")}
                searchValue={globalFilter}
                onSearchChange={setGlobalFilter}
                searchPlaceholder={t("books.searchPlaceholder")}
                addHref="/books/new"
                addLabel={t("book.title.add")}
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
              manualSorting
              isLoading={isLoading}
              isFetching={isFetching}
              getRowId={(row) => String(row.id)}
              className="flex-1"
            />
          </ListPageShell>
        </SidebarInset>
      </SidebarProvider>
    </div>
  );
}
