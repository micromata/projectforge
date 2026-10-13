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

/** The art a period filter of this list is given as by default (see [PageDef.filterDefaultPeriodKind]). */
const FilterDefaultPeriodKindContext = createContext<PeriodKindId | null>(null);

export function FilterPeriodKindsProvider({
  periodKinds,
  defaultPeriodKind,
  children,
}: {
  /** Absent means the default; `[]` leaves the quick access out of every period filter of the list. */
  periodKinds?: readonly PeriodKindId[];
  /** The art a date typed into an empty period filter snaps to; absent leaves the two dates free. */
  defaultPeriodKind?: PeriodKindId;
  children: React.ReactNode;
}) {
  const value = useMemo(() => periodKinds, [periodKinds]);
  const inner = (
    <FilterDefaultPeriodKindContext.Provider value={defaultPeriodKind ?? null}>
      {children}
    </FilterDefaultPeriodKindContext.Provider>
  );
  if (!value) return inner;
  return (
    <FilterPeriodKindsContext.Provider value={value}>
      {inner}
    </FilterPeriodKindsContext.Provider>
  );
}

/** The default art of this list's period filters, if it offers it at all. */
export function useFilterDefaultPeriodKind(): PeriodKind | null {
  const id = useContext(FilterDefaultPeriodKindContext);
  const kinds = useFilterPeriodKinds();
  return kinds.find((kind) => kind.id === id) ?? null;
}

/** The arts a period filter offers, resolved to the kinds themselves. */
export function useFilterPeriodKinds(): PeriodKind[] {
  const ids = useContext(FilterPeriodKindsContext);
  return useMemo(() => periodKindsOf(ids), [ids]);
}
