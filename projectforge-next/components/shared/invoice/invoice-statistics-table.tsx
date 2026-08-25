"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatCurrency,
  formatDateRange,
  type FormatContext,
} from "@/lib/format";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  TONE_CLASS,
  type InvoiceComparisonEntry,
  type InvoiceStatisticsEntry,
} from "./invoice-statistics";

/** An entry's value the way the line writes it: `Ø n` days, otherwise an amount in the user's currency. */
function formatValue(
  entry: InvoiceStatisticsEntry,
  format: FormatContext
): string {
  return entry.kind === "days"
    ? `Ø ${entry.value ?? 0}`
    : formatCurrency(entry.value, format);
}

/** The change to the current period as `+12 %` / `−5 %`, or null when there is no base to grow from. */
function formatDelta(deltaPercent: number | null): string | null {
  if (deltaPercent === null) return null;
  const rounded = Math.round(deltaPercent);
  // A real minus sign and an explicit plus, so the direction reads at a glance without colour.
  const sign = rounded > 0 ? "+" : rounded < 0 ? "−" : "";
  return `${sign}${Math.abs(rounded)} %`;
}

/**
 * The header of a metric column. The actual payment target reads by a short label ("tatsächliches ZZ"):
 * its full name is the widest of them all and would set the column width for a value of "Ø 66".
 */
function headerLabel(labelKey: string, has: (key: string) => boolean): string {
  return labelKey === "fibu.rechnung.zahlungsZiel.actual"
    ? "fibu.rechnung.zahlungsZiel.actualShort"
    : leafKeyOf(labelKey, has);
}

/**
 * The invoice statistics as a table when the previous-year comparison is on: the metrics across the top,
 * one row for the current period and one for the year earlier. Each metric spans two sub-columns — the
 * amount and its year-on-year change — so the two amounts line up under each other and the changes sit in
 * their own column, which the wrapping inline line cannot do (see [InvoiceStatisticsLine]).
 *
 * `current` and `comparison` are aligned by construction — [invoiceComparisonEntries] maps over the same
 * [invoiceStatisticsEntries] — so the columns of the two rows match one for one.
 *
 * `corner` is rendered in the empty top-left cell: the caret that collapses the comparison back to the
 * single line (the list passes it; the mass-update summary has no toggle and passes nothing).
 */
export function InvoiceStatisticsTable({
  current,
  comparison,
  previousPeriod,
  corner,
}: {
  current: InvoiceStatisticsEntry[];
  comparison: InvoiceComparisonEntry[];
  /**
   * The window the "Vorjahr" row compares against — the invoice-date range shifted a year back. Shown as
   * a "*" footnote on the row's label, because "Vorjahr" over a year-to-date range is a partial year, not
   * a whole one; null when there is no bounded range (then the comparison is not shown at all).
   */
  previousPeriod?: { from: string; to: string } | null;
  corner?: ReactNode;
}) {
  const t = useTranslations();
  const format = useFormatContext();

  return (
    <div className="overflow-x-auto border-b bg-muted/40">
      <table className="text-[13px]" aria-label={t("statistics")}>
        <thead>
          <tr>
            <th className="px-2 py-1 text-left">{corner}</th>
            {current.map((entry) => (
              <th
                key={entry.labelKey}
                colSpan={2}
                className={cn(
                  "px-4 py-1 text-center text-[11px] font-normal opacity-70",
                  TONE_CLASS[entry.tone]
                )}
              >
                {t(headerLabel(entry.labelKey, t.has))}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          <tr>
            <th
              scope="row"
              className="px-4 py-0.5 text-left text-[11px] font-medium opacity-70"
            >
              {t("fibu.rechnung.statistics.currentPeriod")}
            </th>
            {current.map((entry) => (
              <MetricCells
                key={entry.labelKey}
                tone={entry.tone}
                amount={formatValue(entry, format)}
              />
            ))}
          </tr>
          <tr className="opacity-70">
            <th
              scope="row"
              className="px-4 py-0.5 text-left text-[11px] font-medium"
            >
              {t("fibu.rechnung.statistics.previousYear")}
              {previousPeriod && (
                <HintTooltip
                  title={t("fibu.rechnung.statistics.previousYearPeriod")}
                  text={formatDateRange(
                    previousPeriod.from,
                    previousPeriod.to,
                    format
                  )}
                  plain
                >
                  {/* A footnote marker, not a control: the caret in the corner already toggles the row. */}
                  <sup className="ml-0.5 cursor-help">*</sup>
                </HintTooltip>
              )}
            </th>
            {comparison.map((entry) => (
              <MetricCells
                key={entry.labelKey}
                tone={entry.tone}
                amount={formatValue(entry, format)}
                delta={formatDelta(entry.deltaPercent)}
              />
            ))}
          </tr>
        </tbody>
      </table>
    </div>
  );
}

/** One metric's two cells of a data row: the amount (right-aligned) and its change in its own column. */
function MetricCells({
  tone,
  amount,
  delta,
}: {
  tone: InvoiceStatisticsEntry["tone"];
  amount: string;
  delta?: string | null;
}) {
  return (
    <>
      <td
        className={cn("py-0.5 pl-4 text-right tabular-nums", TONE_CLASS[tone])}
      >
        {amount}
      </td>
      <td
        className={cn(
          "py-0.5 pr-4 pl-1 text-left text-[11px] tabular-nums opacity-70",
          TONE_CLASS[tone]
        )}
      >
        {delta}
      </td>
    </>
  );
}
