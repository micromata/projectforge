"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatPercentageDecimal } from "@/lib/format";
import { cn } from "@/lib/utils";
import { shareOfNetSum } from "../invoice-values";

/**
 * What share of its position a cost assignment carries, beside the amount it is derived from.
 *
 * A read-only label/value pair rather than a [FieldShell], which ties a label to a control there is
 * none of here (the same shape a book's loan line uses). Computed in the browser and not taken from the
 * recalculated sums, unlike every amount on this form: a ratio of two numbers carries no rounding rule
 * worth protecting, and this way it follows the amount as it is typed instead of after the debounce.
 *
 * Whole percent, as Wicket's `Prozent` column reads — its `BigDecimal.divide` inherits the amount's
 * scale of two digits, so a position split in thirds shows 33 % three times there. Kept, because that is
 * what the number is for: whether a row is roughly half or a tenth of the position.
 */
export function CostAssignmentShare({
  netto,
  positionNetSum,
  className,
}: {
  netto: number | null | undefined;
  /** From the recalculated sums; absent until the first answer arrives. */
  positionNetSum: number | null | undefined;
  className?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  // Nothing rather than "0 %": an amount that is not filled in yet has no share, and neither has a row
  // of a position that sums to nothing. Wicket blanks its cell in the same cases.
  const share = shareOfNetSum(netto, positionNetSum);

  return (
    <span className={cn("flex flex-col gap-1.5", className)}>
      <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {t("percent")}
      </span>
      {/* Aligned with the boxes beside it, which carry a border and a padding this text has not. */}
      <span className="py-2 text-sm tabular-nums">
        {share == null ? "" : formatPercentageDecimal(share, format, 0)}
      </span>
    </span>
  );
}
