"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatNumber } from "@/lib/format";
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
    </dl>
  );
}
