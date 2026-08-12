"use client";

import { useTranslations } from "next-intl";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatCurrency,
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

  const entries: [string, string][] = [
    // `._` because the key is a text of its own *and* the parent of `fibu.auftrag.nettoSumme.weighted`,
    // which the generator can only express as a nested object plus a `_` leaf.
    ["fibu.auftrag.nettoSumme._", formatCurrency(sums?.netSum, format)],
    [
      "fibu.auftrag.commissioned",
      formatCurrency(sums?.commissionedNetSum, format),
    ],
    ["fibu.fakturiert", formatCurrency(sums?.invoicedSum, format)],
    ["fibu.toBeInvoiced", formatCurrency(sums?.toBeInvoicedSum, format)],
    [
      "projectmanagement.personDays._",
      formatNumber(sums?.personDays, format, 2),
    ],
  ];

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
      {entries.map(([key, value]) => (
        <div key={key} className="flex flex-col">
          <dt className="text-[11.5px] font-semibold tracking-wide text-muted-foreground uppercase">
            {t(key)}
          </dt>
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
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <dt className="cursor-help text-[11.5px] font-semibold tracking-wide text-primary uppercase decoration-dotted underline-offset-2 hover:underline">
              {/* `._` because the key is a text of its own *and* the parent of `.info` — see the sums above. */}
              {t("fibu.auftrag.probabilityOfOccurrence.weighted._")}
            </dt>
          </TooltipTrigger>
          <TooltipContent className="max-w-xs">
            {t("fibu.auftrag.probabilityOfOccurrence.weighted.info")}
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
      <dd className="text-base font-semibold text-primary tabular-nums">
        {formatPercentageDecimal(value, format)}
      </dd>
    </div>
  );
}
