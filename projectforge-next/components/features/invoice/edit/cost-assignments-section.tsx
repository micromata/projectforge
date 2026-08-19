"use client";

import { useTranslations } from "next-intl";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useFieldArray } from "@/hooks/use-field-array";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import {
  emptyKostZuweisungValues,
  nextKostZuweisungIndex,
  remainingNet,
} from "../invoice-values";
import { useProjectKost2 } from "../use-project-kost2";
import { CostAssignmentRow } from "./cost-assignment-row";
import type { KostZuweisungValues } from "../invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice";

export interface CostAssignmentsSectionProps {
  /** Prefix of the enclosing position's fields, e.g. `positionen[1].`. */
  prefix: string;
  sums: InvoicePositionSums | undefined;
  /** Absent where the invoice may not be written — then the rows are read-only. */
  writeAccess: boolean;
  className?: string;
}

/**
 * The cost assignments of one invoice position — the third nesting level of this form, and the reason
 * [useFieldArray] had to learn bracketed paths at all: this array lives at
 * `positionen[1].kostZuweisungen`, not at a property of the invoice.
 *
 * Inline inside the position rather than in the modal Wicket opens (`KostZuweisungEditPanel`): what a
 * split has to add up to is the position's net sum, which is on screen here — and the whole invoice is
 * then one form with one submit, instead of a dialog saving on its own.
 *
 * Rendered only where cost accounting is configured; see [PositionRow], which decides that from
 * `Rechnung.costConfigured`.
 */
export function CostAssignmentsSection({
  prefix,
  sums,
  writeAccess,
  className,
}: CostAssignmentsSectionProps) {
  const t = useTranslations();
  const format = useFormatContext();
  const array = useFieldArray<KostZuweisungValues>(`${prefix}kostZuweisungen`);
  // The first cost unit of the invoice's project, for the very first row of a position.
  const defaultKost2 = useProjectKost2();
  // Negated by `RechnungPosInfo`: an unassigned rest of 400,00 € arrives as -400,00 (see
  // InvoicePositionSums). Shown as the amount that is still missing, which is how Wicket words it.
  const fehlbetrag = sums?.kostZuweisungNetFehlbetrag;

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="text-xs font-medium text-muted-foreground">
          {t("fibu.rechnung.showKostZuweisungen")}
        </span>
        <span className="flex flex-wrap items-baseline gap-x-4 text-xs tabular-nums">
          <span className="text-muted-foreground">
            {`${t("fibu.common.netto")}: ${formatCurrency(sums?.kostZuweisungNetSum, format)}`}
          </span>
          {/* Only where something is actually missing: a difference of zero is the normal case and a
              permanent "0,00 €" beside every position would read as a complaint. */}
          {fehlbetrag != null && fehlbetrag !== 0 && (
            <span className="font-semibold text-destructive">
              {`${t("fibu.rechnung.kostZuweisungFehlbetrag")}: ${formatCurrency(fehlbetrag, format)}`}
            </span>
          )}
        </span>
      </div>
      <RepeatableList
        array={array}
        emptyText={t("fibu.rechnung.showKostZuweisungen")}
        addLabel={
          writeAccess ? t("fibu.rechnung.tooltip.addKostZuweisung") : undefined
        }
        // Indexed here rather than only on save, for the reason a position is numbered here: the index
        // is what `KostZuweisungDO`'s identity is made of (see nextKostZuweisungIndex).
        onAdd={
          writeAccess
            ? () =>
                array.add(
                  emptyKostZuweisungValues(
                    nextKostZuweisungIndex(array.rows),
                    // The row it is added below, so its cost units carry over — splitting a position
                    // usually keeps cost 1.
                    array.rows[array.rows.length - 1],
                    // And what is left of the position, which is what the row is there to assign.
                    remainingNet(sums?.netSum, array.rows),
                    defaultKost2
                  )
                )
            : undefined
        }
        row={(assignment, index) => (
          <CostAssignmentRow
            prefix={array.fieldName(index, "")}
            index={index}
            deleted={assignment.deleted}
            netto={assignment.netto}
            positionNetSum={sums?.netSum}
            onRemove={writeAccess ? () => array.remove(index) : undefined}
            onRestore={writeAccess ? () => array.restore(index) : undefined}
          />
        )}
      />
    </div>
  );
}
