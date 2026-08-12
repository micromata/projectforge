"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatDate, formatNumber } from "@/lib/format";
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
 * What a collapsed position says: primary line with number/title/status/sum, secondary line with
 * all other filled fields so nothing is hidden when the row is folded.
 */
export function PositionRowHeader({ position, sums }: PositionRowHeaderProps) {
  const t = useTranslations();
  const format = useFormatContext();
  const netSum = sums?.netSum ?? position.nettoSumme;
  const status = p
    .enumOptions("status", t)
    .find((o) => o.value === position.status);
  const art = p
    .enumOptions("art", t)
    .find((o) => o.value === position.art);
  const paymentType = p
    .enumOptions("paymentType", t)
    .find((o) => o.value === position.paymentType);
  const forecastType = p
    .enumOptions("forecastType", t)
    .find((o) => o.value === position.forecastType);
  const modeOfPayment = p
    .enumOptions("modeOfPaymentType", t)
    .find((o) => o.value === position.modeOfPaymentType);

  const ownPeriod = position.periodOfPerformanceType === "OWN";
  const periodLabel =
    ownPeriod && position.periodOfPerformanceBegin
      ? `${formatDate(position.periodOfPerformanceBegin, format)} – ${formatDate(position.periodOfPerformanceEnd, format)}`
      : null;

  const secondaryChips: string[] = [];
  if (art) secondaryChips.push(art.label);
  if (paymentType) secondaryChips.push(paymentType.label);
  if (forecastType) secondaryChips.push(forecastType.label);
  if (modeOfPayment) secondaryChips.push(modeOfPayment.label);
  if (position.personDays != null && position.personDays !== 0)
    secondaryChips.push(
      `${formatNumber(position.personDays, format, 2)} ${t("projectmanagement.personDays.short")}`
    );
  if (periodLabel) secondaryChips.push(periodLabel);
  if (position.task?.displayName) secondaryChips.push(position.task.displayName);
  if (position.vollstaendigFakturiert) secondaryChips.push(t("fibu.auftrag.vollstaendigFakturiert"));
  if (position.bemerkung) secondaryChips.push(position.bemerkung);

  return (
    <span className="flex min-w-0 flex-1 flex-col gap-0.5">
      {/* Primary line — quick scan */}
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
      {/* Secondary line — all filled detail fields, compact single line */}
      {secondaryChips.length > 0 && (
        <span className="flex min-w-0 items-baseline gap-x-2 overflow-hidden text-xs text-muted-foreground">
          {secondaryChips.map((chip, i) => (
            <span
              key={i}
              className={cn(
                "shrink-0",
                chip === position.bemerkung && "min-w-0 shrink truncate italic"
              )}
            >
              {chip}
            </span>
          ))}
        </span>
      )}
    </span>
  );
}
