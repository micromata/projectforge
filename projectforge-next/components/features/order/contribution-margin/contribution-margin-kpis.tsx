"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Card } from "@/components/ui/card";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatDate } from "@/lib/format";
import type { ContributionMarginData } from "@/lib/rs/order";
import { cn } from "@/lib/utils";
import { ContributionMarginPercentage } from "./contribution-margin-percentage";

/**
 * The key figures of the period as tiles above the chart: the current period in the first row, the same
 * period one year earlier in the second and two years earlier in the third, each as revenue, costs, DB1 and
 * the DB % against the target (large, with its traffic light). The current row is marked "preliminary" if the
 * period contains months after the last imported accounting records. Each tile names the last day its row's
 * sums cover at the bottom right; the comparison rows cover their whole 12 months.
 */
export function ContributionMarginKpis({
  data,
}: {
  data: ContributionMarginData;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const { total } = data;
  const preliminary = data.preliminary.some(Boolean)
    ? t("preliminary")
    : undefined;
  const amount = (value: number) => (
    <span className={cn(value < 0 && "text-destructive")}>
      {formatCurrency(value, ctx, 0)}
    </span>
  );
  // The previous years' costs aren't sent; they are their revenue minus their DB1.
  const prevYearCosts = total.prevYearRevenue - total.prevYearProfit;
  const prevPrevYearCosts =
    total.prevPrevYearRevenue - total.prevPrevYearProfit;
  const target = t("target", { arg0: data.targetPercentage });
  const untilText = (date: string | null | undefined) =>
    date ? t("until", { arg0: formatDate(date, ctx) }) : undefined;
  const until = untilText(data.valuesEnd);
  const prevYearUntil = untilText(data.prevYearValuesEnd);
  const prevPrevYearUntil = untilText(data.prevPrevYearValuesEnd);
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      <Kpi until={until} label={t("revenue")} note={preliminary}>
        {amount(total.revenue)}
      </Kpi>
      <Kpi until={until} label={t("costs")} note={preliminary}>
        {formatCurrency(total.costs, ctx, 0)}
      </Kpi>
      <Kpi until={until} label={t("profit")} note={preliminary}>
        {amount(total.profit)}
      </Kpi>
      <Kpi until={until} label={t("percentage")} note={target}>
        <ContributionMarginPercentage
          percentage={total.percentage}
          costs={total.costs}
          limits={data}
          large
        />
      </Kpi>
      <Kpi until={prevYearUntil} label={t("prevYearRevenue")}>
        {amount(total.prevYearRevenue)}
      </Kpi>
      <Kpi until={prevYearUntil} label={t("prevYearCosts")}>
        {formatCurrency(prevYearCosts, ctx, 0)}
      </Kpi>
      <Kpi until={prevYearUntil} label={t("prevYear")}>
        {amount(total.prevYearProfit)}
      </Kpi>
      <Kpi until={prevYearUntil} label={t("prevYearPercentage")} note={target}>
        <ContributionMarginPercentage
          percentage={total.prevYearPercentage}
          costs={Math.max(0, prevYearCosts)}
          limits={data}
          large
        />
      </Kpi>
      <Kpi until={prevPrevYearUntil} label={t("prevPrevYearRevenue")}>
        {amount(total.prevPrevYearRevenue)}
      </Kpi>
      <Kpi until={prevPrevYearUntil} label={t("prevPrevYearCosts")}>
        {formatCurrency(prevPrevYearCosts, ctx, 0)}
      </Kpi>
      <Kpi until={prevPrevYearUntil} label={t("prevPrevYear")}>
        {amount(total.prevPrevYearProfit)}
      </Kpi>
      <Kpi
        until={prevPrevYearUntil}
        label={t("prevPrevYearPercentage")}
        note={target}
      >
        <ContributionMarginPercentage
          percentage={total.prevPrevYearPercentage}
          costs={Math.max(0, prevPrevYearCosts)}
          limits={data}
          large
        />
      </Kpi>
    </div>
  );
}

function Kpi({
  label,
  note,
  until,
  className,
  children,
}: {
  label: string;
  note?: string;
  /** The last day the value covers, shown at the bottom right. */
  until?: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <Card
      size="sm"
      className={cn("gap-1 border px-4 py-3 shadow-sm", className)}
    >
      <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
      </span>
      <span className="text-right text-xl font-semibold tabular-nums">
        {children}
      </span>
      {(note || until) && (
        <span className="mt-auto flex justify-between gap-2 text-xs text-muted-foreground">
          <span>{note}</span>
          <span className="tabular-nums">{until}</span>
        </span>
      )}
    </Card>
  );
}
