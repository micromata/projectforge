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
  children: ReactNode;
}

export function ListPageShell({
  toolbar,
  banner,
  children,
}: ListPageShellProps) {
  return (
    <>
      {toolbar}
      {banner}
      <div className="flex flex-1 overflow-hidden">{children}</div>
    </>
  );
}
