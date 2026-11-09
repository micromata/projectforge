"use client";

import { useTranslations } from "next-intl";
import { KpiTile } from "@/components/shared/kpi-tile";
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
      <KpiTile noteRight={until} label={t("revenue")} note={preliminary}>
        {amount(total.revenue)}
      </KpiTile>
      <KpiTile noteRight={until} label={t("costs")} note={preliminary}>
        {formatCurrency(total.costs, ctx, 0)}
      </KpiTile>
      <KpiTile noteRight={until} label={t("profit")} note={preliminary}>
        {amount(total.profit)}
      </KpiTile>
      <KpiTile noteRight={until} label={t("percentage")} note={target}>
        <ContributionMarginPercentage
          percentage={total.percentage}
          costs={total.costs}
          limits={data}
          large
        />
      </KpiTile>
      <KpiTile noteRight={prevYearUntil} label={t("prevYearRevenue")}>
        {amount(total.prevYearRevenue)}
      </KpiTile>
      <KpiTile noteRight={prevYearUntil} label={t("prevYearCosts")}>
        {formatCurrency(prevYearCosts, ctx, 0)}
      </KpiTile>
      <KpiTile noteRight={prevYearUntil} label={t("prevYear")}>
        {amount(total.prevYearProfit)}
      </KpiTile>
      <KpiTile
        noteRight={prevYearUntil}
        label={t("prevYearPercentage")}
        note={target}
      >
        <ContributionMarginPercentage
          percentage={total.prevYearPercentage}
          costs={Math.max(0, prevYearCosts)}
          limits={data}
          large
        />
      </KpiTile>
      <KpiTile noteRight={prevPrevYearUntil} label={t("prevPrevYearRevenue")}>
        {amount(total.prevPrevYearRevenue)}
      </KpiTile>
      <KpiTile noteRight={prevPrevYearUntil} label={t("prevPrevYearCosts")}>
        {formatCurrency(prevPrevYearCosts, ctx, 0)}
      </KpiTile>
      <KpiTile noteRight={prevPrevYearUntil} label={t("prevPrevYear")}>
        {amount(total.prevPrevYearProfit)}
      </KpiTile>
      <KpiTile
        noteRight={prevPrevYearUntil}
        label={t("prevPrevYearPercentage")}
        note={target}
      >
        <ContributionMarginPercentage
          percentage={total.prevPrevYearPercentage}
          costs={Math.max(0, prevPrevYearCosts)}
          limits={data}
          large
        />
      </KpiTile>
    </div>
  );
}
