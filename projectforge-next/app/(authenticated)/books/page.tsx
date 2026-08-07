"use client";

import { PageShell } from "@/components/shared/page-shell";
import { ListPageShell } from "@/components/shared/list-page-shell";
import { Spinner } from "@/components/shared/spinner";
import {
  DataTable,
  DataTableColumnPanel,
  FilterAllDialog,
  FilterFavoritesMenu,
  FilterPills,
  filterValuesFromEntries,
  useColumnStatePersistence,
  useDataTable,
  useFilterFavorites,
  useListFilters,
  useMagicFilterQuery,
  useRememberedFilter,
  useRememberFilter,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";
import type { MagicFilter } from "@/lib/rs/types";
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
  const columns = useBooksColumns();
  const filters = useListFilters(ENTITY, { restoredFilter });

  const columnState = useTableState({
    restoredState: storedState,
    initialSorting: storedState.sorting,
  });

  const {
    data,
    rowCount,
    isLoading,
    isFetching,
    filter,
    sorting,
    setSorting,
    pagination,
    setPagination,
    globalFilter,
    setGlobalFilter,
    applyFilter,
  } = useMagicFilterQuery<BookListRow>({
    entity: ENTITY,
    queryKey: ["books"],
    initialPageSize: 50,
    // Sorting drives the backend query, so it lives with the query, not in the
    // column state — the stored order seeds it here.
    initialSorting: storedState.sorting,
    // The search box belongs to the filter row, so it is restored with it.
    initialGlobalFilter: restoredFilter?.searchString,
    // The pill filters are applied server-side, unlike the header's column filters.
    filterEntries: filters.entries,
    // Has to go out with every list call: the backend stores the filter it gets as
    // the user's current one, so without it the link to the favorite would be lost
    // and the edited filter could no longer be saved back into it.
    favoriteId: filters.favorite?.id,
    favoriteName: filters.favorite?.name,
  });

  // The user's saved filters — the backend's filter favorites, so a filter saved
  // here is the same one the legacy list page offers.
  const favorites = useFilterFavorites({
    entity: ENTITY,
    filter,
    current: filters.favorite,
    onCurrentChange: filters.setFavorite,
    onApply: (applied) => {
      filters.setValues(filterValuesFromEntries(applied.entries));
      applyFilter(applied);
    },
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
    // Sorting and the search string go to Spring; the column filters and paging work
    // on the client, because getList returns the whole result set at once.
    manualSorting: true,
    getRowId: (row: BookListRow) => String(row.id),
  });

  // Coming back to the list should show the filter it was left with, also without
  // a reload — the cached initialList would otherwise still hold the old one.
  // The filter already carries the favorite's id and name.
  useRememberFilter(ENTITY, filter);

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
              <DataTableColumnPanel
                table={table}
                onReset={resetColumns}
                className="h-6 rounded-full px-2.5 text-xs"
              />
            }
            filterPills={
              <FilterPills
                elements={filters.elements}
                values={filters.values}
                onChange={filters.setValues}
                trailing={
                  <>
                    <FilterAllDialog
                      elements={filters.elements}
                      values={filters.values}
                      onApply={filters.setValues}
                      className="h-6 gap-1 rounded-full px-2.5 text-xs"
                    />
                    <FilterFavoritesMenu
                      favorites={favorites}
                      className="h-6 gap-1 rounded-full px-2.5 text-xs"
                    />
                  </>
                }
              />
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
