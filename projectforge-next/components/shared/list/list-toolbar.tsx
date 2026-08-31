"use client";

import type { ReactNode } from "react";
import { Separator } from "@/components/ui/separator";
import { AddEntryButton } from "@/components/shared/add-entry-button";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SearchInput } from "./search-input";

export interface ListToolbarProps {
  /** Heading of the page, e.g. "Bücherliste". */
  title: string;
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
  /**
   * Whether [addHref] leaves this app — the add page of an entity whose list is migrated but whose
   * form is not (see useEditTargets). Rendered as a plain anchor then, because client-side routing
   * would not find a Wicket page.
   */
  addIsLegacy?: boolean;
  /** The legacy list page (`ui.legacyUrl` of the list response), see LegacyPageLink. */
  legacyUrl?: string;
  /**
   * Actions of the list itself — the exports of the order book (see PageDef.listActions). Between the
   * legacy link and the gear menu: they act on the page, but on all of it rather than on its settings.
   */
  actions?: ReactNode;
  /**
   * Switches the list into selection mode — its own prop rather than one of [actions], because it
   * changes what the page *is* (clicks select, checkboxes appear) instead of acting on the list.
   * Absent for a list with no mass update, which has nothing to select for.
   */
  selectionToggle?: ReactNode;
  /** Column visibility/pinning panel, rendered once the table instance exists. */
  columnPanel?: ReactNode;
  /** Active filters as editable pills plus the "all filters" trigger. */
  filterPills?: ReactNode;
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
  category,
  searchValue,
  onSearchChange,
  addHref,
  addIsLegacy,
  legacyUrl,
  actions,
  selectionToggle,
  columnPanel,
  filterPills,
  notice,
  gearMenu,
}: ListToolbarProps) {
  return (
    <div className="border-b bg-background">
      <PageTitleRow title={title} category={category} legacyUrl={legacyUrl}>
        {selectionToggle}
        {actions}
        {/* Divider only with a menu beside it *and* something to separate it from: it parts the
            list's own actions from "add", which creates an entity. */}
        {gearMenu && (
          <>
            {gearMenu}
            {/* `!self-center`: with an explicit height the primitive's `self-stretch` degrades to
                flex-start and hangs the line above the buttons. The `!` is needed because the
                primitive's `data-vertical:self-stretch` carries an attribute selector and would
                otherwise outweigh a plain `self-center`. */}
            {addHref && (
              <Separator orientation="vertical" className="!h-5 !self-center" />
            )}
          </>
        )}
        {addHref && <AddEntryButton href={addHref} isLegacy={addIsLegacy} />}
      </PageTitleRow>

      <div className="flex items-center gap-3 px-4 py-2.5">
        <div className="relative max-w-md flex-1">
          <SearchInput value={searchValue} onChange={onSearchChange} />
        </div>
      </div>

      {/* Filters left, table settings right: both act on the list below, not on the page. The notice
          sits above the pills — right where the user narrows the filter that overflowed. */}
      {(filterPills || columnPanel || notice) && (
        <div className="flex flex-col gap-2 px-4 pb-2.5">
          {notice}
          {(filterPills || columnPanel) && (
            <div className="flex items-start gap-3">
              <div className="flex-1">{filterPills}</div>
              {columnPanel}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
