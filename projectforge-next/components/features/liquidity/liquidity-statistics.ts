/**
 * The statistics of the liquidity entry list: what the backend sends and which entries are shown.
 *
 * Kept apart from the component so the rules below can be asserted without a DOM, the same split the
 * invoice statistics follow.
 */

/** Mirrors `LiquidityEntityRest.LiquidityStatistics`, computed over the whole (filtered) result set. */
export interface LiquidityStatistics {
  counter?: number | null;
  counterPaid?: number | null;
  total?: number | null;
  paid?: number | null;
  open?: number | null;
  overdue?: number | null;
  /** True when the base-date filter is a date in the past — the Wicket list's red statistics box. */
  pastBaseDate?: boolean | null;
}

/**
 * How an entry reads — what is still open in the accent teal, what is overdue in the accent pink,
 * everything else plain. Named by meaning, not by colour, so a theme change happens in one place.
 */
export type LiquidityStatisticsTone = "plain" | "open" | "overdue";

/** The brand token each tone reads in — teal and pink, as the Wicket list colours them. */
export const TONE_CLASS: Record<LiquidityStatisticsTone, string> = {
  plain: "",
  open: "text-brand-teal",
  overdue: "text-brand-pink",
};

/** One entry of the line: its label, its amount, and how it reads. */
export interface LiquidityStatisticsEntry {
  labelKey: string;
  value?: number | null;
  tone: LiquidityStatisticsTone;
}

/**
 * The entries to show, in the order the Wicket list form shows them (`LiquidityEntryListForm.addStatistics`):
 * the total of the filtered entries, what is already paid, what is still open and what is overdue. The total,
 * paid and open always show — "0,00 €" is the honest answer to a filter that matched nothing. Overdue is
 * dropped while zero, the figure a reader only looks for when there is something to look for.
 */
export function liquidityStatisticsEntries(
  statistics: LiquidityStatistics | undefined
): LiquidityStatisticsEntry[] {
  if (!statistics) return [];
  const entries: LiquidityStatisticsEntry[] = [
    { labelKey: "fibu.common.betrag", value: statistics.total, tone: "plain" },
    {
      labelKey: "fibu.rechnung.status.bezahlt",
      value: statistics.paid,
      tone: "plain",
    },
    { labelKey: "fibu.rechnung.offen", value: statistics.open, tone: "open" },
  ];
  if (statistics.overdue) {
    entries.push({
      labelKey: "fibu.rechnung.filter.ueberfaellig",
      value: statistics.overdue,
      tone: "overdue",
    });
  }
  return entries;
}
