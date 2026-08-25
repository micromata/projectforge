"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatCurrency,
  formatDateRange,
  formatNumber,
  formatPercentageDecimal,
} from "@/lib/format";
import { cn } from "@/lib/utils";
import { useOrderSums } from "../use-order-sums";

/**
 * The sums of the order, as the server computes them from what is currently in the form
 * ([useOrderSums]).
 *
 * Shown rather than editable, and computed there rather than here: which statuses count as commissioned
 * and how a probability of occurrence weighs into the acquisition sum is `OrderInfo`'s business — a
 * second implementation in the browser would be a second answer.
 */
export function OrderSumsLine({ className }: { className?: string }) {
  const t = useTranslations();
  const format = useFormatContext();
  const { sums, isLoading } = useOrderSums();

  const toBeInvoiced = sums?.toBeInvoicedSum;
  const entries: { key: string; value: string; className?: string }[] = [
    // `._` because the key is a text of its own *and* the parent of `fibu.auftrag.nettoSumme.weighted`,
    // which the generator can only express as a nested object plus a `_` leaf.
    {
      key: "fibu.auftrag.nettoSumme._",
      value: formatCurrency(sums?.netSum, format),
    },
    {
      key: "fibu.auftrag.commissioned",
      value: formatCurrency(sums?.commissionedNetSum, format),
    },
    {
      key: "fibu.fakturiert",
      value: formatCurrency(sums?.invoicedSum, format),
    },
    // Two different things, kept apart as the list statistics do (see order-statistics.ts):
    // „noch nicht fakturiert" is the commissioned amount not yet billed — an information — while
    // „zu fakturieren" is the part of it that is due now, the accounting staff's to-do.
    {
      key: "fibu.notYetInvoiced",
      value: formatCurrency(sums?.notYetInvoicedSum, format),
    },
    // „zu fakturieren" only when there is something due, and then in red — a 0,00 € to-do says
    // nothing, and the list statistics drop it just the same (order-statistics.ts). The colour is the
    // list's own token for this concept (`toBeInvoiced` tone, order-statistics-line.tsx).
    ...(toBeInvoiced != null && toBeInvoiced > 0
      ? [
          {
            key: "fibu.toBeInvoiced",
            value: formatCurrency(toBeInvoiced, format),
            className: "text-brand-pink",
          },
        ]
      : []),
    {
      key: "projectmanagement.personDays._",
      value: formatNumber(sums?.personDays, format, 2),
    },
  ];

  // The period over *all* positions, beside the numbers it belongs with rather than in the head section:
  // it is what the order spans, while the two date boxes below are only what a position of type
  // "siehe oben" refers to (see OrderSums.periodOfPerformanceBegin). Left out entirely while neither end
  // is known — an empty "… – …" would read as a period nobody entered.
  const period = formatDateRange(
    sums?.periodOfPerformanceBegin,
    sums?.periodOfPerformanceEnd,
    format
  );
  if (period)
    entries.push({ key: "fibu.periodOfPerformance._", value: period });

  return (
    <dl
      className={cn(
        "flex flex-wrap gap-x-6 gap-y-2",
        // Dimmed while a recalculation is on its way, so a number that is about to change doesn't read
        // as final. The old values stay visible — blanking them would make the line jump.
        isLoading && "opacity-60",
        className
      )}
    >
      {entries.map(({ key, value, className: entryClassName }) => (
        <div key={key} className={cn("flex flex-col", entryClassName)}>
          {/* Neither bold nor upper case, and the same size as in the list statistics
              ([OrderStatisticsLine]): the two lines carry the same vocabulary, so a reader who knows
              „zu fakturieren" from the list should meet it here in the same shape — colour included. */}
          <dt className="text-[11px] opacity-70">{t(key)}</dt>
          <dd className="text-sm tabular-nums">{value}</dd>
        </div>
      ))}
      <WeightedProbability value={sums?.weightedProbabilityOfOccurrence} />
    </dl>
  );
}

/**
 * The probability the forecast actually applies, next to the sums it weighs.
 *
 * Set apart from them on purpose: it is the one number here that is not money, and the one a reader is
 * most likely to mistake for the `probabilityOfOccurrence` field above — that field is only the *given*
 * probability, which the statuses of the order and of each position may override entirely (see
 * `ForecastUtils.getProbabilityOfAccurence`, hence the tooltip saying so).
 *
 * Nothing is shown for an order whose positions carry no net sum: a weighted probability would be a
 * division by zero, and the backend answers null rather than a 0 % that would read as "lost".
 */
function WeightedProbability({ value }: { value?: number | null }) {
  const t = useTranslations();
  const format = useFormatContext();
  if (value == null) return null;
  return (
    <div className="flex flex-col">
      <HintTooltip
        text={t("fibu.auftrag.probabilityOfOccurrence.weighted.info")}
      >
        <dt className="cursor-help text-[11px] text-primary opacity-70 decoration-dotted underline-offset-2 hover:underline">
          {/* `._` because the key is a text of its own *and* the parent of `.info` — see the sums above. */}
          {t("fibu.auftrag.probabilityOfOccurrence.weighted._")}
        </dt>
      </HintTooltip>
      <dd className="text-base font-semibold text-primary tabular-nums">
        {formatPercentageDecimal(value, format)}
      </dd>
    </div>
  );
}
