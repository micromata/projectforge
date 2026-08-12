"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Invoice01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatDate, formatNumber } from "@/lib/format";
import { AUFTRAGS_POSITION_METADATA } from "@/lib/metadata/auftrags-position.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { cn } from "@/lib/utils";
import { TaskChip } from "@/components/shared/tasks/task-chip";
import type { OrderPositionValues } from "../order-schema";
import type { OrderPositionSums } from "@/lib/rs/order";
import type { PositionInvoiceInfo } from "../types";

const p = fromMetadata(AUFTRAGS_POSITION_METADATA);

export interface PositionRowHeaderProps {
  position: OrderPositionValues;
  /** From `/rs/order/recalculate`; absent for a position that has no number yet. */
  sums: OrderPositionSums | undefined;
  invoiceInfo?: PositionInvoiceInfo;
}

/**
 * What a collapsed position says: primary line with number/title/status/sum, secondary line with
 * all other filled fields so nothing is hidden when the row is folded.
 */
export function PositionRowHeader({
  position,
  sums,
  invoiceInfo,
}: PositionRowHeaderProps) {
  const t = useTranslations();
  const format = useFormatContext();
  const netSum = sums?.netSum ?? position.nettoSumme;
  const status = p
    .enumOptions("status", t)
    .find((o) => o.value === position.status);
  const art = p.enumOptions("art", t).find((o) => o.value === position.art);
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

  // Enum values shown as badges, plain text shown inline.
  const enumBadges: string[] = [];
  if (art) enumBadges.push(art.label);
  if (paymentType) enumBadges.push(paymentType.label);
  if (forecastType) enumBadges.push(forecastType.label);
  if (modeOfPayment) enumBadges.push(modeOfPayment.label);
  if (position.vollstaendigFakturiert)
    enumBadges.push(t("fibu.auftrag.vollstaendigFakturiert"));

  const textChips: string[] = [];
  if (position.personDays != null && position.personDays !== 0)
    textChips.push(
      `${formatNumber(position.personDays, format, 2)} ${t("projectmanagement.personDays.short")}`
    );
  if (periodLabel) textChips.push(periodLabel);

  const invoices = invoiceInfo?.invoices?.filter((inv) => inv.id != null) ?? [];
  const hasSecondary =
    enumBadges.length > 0 ||
    textChips.length > 0 ||
    position.task?.id != null ||
    !!position.bemerkung ||
    invoices.length > 0;

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
      {hasSecondary && (
        <span className="flex min-w-0 flex-wrap items-center gap-x-1.5 gap-y-0.5 text-xs text-muted-foreground">
          {enumBadges.map((label, i) => (
            <Badge key={i} variant="secondary" className="shrink-0 font-normal">
              {label}
            </Badge>
          ))}
          {textChips.map((chip, i) => (
            <span key={i} className="shrink-0">{chip}</span>
          ))}
          {position.bemerkung && (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <span className="max-w-48 truncate italic">
                    {position.bemerkung}
                  </span>
                </TooltipTrigger>
                <TooltipContent className="max-w-xs whitespace-pre-wrap">
                  {position.bemerkung}
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          )}
          {position.task?.id && position.task.displayName && (
            <span className="shrink-0">
              <TaskChip
                taskId={position.task.id}
                displayName={position.task.displayName}
              />
            </span>
          )}
          {invoices.map((invoice) => (
            <TooltipProvider key={invoice.id}>
              <Tooltip>
                <TooltipTrigger asChild>
                  <span className="flex shrink-0 items-center gap-1">
                    <HugeiconsIcon
                      icon={Invoice01Icon}
                      size={12}
                      className="text-muted-foreground"
                    />
                    <Link
                      href={`/outgoingInvoice/edit/${invoice.id}`}
                      className="text-primary underline-offset-2 hover:underline"
                      aria-label={`${t("fibu.rechnung._")} ${invoice.nummer}`}
                    >
                      {invoice.nummer}
                    </Link>
                  </span>
                </TooltipTrigger>
                <TooltipContent>
                  {[
                    invoice.date ? formatDate(invoice.date, format) : null,
                    invoice.netSum != null
                      ? formatCurrency(invoice.netSum, format)
                      : null,
                  ]
                    .filter(Boolean)
                    .join(" · ")}
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          ))}
        </span>
      )}
    </span>
  );
}
