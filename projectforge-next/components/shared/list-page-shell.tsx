"use client";

import type { ReactNode } from "react";
import { DataTableHeadSlot } from "@/components/data-table/data-table-head-slot";
import { useCramped } from "@/hooks/use-cramped";

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
  const cramped = useCramped();
  if (cramped) {
    // On a phone the header alone may fill the screen. In the table's scroll area (DataTableHeadSlot) it scrolls
    // away with the rows, and the table then has the whole height, its column header sticky; a swipe down brings
    // it back. One scroller: iOS doesn't hand a swipe on from a table at its top to an outer one (<main>).
    return (
      <DataTableHeadSlot
        value={
          <div className="flex flex-col">
            {toolbar}
            {banner}
            {selectionBar}
          </div>
        }
      >
        <div className="flex min-h-0 min-w-0 flex-1 overflow-hidden">
          {children}
        </div>
      </DataTableHeadSlot>
    );
  }
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
          stays reachable while <main> scrolls. On a narrow or low screen (a phone) the header alone may
          fill <main>: there the row is exactly as tall as <main> (100cqh, see PageShell), so the header
          scrolls away and the table then takes the whole screen, still scrolling on its own (sticky
          header, wide columns sideways). */}
      <div className="flex min-h-64 min-w-0 flex-1 overflow-hidden cramped:h-[100cqh] cramped:flex-none">
        {children}
      </div>
    </>
  );
}
