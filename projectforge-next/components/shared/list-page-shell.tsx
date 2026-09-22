"use client";

import type { ReactNode } from "react";

export interface ListPageShellProps {
  toolbar: ReactNode;
  /**
   * Between the toolbar and the table — aggregates over the result set, as the order book shows its
   * sums (see OrderStatisticsLine). Above rather than below the table on purpose: it belongs to the
   * whole list, not to the page of rows that happens to be visible.
   */
  banner?: ReactNode;
  /**
   * Directly above the table while the list is in selection mode — the count and what can be done
   * with it (see SelectionBar).
   *
   * Below the statistics banner rather than beside the toolbar: it is a *state* of the table, and it
   * appears and disappears, so it must not move the toolbar's own rows around when it does.
   */
  selectionBar?: ReactNode;
  children: ReactNode;
}

export function ListPageShell({
  toolbar,
  banner,
  selectionBar,
  children,
}: ListPageShellProps) {
  return (
    <>
      {/* The header stays full-height and pinned above the table; grouped as one shrink-0 block so it
          never gives up its rows to the flex-1 table row below. */}
      <div className="flex shrink-0 flex-col">
        {toolbar}
        {banner}
        {selectionBar}
      </div>
      {/* min-w-0 keeps the wide table from inflating the page (it scrolls inside its own box instead);
          min-h-64 both overrides the default content-based min-height (so the row can shrink for the
          inner vertical scroll) and floors it, so on a viewport too short for the header the table
          stays reachable while <main> scrolls. */}
      <div className="flex min-h-64 min-w-0 flex-1 overflow-hidden">
        {children}
      </div>
    </>
  );
}
