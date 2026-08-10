"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { ListPageShell } from "@/components/shared/list-page-shell";
import { Spinner } from "@/components/shared/spinner";
import {
  DataTable,
  DataTableColumnPanel,
  FilterFavoritesMenu,
  FilterPills,
  ListGearMenu,
  useRememberedFilter,
  useStoredColumnState,
  type ColumnState,
} from "@/components/data-table";
import { ListToolbar } from "@/components/shared/list/list-toolbar";
import { useEntityListPage } from "@/hooks/use-entity-list-page";
import type { MagicFilter } from "@/lib/rs/types";
import { useBooksColumns } from "@/components/features/books/books-columns";
import { BOOKS_LIST_QUERY_KEY } from "@/components/features/books/edit/use-book-detail";
import type { BookListRow } from "@/components/features/books/types";

const ENTITY = "book";

export default function BooksPage() {
  // Column layout is stored server-side per entity, so it follows the user across
  // devices (see AbstractPagesRest.columnStates). The table only mounts once it
  // has arrived: the state seeds TanStack's initial state, which can't be
  // replaced afterwards without fighting the user's own edits.
  const stored = useStoredColumnState(ENTITY);
  // Same for the filter the user last used, which the backend remembers per user.
  const remembered = useRememberedFilter(ENTITY);

  if (stored.isPending || remembered.isPending) {
    return (
      <PageShell>
        <div className="flex flex-1 items-center justify-center">
          <Spinner />
        </div>
      </PageShell>
    );
  }

  // A failed read is not worth blocking the page for — start from the defaults.
  return (
    <BooksList
      storedState={stored.data ?? {}}
      restoredFilter={remembered.filter}
    />
  );
}

function BooksList({
  storedState,
  restoredFilter,
}: {
  storedState: ColumnState;
  restoredFilter?: MagicFilter;
}) {
  const router = useRouter();
  const t = useTranslations();
  const columns = useBooksColumns();

  const list = useEntityListPage<BookListRow>({
    entity: ENTITY,
    queryKey: BOOKS_LIST_QUERY_KEY,
    columns,
    storedState,
    restoredFilter,
  });

  return (
    <PageShell>
      <ListPageShell
        toolbar={
          <ListToolbar
            title={t("books.title")}
            category={t("menu.common")}
            searchValue={list.globalFilter}
            onSearchChange={list.setGlobalFilter}
            searchPlaceholder={t("books.searchPlaceholder")}
            addHref="/books/new"
            addLabel={t("book.title.add")}
            legacyUrl={list.legacyUrl}
            gearMenu={
              <ListGearMenu entity={ENTITY} onFilterReset={list.resetFilter} />
            }
            columnPanel={
              <DataTableColumnPanel
                table={list.table}
                onReset={list.resetColumns}
                className="h-6 rounded-full px-2.5 text-xs"
              />
            }
            filterPills={
              <FilterPills
                elements={list.filters.elements}
                values={list.filters.values}
                onChange={list.applyValues}
                trailing={
                  <FilterFavoritesMenu
                    favorites={list.favorites}
                    className="h-6 gap-1 rounded-full px-2.5 text-xs"
                  />
                }
              />
            }
          />
        }
      >
        <DataTable<BookListRow>
          table={list.table}
          columns={columns}
          data={list.data}
          isLoading={list.isLoading}
          isFetching={list.isFetching}
          onRowClick={(row) => router.push(`/books/${row.id}`)}
          className="flex-1"
        />
      </ListPageShell>
    </PageShell>
  );
}
