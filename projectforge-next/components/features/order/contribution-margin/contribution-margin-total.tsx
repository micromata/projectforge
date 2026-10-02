"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { ContributionMarginData } from "@/lib/rs/order";
import { ContributionMarginPercentage } from "./contribution-margin-percentage";

/** The sums of all projects of the table, below it (see ContributionMarginProjectTable). */
export function ContributionMarginTotal({
  data,
}: {
  data: ContributionMarginData;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const { total } = data;
  const amounts: [string, number][] = [
    [t("revenue"), total.revenue],
    [t("costs"), total.costs],
    [t("profit"), total.profit],
    [t("prevYear"), total.prevYearProfit],
    [t("prevPrevYear"), total.prevPrevYearProfit],
  ];
  const percentages: [string, number | null, number][] = [
    [t("percentage"), total.percentage, total.costs],
    [
      t("prevYearPercentage"),
      total.prevYearPercentage,
      Math.max(0, -total.prevYearProfit),
    ],
  ];
  return (
    <div className="flex flex-wrap gap-x-6 gap-y-1 border-t px-3 py-2 text-sm">
      <span className="font-semibold">{t("total")}</span>
      <dl className="contents">
        {amounts.map(([label, value]) => (
          <div key={label} className="flex gap-1.5">
            <dt className="text-muted-foreground">{label}</dt>
            <dd
              className={cn(
                "font-semibold tabular-nums",
                value < 0 && "text-destructive"
              )}
            >
              {formatCurrency(value, ctx)}
            </dd>
          </div>
        ))}
        {percentages.map(([label, value, costs]) => (
          <div key={label} className="flex gap-1.5">
            <dt className="text-muted-foreground">{label}</dt>
            <dd className="font-semibold">
              <ContributionMarginPercentage
                percentage={value}
                costs={costs}
                limits={data}
              />
            </dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
