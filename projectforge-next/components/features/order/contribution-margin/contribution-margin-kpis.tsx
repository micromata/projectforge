"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Card } from "@/components/ui/card";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import type { ContributionMarginData } from "@/lib/rs/order";
import { cn } from "@/lib/utils";
import { ContributionMarginPercentage } from "./contribution-margin-percentage";

/**
 * The key figures of the period as tiles above the chart: the current period in the first row, the same
 * period one year earlier in the second and two years earlier in the third, each as revenue, costs, DB1 and
 * the DB % against the target (large, with its traffic light). The current row is marked "preliminary" if the
 * period contains months after the last imported accounting records.
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
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      <Kpi label={t("revenue")} note={preliminary}>
        {amount(total.revenue)}
      </Kpi>
      <Kpi label={t("costs")} note={preliminary}>
        {formatCurrency(total.costs, ctx, 0)}
      </Kpi>
      <Kpi label={t("profit")} note={preliminary}>
        {amount(total.profit)}
      </Kpi>
      <Kpi label={t("percentage")} note={target}>
        <ContributionMarginPercentage
          percentage={total.percentage}
          costs={total.costs}
          limits={data}
          large
        />
      </Kpi>
      <Kpi label={t("prevYearRevenue")}>{amount(total.prevYearRevenue)}</Kpi>
      <Kpi label={t("prevYearCosts")}>
        {formatCurrency(prevYearCosts, ctx, 0)}
      </Kpi>
      <Kpi label={t("prevYear")}>{amount(total.prevYearProfit)}</Kpi>
      <Kpi label={t("prevYearPercentage")} note={target}>
        <ContributionMarginPercentage
          percentage={total.prevYearPercentage}
          costs={Math.max(0, prevYearCosts)}
          limits={data}
          large
        />
      </Kpi>
      <Kpi label={t("prevPrevYearRevenue")}>
        {amount(total.prevPrevYearRevenue)}
      </Kpi>
      <Kpi label={t("prevPrevYearCosts")}>
        {formatCurrency(prevPrevYearCosts, ctx, 0)}
      </Kpi>
      <Kpi label={t("prevPrevYear")}>{amount(total.prevPrevYearProfit)}</Kpi>
      <Kpi label={t("prevPrevYearPercentage")} note={target}>
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
  className,
  children,
}: {
  label: string;
  note?: string;
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
      {note && <span className="text-xs text-muted-foreground">{note}</span>}
    </Card>
  );
}
