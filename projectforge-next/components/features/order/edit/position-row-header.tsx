"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { AUFTRAGS_POSITION_METADATA } from "@/lib/metadata/auftrags-position.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { cn } from "@/lib/utils";
import type { OrderPositionValues } from "../order-schema";
import type { OrderPositionSums } from "@/lib/rs/order";

const p = fromMetadata(AUFTRAGS_POSITION_METADATA);

export interface PositionRowHeaderProps {
  position: OrderPositionValues;
  /** From `/rs/order/recalculate`; absent for a position that has no number yet. */
  sums: OrderPositionSums | undefined;
}

/**
 * What a collapsed position says: its number, its title, its status and its net sum.
 *
 * Enough to find a position among a dozen without opening it, which is the whole point of the row being
 * collapsible. The net sum is the server's ([useOrderSums]) as soon as it answers, and the value the
 * user typed until then — so the number never lags behind the field right above it.
 */
export function PositionRowHeader({ position, sums }: PositionRowHeaderProps) {
  const t = useTranslations();
  const format = useFormatContext();
  const netSum = sums?.netSum ?? position.nettoSumme;
  // The same translated constants the select below offers, rather than a second lookup of its own.
  const status = p
    .enumOptions("status", t)
    .find((option) => option.value === position.status);

  return (
    <span className="flex min-w-0 flex-1 items-center gap-2 text-sm">
      <span className="shrink-0 text-muted-foreground">
        {t("label.position.short")}
        {position.number != null ? ` ${position.number}` : ""}
      </span>
      <span
        className={cn(
          "min-w-0 flex-1 truncate",
          !position.titel && "text-muted-foreground italic"
        )}
      >
        {position.titel || t("fibu.auftrag.position._")}
      </span>
      {status && (
        <Badge variant="secondary" className="shrink-0 font-normal">
          {status.label}
        </Badge>
      )}
      <span className="shrink-0 tabular-nums">
        {formatCurrency(netSum, format)}
      </span>
    </span>
  );
}
