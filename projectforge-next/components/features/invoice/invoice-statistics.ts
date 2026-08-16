/**
 * The statistics of an invoice list: what the backend sends and which entries are worth showing.
 *
 * Kept apart from the component so the rules below can be asserted without a DOM, the same split
 * `order-statistics.ts` follows.
 */

/** Mirrors `OutgoingInvoiceEntityRest.InvoiceStatistics`, computed over the whole result set. */
export interface InvoiceStatistics {
  counter?: number | null;
  counterPaid?: number | null;
  brutto?: number | null;
  bruttoWithDiscount?: number | null;
  netto?: number | null;
  paid?: number | null;
  open?: number | null;
  overdue?: number | null;
  discount?: number | null;
  /** Agreed payment target in days, averaged — what was asked for. */
  paymentTargetAverage?: number | null;
  /** Actual payment target in days, weighted by the gross sum — what the customers did. */
  actualPaymentTargetAverage?: number | null;
  /** Invoices whose currency could not be converted, so their own amount entered the sums. */
  currencyConversionWarnings?: string[] | null;
}

/**
 * How an entry reads — the two the Wicket list emphasizes (`TextStyle.BLUE` on what is still open,
 * `TextStyle.RED` on what is overdue), everything else plain.
 *
 * Named by meaning, not by colour: the component maps it to a brand token, so a theme change happens in
 * one place (see app/globals.css).
 */
export type InvoiceStatisticsTone = "plain" | "open" | "overdue";

/** One entry of the line: its label, its value, and how it reads. */
export interface InvoiceStatisticsEntry {
  labelKey: string;
  /** An amount in the system currency, or a number of days for the two averages. */
  value?: number | null;
  kind: "currency" | "days";
  tone: InvoiceStatisticsTone;
}

/**
 * The entries to show, in the order and with the labels of `AbstractRechnungListForm.addStatistics`.
 *
 * Gross and net always show, as does what is still open — "0,00 €" is the honest answer to a filter that
 * matched nothing, and to a list in which everything is paid. The three that are dropped while zero are
 * the ones the reader only looks for when there is something to look for: nothing overdue, no discount
 * taken, and a payment target of Ø 0 days, which means no invoice carried one.
 *
 * `bruttoWithDiscount` is left out where it equals the gross sum, exactly as the Wicket page shows it only
 * then: two identical amounts side by side read as an error.
 */
export function invoiceStatisticsEntries(
  statistics: InvoiceStatistics | undefined
): InvoiceStatisticsEntry[] {
  if (!statistics) return [];
  const entries: InvoiceStatisticsEntry[] = [
    {
      labelKey: "fibu.common.brutto",
      value: statistics.brutto,
      kind: "currency",
      tone: "plain",
    },
  ];
  if (
    statistics.bruttoWithDiscount != null &&
    statistics.bruttoWithDiscount !== statistics.brutto
  ) {
    entries.push({
      labelKey: "fibu.rechnung.mitSkonto",
      value: statistics.bruttoWithDiscount,
      kind: "currency",
      tone: "plain",
    });
  }
  entries.push(
    {
      labelKey: "fibu.common.netto",
      value: statistics.netto,
      kind: "currency",
      tone: "plain",
    },
    {
      labelKey: "fibu.rechnung.offen",
      value: statistics.open,
      kind: "currency",
      tone: "open",
    }
  );
  const optional: InvoiceStatisticsEntry[] = [
    {
      labelKey: "fibu.rechnung.filter.ueberfaellig",
      value: statistics.overdue,
      kind: "currency",
      tone: "overdue",
    },
    {
      labelKey: "fibu.rechnung.skonto",
      value: statistics.discount,
      kind: "currency",
      tone: "plain",
    },
    {
      labelKey: "fibu.rechnung.zahlungsZiel",
      value: statistics.paymentTargetAverage,
      kind: "days",
      tone: "plain",
    },
    {
      labelKey: "fibu.rechnung.zahlungsZiel.actual",
      value: statistics.actualPaymentTargetAverage,
      kind: "days",
      tone: "plain",
    },
  ];
  return [...entries, ...optional.filter((entry) => !!entry.value)];
}
