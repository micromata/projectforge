"use client";

import { useCallback, useMemo } from "react";
import { useTranslations } from "next-intl";
import {
  DataTable,
  DataTableColumnPanel,
  FilterFavoritesMenu,
  FilterPills,
  ListGearMenu,
  selectionColumn,
  SELECTION_COLUMN_ID,
  useRememberedFilter,
  useStoredColumnState,
  type ColumnState,
} from "@/components/data-table";
import { ListPageShell } from "@/components/shared/list-page-shell";
import { PageShell } from "@/components/shared/page-shell";
import { Spinner } from "@/components/shared/spinner";
import { useEditTargets } from "@/hooks/use-edit-targets";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import { useEntityListPage, type ListRow } from "@/hooks/use-entity-list-page";
import { useSelectAllShortcut } from "@/hooks/use-select-all-shortcut";
import { useEntitySelection } from "@/store/selection-store";
import { deletedRowClass } from "@/lib/dynamic/grid/row-class";
import {
  auditColumnsFor,
  defaultVisibilityOf,
} from "@/lib/page-def/audit-columns";
import { defaultPinningOf } from "@/lib/page-def/define-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { LegendEntry, PageDef } from "@/lib/page-def/types";
import type { MagicFilter } from "@/lib/rs/types";
import { TableLegend } from "@/components/data-table/table-legend";
import { ListSelectionSection } from "./list-selection-section";
import { ListToolbar } from "./list-toolbar";
import { SelectionModeToggle } from "./selection-mode-toggle";
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

/**
 * The checkbox column leads every row, whatever the user's stored layout says — constant, so the
 * table's memoization is not defeated by a fresh array on every render (see withLockedFirst).
 */
const LOCKED_COLUMN_IDS = [SELECTION_COLUMN_ID];

/** Always includes the deleted entry first, then any entity-specific entries. */
function legendEntries<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>(page: PageDef<Row, Values, Data, M>): LegendEntry[] {
  const deletedEntry: LegendEntry = {
    className: "row-deleted",
    labelKey: page.deletedLabelKey ?? "table.legend.deleted",
    strikethrough: true,
  };
  return [deletedEntry, ...(page.legend ?? [])];
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
  const t = useTranslations();
  // Where "add" and a row click lead: this app's form, or the legacy one for a page whose list is
  // migrated and whose form is not yet (see useEditTargets).
  const targets = useEditTargets(page.entity, page.route, !!page.edit);
  // Every list offers `created` and `lastUpdate`, hidden until the user asks for them — appended here
  // rather than declared per page (see auditColumnsFor).
  const declarations = useMemo(() => {
    const appended = auditColumnsFor(page.columns, page.metadata);
    const columns = appended.length
      ? [...page.columns, ...appended]
      : page.columns;
    // Over all of them, appended and declared alike: a page may hide a column of its own at first too
    // (see ColumnBase.hiddenByDefault).
    return { columns, defaultVisibility: defaultVisibilityOf(columns) };
  }, [page.columns, page.metadata]);
  const declared = useDeclaredColumns<Row, M>(
    page.metadata,
    declarations.columns
  );
  // The mode read straight from the store, because the columns depend on it and they are an argument
  // of the hook that owns the selection (see useListSelection, which reads the same entry).
  const selectionActive = useEntitySelection(page.entity).active;
  // Prepended rather than declared, and outside useDeclaredColumns: it shows no property of the
  // entity, so there is nothing for a declaration to name and no metadata to derive it from. Only
  // inside the mode — outside it, a column of unticked checkboxes is a column of nothing.
  const columns = useMemo(
    () =>
      page.massUpdate && selectionActive
        ? [selectionColumn<Row>(), ...declared]
        : declared,
    [declared, page.massUpdate, selectionActive]
  );
  // Derived from the same declarations as the columns, so the pinned edge and the order are one
  // statement and cannot drift (see defaultPinningOf). The checkbox column is not in here: it leads
  // the row as a *locked* column, which is a render-time derivation and stays out of the layout the
  // user owns and the backend stores (see withLockedFirst).
  const defaultPinning = useMemo(
    () => defaultPinningOf(declarations.columns),
    [declarations.columns]
  );

  const list = useEntityListPage<Row>({
    entity: page.entity,
    queryKey: page.queryKey,
    columns,
    storedState,
    restoredFilter,
    defaultPinning,
    defaultVisibility: declarations.defaultVisibility,
    massUpdateEndpoint: page.massUpdate?.endpoint,
    lockedColumnIds: LOCKED_COLUMN_IDS,
  });
  const ListActions = page.listActions;
  const mode = list.selectionMode;
  const table = list.table;
  // Both the shortcut and the bar's button mean the whole result set, which is the set the table's own
  // header checkbox covers as well (the list holds it all on the client).
  const selectAll = useCallback(
    () => table.toggleAllRowsSelected(true),
    [table]
  );
  useSelectAllShortcut(mode.active, selectAll);

  return (
    <PageShell>
      <ListPageShell
        toolbar={
          <ListToolbar
            title={t(page.titleKey)}
            category={t(page.categoryKey)}
            searchValue={list.globalFilter}
            onSearchChange={list.setGlobalFilter}
            addHref={targets.addHref}
            addIsLegacy={targets.legacy}
            legacyUrl={list.legacyUrl}
            selectionToggle={
              page.massUpdate && (
                <SelectionModeToggle
                  active={mode.active}
                  onToggle={() => (mode.active ? mode.leave() : mode.enter())}
                />
              )
            }
            actions={ListActions && <ListActions filter={list.filter} />}
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
        banner={page.statistics?.({
          statistics: list.statistics,
          isFetching: list.isFetching,
        })}
        selectionBar={
          page.massUpdate &&
          mode.active && (
            <ListSelectionSection<Row, M>
              massUpdate={page.massUpdate}
              mode={mode}
              metadata={page.metadata}
              columns={declarations.columns}
              onSelectAll={selectAll}
            />
          )
        }
      >
        <DataTable<Row>
          table={list.table}
          columns={columns}
          data={list.data}
          isLoading={list.isLoading}
          isFetching={list.isFetching}
          rowClassName={(row) =>
            deletedRowClass(row) ?? page.rowClassName?.(row)
          }
          // Coming back from the edit page: the backend remembers which entry that was, so the list
          // marks it and brings it into view (see useHighlightedRow).
          highlightRowId={list.highlightRowId}
          highlightScope={page.entity}
          onRowClick={(row) => targets.openEntry(row.id)}
          // The mode decides what a click means: outside it every click opens the entry, inside it
          // every click selects (`selection` is undefined outside, so nothing of it is wired up).
          selection={mode.selection}
          footer={<TableLegend entries={legendEntries(page)} />}
          className="flex-1"
        />
      </ListPageShell>
    </PageShell>
  );
}
