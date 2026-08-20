"use client";

import { GuardedLink } from "@/components/shared/guarded-link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Invoice01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { CollapsibleSummary } from "@/components/shared/collapsible-summary";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import {
  formatCurrency,
  formatDate,
  formatNumber,
  formatPercentageDecimal,
} from "@/lib/format";
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
 * What a collapsed position says: primary line with number/title/status/sum, and — while the row is
 * folded — every other filled field below it, so nothing is hidden ([CollapsibleSummary]).
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

  const textChips: string[] = [];
  if (position.personDays != null && position.personDays !== 0)
    textChips.push(
      `${formatNumber(position.personDays, format, 2)} ${t("projectmanagement.personDays.short")}`
    );
  if (periodLabel) textChips.push(periodLabel);

  const invoices = invoiceInfo?.invoices?.filter((inv) => inv.id != null) ?? [];

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
          {/*
           * The probability the forecast applies to this position, next to the sum it weighs — the same number
           * the sums line shows for the order, but here per position, which is where it is actually defined
           * (see `ForecastUtils.getProbabilityOfAccurence`). Absent until the first recalculation answers, and
           * for a position that has no number yet.
           */}
          {sums?.probabilityOfOccurrence != null && (
            <HintTooltip
              text={t("fibu.auftrag.probabilityOfOccurrence.effective.info")}
            >
              <span className="shrink-0 cursor-help text-xs font-semibold text-primary tabular-nums">
                {formatPercentageDecimal(sums.probabilityOfOccurrence, format)}
              </span>
            </HintTooltip>
          )}
          <span className="shrink-0 tabular-nums">
            {formatCurrency(netSum, format)}
          </span>
        </>
      }
      details={[
        // Ahead of the enum badges and in teal, not as one grey badge among them: whether a position
        // is settled is what the folded row is read for, the same state the payment schedule shows.
        position.vollstaendigFakturiert && (
          <span className="font-medium text-brand-teal">
            {t("fibu.auftrag.vollstaendigFakturiert")}
          </span>
        ),
        ...enumBadges.map((label) => (
          <Badge key={label} variant="secondary" className="font-normal">
            {label}
          </Badge>
        )),
        ...textChips,
        position.bemerkung && (
          <HintTooltip plain text={position.bemerkung}>
            <span className="block max-w-48 truncate italic">
              {position.bemerkung}
            </span>
          </HintTooltip>
        ),
        position.task?.id && position.task.displayName && (
          <TaskChip
            taskId={position.task.id}
            displayName={position.task.displayName}
          />
        ),
        ...invoices.map((invoice) => (
          <HintTooltip
            key={invoice.id}
            plain
            text={[
              invoice.date ? formatDate(invoice.date, format) : null,
              invoice.netSum != null
                ? formatCurrency(invoice.netSum, format)
                : null,
            ]
              .filter(Boolean)
              .join(" · ")}
          >
            <span className="flex items-center gap-1">
              <HugeiconsIcon
                icon={Invoice01Icon}
                size={12}
                className="text-muted-foreground"
              />
              {/* `/invoice/{id}`, not `/outgoingInvoice/edit/{id}`: the generic route answers
                  notFound() for a hand-built entity. Guarded, as in position-invoices.tsx: this
                  link sits inside the order's own form and leads out of it. */}
              <GuardedLink
                href={`/invoice/${invoice.id}`}
                className="text-primary underline-offset-2 hover:underline"
                aria-label={`${t("fibu.rechnung._")} ${invoice.nummer}`}
              >
                {invoice.nummer}
              </GuardedLink>
            </span>
          </HintTooltip>
        )),
      ]}
    />
  );
}
