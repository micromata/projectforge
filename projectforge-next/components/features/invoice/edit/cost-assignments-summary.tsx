"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatCurrency,
  formatDisplayName,
  formatPercentageDecimal,
} from "@/lib/format";
import { cn } from "@/lib/utils";
import { shareOfNetSum } from "../invoice-values";
import type { KostZuweisungValues } from "../invoice-schema";

/**
 * The cost assignments of a position as one line each, for a row that is folded shut.
 *
 * Folded rows are the normal state of a stored invoice ([PositionRow] opens only what was just added), so
 * a split that is only visible once the row is open is a split nobody checks — and whether the amounts
 * add up is exactly what a reader scanning an invoice is after. The editable form of the same data is
 * [CostAssignmentsSection]; this is read-only and deliberately terse.
 *
 * Deleted assignments are left out: they are on their way out of the invoice and say nothing about what
 * it currently splits. An assignment with nothing filled in yet is named rather than skipped, so a row
 * that exists is never invisible.
 */
export function CostAssignmentsSummary({
  assignments,
  positionNetSum,
  className,
}: {
  assignments: readonly KostZuweisungValues[];
  /** The position's net sum, for the share each line carries of it; absent until the sums arrive. */
  positionNetSum?: number | null;
  className?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const live = assignments.filter((assignment) => !assignment.deleted);
  if (live.length === 0) return null;

  return (
    <span className={cn("flex min-w-0 flex-col gap-0.5", className)}>
      {live.map((assignment, index) => {
        // The order of the open row's fields, so the two readings match: cost 1, cost 2, amount, the
        // share that is of the position, why.
        const share = shareOfNetSum(assignment.netto, positionNetSum);
        const parts = [
          formatDisplayName(assignment.kost1),
          formatDisplayName(assignment.kost2),
          assignment.netto != null
            ? formatCurrency(assignment.netto, format)
            : "",
          // Whole percent, as in the open row ([CostAssignmentShare]).
          share != null ? formatPercentageDecimal(share, format, 0) : "",
          assignment.comment ?? "",
        ].filter(Boolean);
        return (
          <span
            key={assignment.id ?? `new-${assignment.index ?? index}`}
            className="min-w-0 truncate text-xs text-muted-foreground tabular-nums"
          >
            {parts.length > 0 ? (
              parts.join(" · ")
            ) : (
              <span className="italic">
                {`${t("fibu.rechnung.showKostZuweisungen")} ${index + 1}`}
              </span>
            )}
          </span>
        );
      })}
    </span>
  );
}
