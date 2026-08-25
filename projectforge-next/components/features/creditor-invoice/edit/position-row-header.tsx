"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { CollapsibleSummary } from "@/components/shared/collapsible-summary";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import { CostAssignmentsSummary } from "@/components/shared/invoice/cost-assignments-summary";
import { usePositionDetailChips } from "./position-detail-chips";
import type { CreditorInvoicePositionValues } from "../creditor-invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice-sums";

export interface PositionRowHeaderProps {
  position: CreditorInvoicePositionValues;
  /** From `POST /rs/incomingInvoice/recalculate`; absent until the first answer arrives. */
  sums: InvoicePositionSums | undefined;
  /** Whether cost accounting is configured at all — see [PositionRow]. False hides the split. */
  costConfigured: boolean;
}

/**
 * What a collapsed incoming invoice position says: number, text and net sum on the first line, then its
 * remaining fields and its whole cost split below.
 *
 * The same rule as the outgoing invoice's header — folded is the normal state of a stored invoice, so
 * anything left out here is a field nobody reads back — but with a leaner position: no order link.
 */
export function PositionRowHeader({
  position,
  sums,
  costConfigured,
}: PositionRowHeaderProps) {
  const t = useTranslations();
  const format = useFormatContext();
  const chips = usePositionDetailChips(position, sums);
  const netSum = sums?.netSum;
  const fehlbetrag = sums?.kostZuweisungNetFehlbetrag;

  return (
    <CollapsibleSummary
      primary={
        <>
          <span className="shrink-0 text-muted-foreground">
            {t("label.position.short")}
            {position.number != null ? ` ${position.number}` : ""}
          </span>
          <span
            className={cn(
              "min-w-0 flex-1 truncate",
              !position.text && "text-muted-foreground italic"
            )}
          >
            {position.text || t("fibu.rechnung.text")}
          </span>
          {fehlbetrag != null && fehlbetrag !== 0 && (
            <HintTooltip text={t("fibu.rechnung.kostZuweisungFehlbetrag")}>
              <Badge
                variant="destructive"
                className="shrink-0 cursor-help font-normal tabular-nums"
              >
                {formatCurrency(fehlbetrag, format)}
              </Badge>
            </HintTooltip>
          )}
          <span className="shrink-0 tabular-nums">
            {formatCurrency(netSum, format)}
          </span>
        </>
      }
      details={chips.map((chip, i) => (
        <span key={i} className="tabular-nums">
          {/* The term in front of the value, dimmed: the numbers are what is read, the words only say
              which is which. */}
          {chip.label && <span className="opacity-70">{chip.label} </span>}
          {chip.value}
        </span>
      ))}
      // The whole cost split, which is several lines rather than a chip.
      extra={
        costConfigured && (
          <CostAssignmentsSummary
            assignments={position.kostZuweisungen}
            positionNetSum={netSum}
          />
        )
      }
    />
  );
}
