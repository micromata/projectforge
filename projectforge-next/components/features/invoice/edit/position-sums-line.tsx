"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { InvoicePositionSums } from "@/lib/rs/invoice";

/**
 * Net, VAT and gross of one position, as the server computes them from what is currently in the form
 * ([useInvoiceSums]).
 *
 * Shown rather than editable, and computed there rather than here: quantity × unit price is rounded
 * before it enters any sum (`RechnungCalculator.roundPositionsBeforeSum`), which is German law, and a
 * second multiplication in the browser would be a second answer. Wicket shows the same three read-only
 * panels beneath the boxes.
 */
export function PositionSumsLine({
  sums,
  className,
}: {
  sums: InvoicePositionSums | undefined;
  className?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();

  const entries: [string, unknown][] = [
    ["fibu.common.netto", sums?.netSum],
    ["fibu.common.vatAmount", sums?.vatAmount],
    ["fibu.common.brutto", sums?.grossSum],
  ];

  return (
    <dl className={cn("flex flex-wrap gap-x-6 gap-y-2", className)}>
      {entries.map(([key, value]) => (
        <div key={key} className="flex flex-col">
          {/* The same shape as the invoice's own sums line ([InvoiceSumsLine]): a reader who knows
              „Brutto" from the banner should meet it here in the same form. */}
          <dt className="text-[11px] opacity-70">{t(key)}</dt>
          <dd className="text-sm tabular-nums">
            {formatCurrency(value, format)}
          </dd>
        </div>
      ))}
    </dl>
  );
}
