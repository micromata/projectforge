"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { plusYears } from "@/lib/date-period-math";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import type { MagicFilter } from "@/lib/rs/types";
import {
  invoiceComparisonEntries,
  invoiceStatisticsEntries,
  TONE_CLASS,
  type InvoiceStatistics,
} from "./invoice-statistics";
import { CurrencyConversionWarnings } from "./currency-conversion-warnings";
import {
  ComparisonCaret,
  ComparisonToggleRegion,
} from "./invoice-comparison-toggle";
import { InvoiceStatisticsTable } from "./invoice-statistics-table";

/**
 * The invoice-date filter, the field the backend shifts by a year for the comparison
 * (`OutgoingInvoiceEntityRest.DATE_FIELD`). A year-earlier period exists only when both bounds are set.
 */
const DATE_FIELD = "datum";

/** The gross-with-discount column is dropped from the comparison table — see [InvoiceStatisticsLine]. */
const MIT_SKONTO = "fibu.rechnung.mitSkonto";

/** The bounded Rechnungsdatum range of the filter, or null when either bound is missing. */
function boundedDateRange(
  filter: MagicFilter | undefined
): { from: string; to: string } | null {
  const value = filter?.entries.find(
    (entry) => entry.field === DATE_FIELD
  )?.value;
  return value?.from && value?.to ? { from: value.from, to: value.to } : null;
}

/**
 * The window the previous-year row actually compares against: the invoice-date range shifted twelve
 * months back, exactly as the backend builds it (`OutgoingInvoiceEntityRest.previousYearFilter` does
 * `minusYears(1)` on both bounds and drops the period kind). Reconstructed here so the label can name
 * the dates — "Vorjahr" over a year-to-date range means a partial year, not 01.01.–31.12.
 */
function previousYearPeriod(
  range: { from: string; to: string } | null
): { from: string; to: string } | null {
  if (!range) return null;
  return { from: plusYears(range.from, -1), to: plusYears(range.to, -1) };
}

/**
 * The statistics of the whole invoice list above its table, the way Wicket's list page shows them
 * (`AbstractRechnungListForm.addStatistics`): the sums, the two average payment targets, and a warning
 * for every invoice whose currency could not be converted.
 *
 * The numbers are the backend's ([InvoiceStatistics], computed over the result set of the same filter) —
 * summing the loaded rows here would answer differently for what is open and what is overdue, and would
 * put `RechnungCalculator`'s rules into the browser a second time.
 *
 * The list passes `filter` and the `previousYearComparison` toggle: a caret then expands the same figures
 * a year earlier into a table (see [InvoiceStatisticsTable]). The mass-update summary reuses this line
 * without them — no caret, no comparison.
 */
export function InvoiceStatisticsLine({
  statistics,
  isFetching,
  className,
  filter,
  previousYearComparison,
  setPreviousYearComparison,
}: {
  statistics: InvoiceStatistics | undefined;
  /** Dims the line while a new result set is on its way, so a stale sum doesn't read as final. */
  isFetching?: boolean;
  className?: string;
  filter?: MagicFilter;
  previousYearComparison?: boolean;
  setPreviousYearComparison?: (on: boolean) => void;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const entries = invoiceStatisticsEntries(statistics);
  if (entries.length === 0) return null;

  // The year-earlier figures, when the backend sent them (the toggle is on and the date filter is a
  // bounded range); "mit Skonto" is dropped from the table, so the two rows compare the same columns.
  const comparison = invoiceComparisonEntries(
    statistics,
    statistics?.previousYear
  ).filter((entry) => entry.labelKey !== MIT_SKONTO);
  const tableEntries = entries.filter((entry) => entry.labelKey !== MIT_SKONTO);
  const expanded = !!previousYearComparison && comparison.length > 0;

  const dateRange = boundedDateRange(filter);
  // A decorative caret only — the click lives on the wrapping region, so the whole line (or the whole
  // table) toggles, not just this icon (see ComparisonToggleRegion).
  const caret = setPreviousYearComparison ? (
    <ComparisonCaret expanded={expanded} canCompare={!!dateRange} />
  ) : null;

  return (
    <div className={cn(isFetching && "opacity-60", className)}>
      <CurrencyConversionWarnings
        warnings={statistics?.currencyConversionWarnings}
      />
      <ComparisonToggleRegion
        expanded={expanded}
        canCompare={!!dateRange}
        onToggle={setPreviousYearComparison}
      >
        {expanded ? (
          // The same period a year earlier next to now: a table lines the two amounts and each change up
          // in a column, which the wrapping line cannot (see InvoiceStatisticsTable). The caret sits in
          // its corner; clicking anywhere on the table collapses it again.
          <InvoiceStatisticsTable
            current={tableEntries}
            comparison={comparison}
            previousPeriod={previousYearPeriod(dateRange)}
            corner={caret}
          />
        ) : (
          <dl className="flex flex-wrap items-baseline gap-x-4 gap-y-1 border-b bg-muted/40 px-4 py-1.5 text-[13px]">
            {caret && <div className="flex items-center">{caret}</div>}
            {entries.map((entry) => (
              <div
                key={entry.labelKey}
                className={cn(
                  "flex items-baseline gap-1.5",
                  TONE_CLASS[entry.tone]
                )}
              >
                {/* Quiet labels and plain values: the line carries up to seven of them, and emphasizing all
                  emphasizes none — what is open and what is overdue are what stands out, as in the legacy
                  list. The wording is the bundle's, so it reads as written ("Zahlungsziel"). */}
                <dt className="text-[11px] opacity-70">
                  {/* `fibu.rechnung.zahlungsZiel` is a text and the parent of `.actual` — see leafKeyOf. */}
                  {t(leafKeyOf(entry.labelKey, t.has))}
                </dt>
                <dd className="tabular-nums">
                  {entry.kind === "days"
                    ? // "Ø 30" — days, as the Wicket panel writes them. No unit: both entries are labelled
                      // as a payment target, which is measured in nothing else.
                      `Ø ${entry.value ?? 0}`
                    : formatCurrency(entry.value, format)}
                </dd>
              </div>
            ))}
          </dl>
        )}
      </ComparisonToggleRegion>
    </div>
  );
}
