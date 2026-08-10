"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
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
import { ListPageShell } from "@/components/shared/list-page-shell";
import { PageShell } from "@/components/shared/page-shell";
import { Spinner } from "@/components/shared/spinner";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import { useEntityListPage, type ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { PageDef } from "@/lib/page-def/types";
import type { MagicFilter } from "@/lib/rs/types";
import { ListToolbar } from "./list-toolbar";
import { useDeclaredColumns } from "./use-declared-columns";

export interface EntityListPageProps<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
> {
  page: PageDef<Row, Values, Data, M>;
}

/**
 * The whole list page of an entity, rendered from its declaration (see lib/page-def/types.ts).
 *
 * A page that doesn't fit this shape is not made to: it composes `useEntityListPage` and
 * `ListToolbar` itself, which is what this component does too. Two levels — declarative for the
 * normal case, hooks for the exception.
 */
export function EntityListPage<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({ page }: EntityListPageProps<Row, Values, Data, M>) {
  // Column layout is stored server-side per entity, so it follows the user across devices (see
  // AbstractPagesRest.columnStates). The table only mounts once it has arrived: the state seeds
  // TanStack's initial state, which can't be replaced afterwards without fighting the user's own
  // edits. Same for the filter the user last used, which the backend remembers per user.
  const stored = useStoredColumnState(page.entity);
  const remembered = useRememberedFilter(page.entity);

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
    <DeclaredList
      page={page}
      storedState={stored.data ?? {}}
      restoredFilter={remembered.filter}
    />
  );
}

function DeclaredList<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({
  page,
  storedState,
  restoredFilter,
}: {
  page: PageDef<Row, Values, Data, M>;
  storedState: ColumnState;
  restoredFilter?: MagicFilter;
}) {
  const router = useRouter();
  const t = useTranslations();
  const columns = useDeclaredColumns<Row, M>(page.metadata, page.columns);

  const list = useEntityListPage<Row>({
    entity: page.entity,
    queryKey: page.queryKey,
    columns,
    storedState,
    restoredFilter,
  });

  return (
    <PageShell>
      <ListPageShell
        toolbar={
          <ListToolbar
            title={t(page.titleKey)}
            category={t(page.categoryKey)}
            searchValue={list.globalFilter}
            onSearchChange={list.setGlobalFilter}
            searchPlaceholder={t(page.searchPlaceholderKey)}
            addHref={`${page.route}/new`}
            addLabel={t(page.addTitleKey)}
            legacyUrl={list.legacyUrl}
            gearMenu={
              <ListGearMenu
                entity={page.entity}
                onFilterReset={list.resetFilter}
              />
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
        <DataTable<Row>
          table={list.table}
          columns={columns}
          data={list.data}
          isLoading={list.isLoading}
          isFetching={list.isFetching}
          onRowClick={(row) => router.push(`${page.route}/${row.id}`)}
          className="flex-1"
        />
      </ListPageShell>
    </PageShell>
  );
}
