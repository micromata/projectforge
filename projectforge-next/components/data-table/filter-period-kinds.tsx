"use client";

import { createContext, useContext, useMemo } from "react";
import {
  periodKindsOf,
  type PeriodKind,
  type PeriodKindId,
} from "@/lib/date-period";

/**
 * Which arts a period filter of this list offers — the default below unless the page says otherwise (see
 * [PageDef.filterPeriodKinds]).
 *
 * A context rather than a prop, because the fields sit four levels down (pill row → pill → field, and
 * the same fields again in the "all filters" dialog) and none of those levels has anything else to do
 * with periods. It is configuration of the page, set once and never changed while it is open.
 *
 * What belongs on a list is a question of its dates: the order book asks "which quarter of the period of
 * performance?" and is confused by "Jahr bis heute", while an invoice list is exactly the place for it.
 *
 * A list is read in whole months, quarters and years, and the year up to today for the comparison. The
 * order on screen is [PERIOD_KINDS]', not this list's, and so is what an entered range is read as: the
 * calendar month comes first, so 01.03.–31.03. keeps paging month by month rather than becoming a term of
 * three months, and "Jahr bis heute" is never read off two dates at all.
 */
const FilterPeriodKindsContext = createContext<readonly PeriodKindId[]>([
  "month",
  "termThreeMonths",
  "termYear",
  "yearToDate",
]);

export function FilterPeriodKindsProvider({
  periodKinds,
  children,
}: {
  /** Absent means the default; `[]` leaves the quick access out of every period filter of the list. */
  periodKinds?: readonly PeriodKindId[];
  children: React.ReactNode;
}) {
  const value = useMemo(() => periodKinds, [periodKinds]);
  if (!value) return children;
  return (
    <FilterPeriodKindsContext.Provider value={value}>
      {children}
    </FilterPeriodKindsContext.Provider>
  );
}

/** The arts a period filter offers, resolved to the kinds themselves. */
export function useFilterPeriodKinds(): PeriodKind[] {
  const ids = useContext(FilterPeriodKindsContext);
  return useMemo(() => periodKindsOf(ids), [ids]);
}
