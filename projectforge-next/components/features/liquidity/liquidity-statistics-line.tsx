"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { useFormatContext } from "@/hooks/use-format";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { formatCurrency } from "@/lib/format";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import {
  liquidityStatisticsEntries,
  TONE_CLASS,
  type LiquidityStatistics,
} from "./liquidity-statistics";

/**
 * The statistics of the whole liquidity list above its table, the way the Wicket list shows them
 * (`LiquidityEntryListForm.addStatistics`): the total, what is paid, what is still open and what is overdue.
 *
 * The numbers are the backend's ([LiquidityStatistics], computed over the result set of the same filter) —
 * summing the loaded rows here would answer differently for what is open and what is overdue. When the base
 * date filter is in the past the whole line reads in the warning colour, as the Wicket list turns its
 * statistics box red then.
 */
export function LiquidityStatisticsLine({
  statistics,
  isFetching,
}: {
  statistics: LiquidityStatistics | undefined;
  /** Dims the line while a new result set is on its way, so a stale sum doesn't read as final. */
  isFetching?: boolean;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const entries = liquidityStatisticsEntries(statistics);
  if (entries.length === 0) return null;

  return (
    <dl
      className={cn(
        "flex flex-wrap items-baseline gap-x-4 gap-y-1 border-b bg-muted/40 px-4 py-1.5 text-[13px]",
        isFetching && "opacity-60",
        // A base date in the past: the figures are historical, so the line warns like the Wicket box.
        statistics?.pastBaseDate && "bg-brand-pink/10"
      )}
    >
      {statistics?.pastBaseDate && (
        // Says why the line is tinted: the chosen payment date is in the past, so the sums are the
        // historical state as of that date rather than the current liquidity.
        <HintTooltip
          text={t("plugins.liquidityplanning.statistics.pastBaseDate")}
          openOnTap
        >
          <span
            className="flex items-center text-brand-pink"
            aria-label={t("plugins.liquidityplanning.statistics.pastBaseDate")}
          >
            <HugeiconsIcon icon={AlertCircleIcon} size={15} />
          </span>
        </HintTooltip>
      )}
      {entries.map((entry) => (
        <div
          key={entry.labelKey}
          className={cn("flex items-baseline gap-1.5", TONE_CLASS[entry.tone])}
        >
          <dt className="text-[11px] opacity-70">
            {t(leafKeyOf(entry.labelKey, t.has))}
          </dt>
          <dd className="tabular-nums">
            {formatCurrency(entry.value, format)}
          </dd>
        </div>
      ))}
    </dl>
  );
}
