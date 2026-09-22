"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import { useInvoiceSums } from "./use-invoice-sums";

/**
 * The sums of the invoice, as the server computes them from what is currently in the form
 * ([useInvoiceSums]).
 *
 * Shown rather than editable, and computed there rather than here: rounding a position before it enters
 * a sum is German law and `RechnungCalculator`'s rule, and whether a discount was taken in time follows
 * from `RechnungInfo` — a second implementation in the browser would be a second answer.
 *
 * @param entity The REST category of the invoice on the page, handed to [useInvoiceSums].
 */
export function InvoiceSumsLine({
  entity,
  className,
}: {
  entity: string;
  className?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const { sums, isLoading } = useInvoiceSums(entity);

  const entries: [string, string][] = [
    ["fibu.common.netto", formatCurrency(sums?.netSum, format)],
    ["fibu.common.vatAmount", formatCurrency(sums?.vatAmount, format)],
    ["fibu.common.brutto", formatCurrency(sums?.grossSum, format)],
  ];
  // Only where it differs from the gross sum: on every invoice without a discount the two are equal, and
  // a second identical amount beside the first reads as an error rather than as information.
  if (
    sums?.grossSumWithDiscount != null &&
    sums.grossSumWithDiscount !== sums.grossSum
  ) {
    entries.push([
      "fibu.rechnung.mitSkonto",
      formatCurrency(sums.grossSumWithDiscount, format),
    ]);
  }

  const fehlbetrag = sums?.kostZuweisungenFehlbetrag;

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
          <dt className="text-[11px] opacity-70">{t(key)}</dt>
          <dd className="text-sm tabular-nums">{value}</dd>
        </div>
      ))}
      {/* The one number here that means something is wrong: net sums that are not fully assigned to cost
          units. Absent while everything adds up — a permanent "0,00 €" would read as a complaint. Not
          negated, unlike a position's (see `InvoiceSums`). */}
      {fehlbetrag != null && fehlbetrag !== 0 && (
        <div className="flex flex-col">
          <dt className="text-[11px] text-destructive opacity-70">
            {t("fibu.rechnung.kostZuweisungFehlbetrag")}
          </dt>
          <dd className="text-sm font-semibold text-destructive tabular-nums">
            {formatCurrency(fehlbetrag, format)}
          </dd>
        </div>
      )}
    </dl>
  );
}
