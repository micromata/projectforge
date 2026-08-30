"use client";

import { useCallback, useMemo, useState } from "react";
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
import { useListMeta } from "@/hooks/use-list-meta";
import {
  isAccessDenied,
  useAccessDeniedRedirect,
  useReadAccessGuard,
} from "@/hooks/use-read-access-guard";
import { useSelectAllShortcut } from "@/hooks/use-select-all-shortcut";
import { useUpdateAccess } from "@/hooks/use-update-access";
import { useEntitySelection } from "@/store/selection-store";
import { deletedRowClass } from "@/lib/dynamic/grid/row-class";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  auditColumnsFor,
  defaultVisibilityOf,
} from "@/lib/page-def/audit-columns";
import { defaultPinningOf, visibleColumnsOf } from "@/lib/page-def/define-page";
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
  /**
   * A filter to seed the list with instead of the one the backend remembers for this user — the
   * "cleared" filter of a transient jump (the consumption bar opening a task's time sheets). When set,
   * pass `transient` too, so it is not stored as the user's current filter afterwards.
   */
  filterOverride?: MagicFilter;
  /**
   * The list was opened by such a jump: its filter must not be remembered (see `filterOverride` and
   * useEntityListPage.transient).
   */
  transient?: boolean;
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
>({
  page,
  filterOverride,
  transient = false,
}: EntityListPageProps<Row, Values, Data, M>) {
  // Column layout is stored server-side per entity, so it follows the user across devices (see
  // AbstractPagesRest.columnStates). The table only mounts once it has arrived: the state seeds
  // TanStack's initial state, which can't be replaced afterwards without fighting the user's own
  // edits. Same for the filter the user last used, which the backend remembers per user.
  const stored = useStoredColumnState(page.entity);
  const remembered = useRememberedFilter(page.entity);
  // Whether this user may see this entity at all. Blocking, and before everything else: a user without
  // the right must not get the page - not even its toolbar, its columns or its exports - around an
  // empty table (see useReadAccessGuard, which redirects).
  const access = useReadAccessGuard(page.entity);

  if (access.isPending || stored.isPending || remembered.isPending) {
    return (
      <PageShell>
        <div className="flex flex-1 items-center justify-center">
          <Spinner />
        </div>
      </PageShell>
    );
  }
  if (access.denied) {
    return null;
  }

  // A failed read is not worth blocking the page for — start from the defaults. A transient jump seeds
  // the list from its own filter (`filterOverride`) rather than the remembered one, and marks it so the
  // filter is not stored back (see DeclaredList).
  return (
    <DeclaredList
      page={page}
      storedState={stored.data ?? {}}
      restoredFilter={filterOverride ?? remembered.filter}
      transient={transient}
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
  transient = false,
}: {
  page: PageDef<Row, Values, Data, M>;
  storedState: ColumnState;
  restoredFilter?: MagicFilter;
  transient?: boolean;
}) {
  const t = useTranslations();
  // Where "add" and a row click lead: this app's form, or the legacy one for a page whose list is
  // migrated and whose form is not yet (see useEditTargets).
  // The return targets go with it: where the form has several callers, this list is one of them and has
  // to name itself in the url it opens (see returnToQuery).
  const targets = useEditTargets(
    page.entity,
    page.route,
    !!page.edit,
    page.edit?.returnTargets
  );
  // Which of the declared columns this installation and this user have at all — the backend's answer,
  // as `ListMetaData.variables` (see ColumnBase.visible). Already in the cache: the page loads the
  // list's meta data for its filter fields and its edit targets anyway.
  const variables = useListMeta(page.entity).data?.variables;
  // Whether this user may change entries at all. A write-only affordance — the mass-update toggle —
  // has no place for a read-only viewer, e.g. an order-book user on the outgoing invoice list, whose
  // rest class reports `update: false` (see useUpdateAccess). Only entities overriding
  // `listUpdateAccess()` answer anything but `true`, so this changes nothing elsewhere.
  const updateAccess = useUpdateAccess(page.entity);
  // Every list offers `created` and `lastUpdate`, hidden until the user asks for them — appended here
  // rather than declared per page (see auditColumnsFor).
  const declarations = useMemo(() => {
    // Dropped before the audit pair is appended and before the pinning is derived, so both see the
    // set the table actually gets.
    const kept = visibleColumnsOf(page.columns, variables);
    const appended = auditColumnsFor(kept, page.metadata);
    const columns = appended.length ? [...kept, ...appended] : kept;
    // Over all of them, appended and declared alike: a page may hide a column of its own at first too
    // (see ColumnBase.hiddenByDefault).
    return { columns, defaultVisibility: defaultVisibilityOf(columns) };
  }, [page.columns, page.metadata, variables]);
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

  // A view option the statistics slot may switch on (the invoice list's previous-year comparison): a
  // transient boolean that adds an `extended` flag to the list call. Owned here because the slot is
  // declarative and cannot hold state; it survives period stepping (only the filter values change) and
  // resets when the page is left, which is what "on demand" asks for.
  const [previousYearComparison, setPreviousYearComparison] = useState(false);
  const buildFilter = useCallback(
    (base: MagicFilter): MagicFilter =>
      previousYearComparison
        ? {
            ...base,
            extended: { ...base.extended, previousYearComparison: true },
          }
        : base,
    [previousYearComparison]
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
    buildFilter,
    serverPaging: page.serverPaging,
    transient,
  });
  // The shell's guard has already passed the meta data, so what is left for this one is a right the
  // list call is the first to see refused - withdrawn mid-session, or an entity whose rest class fills
  // no read flag (see useAccessDeniedRedirect).
  const denied = isAccessDenied(list.error);
  useAccessDeniedRedirect(denied);
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

  // After every hook, so the early return doesn't change their order.
  if (denied) {
    return null;
  }

  return (
    <PageShell>
      <ListPageShell
        toolbar={
          <ListToolbar
            // Through leafKeyOf: a title key may be a namespace as well (`task.title.list` is both the
            // heading and the parent of `task.title.list.select`), and the bare key would throw.
            title={t(leafKeyOf(page.titleKey, t.has))}
            category={t(page.categoryKey)}
            searchValue={list.globalFilter}
            onSearchChange={list.setGlobalFilter}
            // Only where this user may add one: without the right the button is left out, as the
            // legacy page leaves the create entry out of its menu (see useEditTargets.canAdd).
            addHref={targets.canAdd ? targets.addHref : undefined}
            addIsLegacy={targets.legacy}
            legacyUrl={list.legacyUrl}
            selectionToggle={
              page.massUpdate &&
              updateAccess !== false && (
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
                periodKinds={page.filterPeriodKinds}
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
          // The filter as sent, so a slot can decide whether its view option even applies (the invoice
          // line enables its comparison only for a bounded date range).
          filter: list.filter,
          previousYearComparison,
          setPreviousYearComparison,
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
          // The list's table is the page's scroll column, so it is what makes the logo row give way.
          collapseLogoOnScroll
          isLoading={list.isLoading}
          isFetching={list.isFetching}
          rowClassName={(row) =>
            deletedRowClass(row) ?? page.rowClassName?.(row)
          }
          // Coming back from the edit page: the backend remembers which entry that was, so the list
          // marks it and brings it into view (see useHighlightedRow).
          highlightRowId={list.highlightRowId}
          highlightScope={page.entity}
          // Leaving the list to look at an entry and coming back returns to the page and the offset it
          // was left with (see useRememberScroll).
          viewScope={page.entity}
          // Only where an entry may be opened at all: without the right there is no handler and no
          // pointer cursor, the way Wicket's list shows a plain label instead of a link (see
          // useEditTargets.canOpen — it is the entity's answer, not the single entry's).
          onRowClick={
            targets.canOpen ? (row) => targets.openEntry(row.id) : undefined
          }
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
