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
  /**
   * The same statistics for the same period one calendar year earlier, when the user asked for the
   * comparison and the invoice-date filter is a bounded range — the backend fills it only on the
   * top-level object (its own `previousYear` stays null). See `OutgoingInvoiceEntityRest`.
   */
  previousYear?: InvoiceStatistics | null;
}

/**
 * How an entry reads — the two the Wicket list emphasizes (`TextStyle.BLUE` on what is still open,
 * `TextStyle.RED` on what is overdue), everything else plain.
 *
 * Named by meaning, not by colour: the component maps it to a brand token, so a theme change happens in
 * one place (see app/globals.css).
 */
export type InvoiceStatisticsTone = "plain" | "open" | "overdue";

/**
 * The brand token each tone reads in — blue and red as the Wicket list colours them. A plain string map
 * rather than a component concern, so the line and the comparison table read from one place.
 */
export const TONE_CLASS: Record<InvoiceStatisticsTone, string> = {
  plain: "",
  open: "text-brand-teal",
  overdue: "text-brand-pink",
};

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

/** One entry of the muted "Vorjahr" row: the year-earlier value and its change from it to now. */
export interface InvoiceComparisonEntry extends InvoiceStatisticsEntry {
  /** The value of the same entry one calendar year earlier — what this row's amount reads. */
  value?: number | null;
  /**
   * Percentage change from the previous value to the current one, `(current − previous) / |previous|`
   * as a percentage. Null when it cannot be formed — the previous value is zero or absent, so there is
   * no base to grow from — in which case the row shows the amount without a delta.
   */
  deltaPercent: number | null;
}

/**
 * The raw value behind a statistics entry, read straight off the object rather than off the displayed
 * line: the previous-year row must show the true year-earlier figure even where the current line drops
 * it (a zero overdue, a discount not taken). Kept next to [invoiceStatisticsEntries] so the two stay in
 * step — a new entry there needs a case here.
 */
function valueByLabelKey(
  statistics: InvoiceStatistics,
  labelKey: string
): number | null | undefined {
  switch (labelKey) {
    case "fibu.common.brutto":
      return statistics.brutto;
    case "fibu.rechnung.mitSkonto":
      return statistics.bruttoWithDiscount;
    case "fibu.common.netto":
      return statistics.netto;
    case "fibu.rechnung.offen":
      return statistics.open;
    case "fibu.rechnung.filter.ueberfaellig":
      return statistics.overdue;
    case "fibu.rechnung.skonto":
      return statistics.discount;
    case "fibu.rechnung.zahlungsZiel":
      return statistics.paymentTargetAverage;
    case "fibu.rechnung.zahlungsZiel.actual":
      return statistics.actualPaymentTargetAverage;
    default:
      return undefined;
  }
}

/**
 * The "Vorjahr" row: the same entries the current line shows (same labels, same order), each carrying its
 * year-earlier value and the change to now. Aligned to the current entries on purpose — a column the
 * current line omits has nothing to compare against, and one only the previous year had would sit under
 * an empty current cell.
 */
export function invoiceComparisonEntries(
  current: InvoiceStatistics | undefined,
  previous: InvoiceStatistics | undefined | null
): InvoiceComparisonEntry[] {
  if (!current || !previous) return [];
  return invoiceStatisticsEntries(current).map((entry) => {
    const currentValue = entry.value ?? 0;
    const previousValue = valueByLabelKey(previous, entry.labelKey) ?? 0;
    // No base to grow from: a percentage against zero is either undefined or infinite, so the row
    // shows the amount alone.
    const deltaPercent =
      previousValue !== 0
        ? ((currentValue - previousValue) / Math.abs(previousValue)) * 100
        : null;
    return { ...entry, value: previousValue, deltaPercent };
  });
}
