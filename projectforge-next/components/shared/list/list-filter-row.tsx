"use client";

import type { ReactNode } from "react";
import { FilterPillsCollapsed } from "@/components/data-table/filter-pills-collapsed";

interface ListFilterRowProps {
  notice?: ReactNode;
  filterPills?: ReactNode;
  columnPanel?: ReactNode;
  /** Folded away on a cramped screen (see ListToolbar): only the pills with a value, no column panel. */
  collapsed: boolean;
  /** How many filters have a value: folded with none, the row is left out. */
  filterCount: number;
}

/**
 * The filter row of a list's head: filters left, table settings right — both act on the list below, not on the
 * page. The notice sits above the pills, right where the user narrows the filter that overflowed.
 */
export function ListFilterRow({
  notice,
  filterPills,
  columnPanel,
  collapsed,
  filterCount,
}: ListFilterRowProps) {
  const pills =
    (filterPills || columnPanel) && !(collapsed && filterCount === 0);
  if (!notice && !pills) return null;
  return (
    <div className="flex flex-col gap-2 px-4 pb-2.5">
      {notice}
      {pills && (
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <FilterPillsCollapsed value={collapsed}>
              {filterPills}
            </FilterPillsCollapsed>
          </div>
          {!collapsed && columnPanel}
        </div>
      )}
    </div>
  );
}
