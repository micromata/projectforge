"use client";

import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * How much of an invoice's net sum is not assigned to a cost unit — the one amount of the list that
 * means something is wrong, so an amount other than zero reads red.
 *
 * This is what Wicket's `showKostZuweisungStatus` switch produced, which appended `*** 1.400,00 € ***`
 * to the number cell of every such row (`RechnungListPage`). A column of its own instead: it can be
 * sorted, it can be switched off, and it doesn't disfigure the invoice number.
 *
 * Zero shows as zero rather than as nothing, unlike the form's line (`InvoiceSumsLine`): a column the
 * user switched on is read row by row, and an empty cell there is indistinguishable from a value that
 * never arrived. Nothing arrived is the other case — an invoice of an installation without cost
 * accounting — and that one stays empty.
 */
export function CostAssignmentCell({ value }: { value: number | null }) {
  const format = useFormatContext();
  if (value == null) return null;
  return (
    <span
      className={cn(
        "font-mono tabular-nums",
        value !== 0 && "font-semibold text-destructive"
      )}
    >
      {formatCurrency(value, format)}
    </span>
  );
}
