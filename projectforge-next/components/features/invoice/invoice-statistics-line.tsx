"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import {
  invoiceStatisticsEntries,
  type InvoiceStatistics,
  type InvoiceStatisticsTone,
} from "./invoice-statistics";
import { CurrencyConversionWarnings } from "./currency-conversion-warnings";

/** The brand token each tone reads in — blue and red as the Wicket list colours them. */
const TONE_CLASS: Record<InvoiceStatisticsTone, string> = {
  plain: "",
  open: "text-brand-teal",
  overdue: "text-brand-pink",
};

/**
 * The statistics of the whole invoice list above its table, the way Wicket's list page shows them
 * (`AbstractRechnungListForm.addStatistics`): the sums, the two average payment targets, and a warning
 * for every invoice whose currency could not be converted.
 *
 * The numbers are the backend's ([InvoiceStatistics], computed over the result set of the same filter) —
 * summing the loaded rows here would answer differently for what is open and what is overdue, and would
 * put `RechnungCalculator`'s rules into the browser a second time.
 */
export function InvoiceStatisticsLine({
  statistics,
  isFetching,
  className,
}: {
  statistics: InvoiceStatistics | undefined;
  /** Dims the line while a new result set is on its way, so a stale sum doesn't read as final. */
  isFetching?: boolean;
  className?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const entries = invoiceStatisticsEntries(statistics);
  if (entries.length === 0) return null;

  return (
    <div className={cn(isFetching && "opacity-60", className)}>
      <CurrencyConversionWarnings
        warnings={statistics?.currencyConversionWarnings}
      />
      <dl
        className="flex flex-wrap items-baseline gap-x-4 gap-y-1 border-b bg-muted/40 px-4 py-1.5 text-[13px]"
        aria-label={t("statistics")}
      >
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
    </div>
  );
}
