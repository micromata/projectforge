"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { CollapsibleSummary } from "@/components/shared/collapsible-summary";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import { CostAssignmentsSummary } from "@/components/shared/invoice/cost-assignments-summary";
import { OrderPositionLink } from "./order-position-link";
import { usePositionDetailChips } from "./position-detail-chips";
import type { InvoicePositionValues } from "../invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice";

export interface PositionRowHeaderProps {
  position: InvoicePositionValues;
  /** From `POST /rs/outgoingInvoice/recalculate`; absent until the first answer arrives. */
  sums: InvoicePositionSums | undefined;
  /** Whether cost accounting is configured at all — see [PositionRow]. False hides the split. */
  costConfigured: boolean;
}

/**
 * What a collapsed invoice position says: number, text and net sum on the first line, then **every**
 * remaining field of the position and its whole cost split below.
 *
 * Complete rather than a teaser, because folded is the normal state of a stored invoice ([PositionRow]
 * opens only what was just added) — anything left out here is a field nobody reads back. Everything
 * below the first line steps aside once the row is open, where the same values sit in the fields
 * ([CollapsibleSummary]). The order's header follows the same rule; what differs is that an invoice
 * position's amounts need naming ([usePositionDetailChips]) and that its cost assignments are a list of
 * their own ([CostAssignmentsSummary]).
 *
 * The Fehlbetrag sits on the first line rather than among the chips: a position whose assignments do not
 * add up is what a reader scanning an invoice is looking for (Wicket paints the same number red).
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
  const order = position.auftragsPosition;

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
      details={[
        ...chips.map((chip, i) => (
          <span key={i} className="tabular-nums">
            {/* The term in front of the value, dimmed: the numbers are what is read, the words only
                say which is which. */}
            {chip.label && <span className="opacity-70">{chip.label} </span>}
            {chip.value}
          </span>
        )),
        // The order this position bills, as a link to it — the same link the picker of the unfolded
        // row carries (see OrderPositionLink).
        order?.auftragId != null && <OrderPositionLink order={order} />,
      ]}
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
