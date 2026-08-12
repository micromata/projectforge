"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import {
  orderStatisticsEntries,
  type OrderStatistics,
  type OrderStatisticsTone,
} from "./order-statistics";

/** The brand token each tone reads in — blue and red as the Wicket list colours them. */
const TONE_CLASS: Record<OrderStatisticsTone, string> = {
  plain: "",
  commissioned: "text-brand-teal",
  toBeInvoiced: "text-brand-pink",
};

/**
 * The statistics of the whole order list above its table: the six sums with the number of orders each
 * of them sums, the way the Wicket page shows them (`AuftragListForm.addStatistics`).
 *
 * The numbers are the backend's ([OrderStatistics], computed over the result set of the same filter) —
 * summing the loaded rows here would answer differently for the two sums no list column carries, and
 * would put `OrderInfo`'s rules into the browser a second time.
 */
export function OrderStatisticsLine({
  statistics,
  isFetching,
  className,
}: {
  statistics: OrderStatistics | undefined;
  /** Dims the line while a new result set is on its way, so a stale sum doesn't read as final. */
  isFetching?: boolean;
  className?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const entries = orderStatisticsEntries(statistics);
  if (entries.length === 0) return null;

  return (
    <dl
      className={cn(
        "flex flex-wrap items-baseline gap-x-4 gap-y-1 border-b bg-muted/40 px-4 py-1.5 text-[13px]",
        isFetching && "opacity-60",
        className
      )}
      aria-label={t("statistics")}
    >
      {entries.map((entry) => (
        <div
          key={entry.labelKey}
          className={cn("flex items-baseline gap-1.5", TONE_CLASS[entry.tone])}
        >
          {/* Neither bold nor upper case: the line carries six values, and emphasizing all of them
              emphasizes none — the two coloured entries are what stands out, as in the legacy list.
              Upper case would also cost the width six labels don't have on a narrow screen. The
              wording is the bundle's, so it reads as written ("zu fakturieren"). */}
          <dt className="text-[11px] opacity-70">{t(entry.labelKey)}</dt>
          <dd className="tabular-nums">
            {formatCurrency(entry.amount, format)}
            {/* The number of orders behind the sum, as "(42)" — quieter than the amount, which is
                what a reader compares. */}
            <span className="ml-0.5 text-[11px] opacity-70">
              ({entry.count})
            </span>
          </dd>
        </div>
      ))}
    </dl>
  );
}
