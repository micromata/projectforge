"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { FilterIcon, MoreHorizontalIcon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import { useCramped } from "@/hooks/use-cramped";
import { Separator } from "@/components/ui/separator";
import { AddEntryButton } from "@/components/shared/add-entry-button";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { CrampedToggleButton } from "./cramped-toggle-button";
import { ListFilterRow } from "./list-filter-row";
import { SearchInput } from "./search-input";

export interface ListToolbarProps {
  /** Heading of the page, e.g. "Bücherliste". */
  title: string;
  /**
   * The host already shows the heading (an embedded list under a tab): no title row of its own, its
   * actions move to the right end of the search row.
   */
  compact?: boolean;
  /** The menu parent above it, e.g. "Allgemein" — where the entry sits in the main menu. */
  category: string;
  searchValue: string;
  /** Called with the typed value once it has settled, see [SearchInput]. */
  onSearchChange: (value: string) => void;
  /**
   * Route of the add page, e.g. `/book/new` — absent where this user may not add an entry, which
   * leaves the button and its keyboard shortcut out entirely (see useEditTargets.canAdd).
   */
  addHref?: string;
  /** The list belongs to an unfinished feature, see PageDef.experimental. */
  experimental?: boolean;
  /**
   * Actions of the list itself — the exports of the order book (see PageDef.listActions). Between the
   * legacy link and the gear menu: they act on the page, but on all of it rather than on its settings.
   * The [ExportMenu] comes last among them, so it is always the gear menu's neighbour.
   */
  actions?: ReactNode;
  /**
   * Switches the list into selection mode — its own prop rather than one of [actions], because it
   * changes what the page *is* (clicks select, checkboxes appear) instead of acting on the list.
   * Absent for a list with no mass update, which has nothing to select for.
   */
  selectionToggle?: ReactNode;
  /** The selection mode is on: on a cramped screen the button unfolding the actions says so while they're folded. */
  selectionActive?: boolean;
  /**
   * The menu of the area the list belongs to (see PageDef.listMenu), the right-most control: the other pages
   * of the area carry it at the same place, so it doesn't move when one switches between them.
   */
  menu?: ReactNode;
  /** Column visibility/pinning panel, rendered once the table instance exists. */
  columnPanel?: ReactNode;
  /** Active filters as editable pills plus the "all filters" trigger. */
  filterPills?: ReactNode;
  /** How many filters have a value, shown on the button unfolding the filter row on a cramped screen. */
  filterCount?: number;
  /**
   * A note about the result of the current filter, shown in the filter row above the pills — the red
   * "list is truncated" warning when the backend capped the result (see ListTruncationNotice). Absent
   * for a complete result.
   */
  notice?: ReactNode;
  /** Maintenance actions of the list (re-index, reset filter), see ListGearMenu. */
  gearMenu?: ReactNode;
}

/** The head of every list page: where it sits, what it is called, search, filters and "add". */
export function ListToolbar({
  title,
  compact = false,
  category,
  searchValue,
  onSearchChange,
  addHref,
  experimental,
  actions,
  selectionToggle,
  selectionActive = false,
  menu,
  columnPanel,
  filterPills,
  filterCount = 0,
  notice,
  gearMenu,
}: ListToolbarProps) {
  const t = useTranslations();
  const cramped = useCramped();
  const [actionsOpen, setActionsOpen] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  // On a phone the table is what matters: the head folds the actions and the filter row away, each unfolded
  // by a tap (not on scrolling, which iOS doesn't do reliably). Folded away by CSS only, so an action's
  // dialog that is open stays mounted.
  const filtersCollapsed = cramped && !filtersOpen;
  const secondary = (
    <>
      {selectionToggle}
      {actions}
      {gearMenu}
    </>
  );
  const controls = (foldable: boolean) => (
    <>
      {foldable ? (
        // In the title row, which makes the actions its own items on a cramped screen: there this one
        // takes a line of its own below the title, unfolded by the "more" button.
        <div
          className={cn(
            "flex flex-wrap items-center gap-2 sm:gap-3 cramped:order-last cramped:basis-full cramped:justify-end",
            !actionsOpen && "cramped:hidden"
          )}
        >
          {secondary}
        </div>
      ) : (
        secondary
      )}
      {/* Divider only with a menu beside it *and* something to separate it from: it parts the
          list's own actions from "add", which creates an entity. `!self-center`: with an explicit height
          the primitive's `self-stretch` degrades to flex-start and hangs the line above the buttons. The
          `!` is needed because the primitive's `data-vertical:self-stretch` carries an attribute selector
          and would otherwise outweigh a plain `self-center`. */}
      {gearMenu && addHref && (
        <Separator
          orientation="vertical"
          className={cn("!h-5 !self-center", foldable && "cramped:hidden")}
        />
      )}
      {addHref && <AddEntryButton href={addHref} />}
      {foldable && (
        <CrampedToggleButton
          icon={MoreHorizontalIcon}
          label={t("more")}
          iconOnly
          open={actionsOpen}
          onToggle={() => setActionsOpen((open) => !open)}
          highlighted={selectionActive && !actionsOpen}
        />
      )}
      {menu && (
        <>
          <Separator orientation="vertical" className="!h-5 !self-center" />
          {menu}
        </>
      )}
    </>
  );
  return (
    <div className="border-b bg-background">
      {!compact && (
        <PageTitleRow
          title={title}
          category={category}
          experimental={experimental}
          compactWhenCramped
        >
          {controls(true)}
        </PageTitleRow>
      )}

      <div
        className={cn(
          "flex items-center gap-3 px-4 pb-2.5",
          compact ? "pt-2.5" : "py-2.5"
        )}
      >
        <div className="relative max-w-md flex-1">
          <SearchInput
            value={searchValue}
            onChange={onSearchChange}
            focusIfIdle
          />
        </div>
        {filterPills && (
          <CrampedToggleButton
            icon={FilterIcon}
            label={t("filter._")}
            open={filtersOpen}
            onToggle={() => setFiltersOpen((open) => !open)}
            count={filterCount}
          />
        )}
        {compact && (
          <div className="ml-auto flex items-center gap-3">
            {controls(false)}
          </div>
        )}
      </div>

      <ListFilterRow
        notice={notice}
        filterPills={filterPills}
        columnPanel={columnPanel}
        collapsed={filtersCollapsed}
        filterCount={filterCount}
      />
    </div>
  );
}
