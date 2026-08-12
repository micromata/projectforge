/**
 * The statistics of an order list: what the backend sends and which lines are worth showing.
 *
 * Kept apart from the component so the rule "a line whose counter is 0 is left out" can be asserted
 * without a DOM, the same split `lib/page-def/define-page.ts` follows.
 */

/** Mirrors `AuftragPagesRest.OrderStatistics`, the aggregates of the whole result set. */
export interface OrderStatistics {
  netSum?: number | null;
  counter?: number | null;
  akquiseSum?: number | null;
  counterAkquise?: number | null;
  commissionedSum?: number | null;
  counterCommissioned?: number | null;
  invoicedSum?: number | null;
  counterInvoiced?: number | null;
  notYetInvoicedSum?: number | null;
  counterNotYetInvoiced?: number | null;
  toBeInvoicedSum?: number | null;
  counterToBeInvoiced?: number | null;
}

/**
 * How a statistics entry is coloured — the two the Wicket list emphasizes (`TextStyle.BLUE` on the
 * commissioned sum, `TextStyle.RED` on the one still to be invoiced), everything else plain.
 *
 * Named by meaning, not by colour: the component maps it to a brand token, so a theme change happens
 * in one place (see app/globals.css).
 */
export type OrderStatisticsTone = "plain" | "commissioned" | "toBeInvoiced";

/** One entry of the line: its label, the amount, how many orders it sums and its tone. */
export interface OrderStatisticsEntry {
  labelKey: string;
  amount?: number | null;
  count: number;
  tone: OrderStatisticsTone;
}

/**
 * The entries to show, in the order and with the labels of `AuftragListForm.addStatistics`.
 *
 * The net sum always shows, even over an empty list — "Netto: 0,00 € (0)" is the honest answer to a
 * filter that matched nothing. Every other line is dropped while its counter is 0, exactly as the
 * Wicket panels hide themselves: a "to be invoiced" of 0,00 € says nothing and only costs the reader
 * a glance.
 */
export function orderStatisticsEntries(
  statistics: OrderStatistics | undefined
): OrderStatisticsEntry[] {
  if (!statistics) return [];
  const entries: OrderStatisticsEntry[] = [
    {
      labelKey: "fibu.common.netto",
      amount: statistics.netSum,
      count: statistics.counter ?? 0,
      tone: "plain",
    },
    {
      labelKey: "akquise",
      amount: statistics.akquiseSum,
      count: statistics.counterAkquise ?? 0,
      tone: "plain",
    },
    {
      labelKey: "fibu.auftrag.status.beauftragt",
      amount: statistics.commissionedSum,
      count: statistics.counterCommissioned ?? 0,
      tone: "commissioned",
    },
    {
      labelKey: "fibu.fakturiert",
      amount: statistics.invoicedSum,
      count: statistics.counterInvoiced ?? 0,
      tone: "plain",
    },
    {
      labelKey: "fibu.notYetInvoiced",
      amount: statistics.notYetInvoicedSum,
      count: statistics.counterNotYetInvoiced ?? 0,
      tone: "plain",
    },
    {
      labelKey: "fibu.toBeInvoiced",
      amount: statistics.toBeInvoicedSum,
      count: statistics.counterToBeInvoiced ?? 0,
      tone: "toBeInvoiced",
    },
  ];
  // The first entry is the total and stays; the rest are the breakdown and only appear where they hold
  // something.
  return entries.filter((entry, index) => index === 0 || entry.count > 0);
}
