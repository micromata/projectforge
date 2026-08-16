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
      {toolbar}
      {banner}
      {selectionBar}
      <div className="flex flex-1 overflow-hidden">{children}</div>
    </>
  );
}
