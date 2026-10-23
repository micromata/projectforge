"use client";

import { useMemo } from "react";
import {
  CartesianGrid,
  Cell,
  LabelList,
  Scatter,
  ScatterChart,
  XAxis,
  YAxis,
  ZAxis,
} from "recharts";
import { useTranslations } from "next-intl";
import {
  ChartContainer,
  ChartTooltip,
  type ChartConfig,
} from "@/components/ui/chart";
import { useFormatContext } from "@/hooks/use-format";
import { niceScale } from "@/lib/chart-scale";
import { formatCurrency, formatNumber } from "@/lib/format";
import type { ContributionMarginBubbles } from "@/lib/rs/order";
import { BubbleTooltipContent } from "./contribution-margin-bubble-tooltip";
import { ContributionMarginBubbleLegend } from "./contribution-margin-bubble-legend";
import { bubbleRows, bubbleYScale } from "./contribution-margin-bubble-rows";
import {
  BUBBLE_DEFAULT_COLOR,
  BUBBLE_OPACITY,
  BUBBLE_OTHERS_DEFAULT_COLOR,
} from "./contribution-margin-bubble-style";
import { PERCENT_AXIS } from "./contribution-margin-chart-data";
import { ContributionMarginTargetBands } from "./contribution-margin-target-bands";

/** The area range of the bubbles in px²: the highest DB1 gets the biggest one (a loss the smallest), the others scale with it. */
const BUBBLE_RANGE: [number, number] = [40, 2500];

/**
 * The contribution margin bubble chart, as controlling built it in Excel: revenue on the x axis, DB % on the y
 * axis over the red/yellow bands and the target line, the DB1 as the size of the bubble, coloured by business
 * unit. On request, the own bubbles carry "short title; DB %" (else the name is in the tooltip only); the
 * anonymous ones of the other projects are pale and unlabelled. A bubble without (or with negative) revenue sits
 * on the 0 line, its tooltip has the real value.
 */
export function ContributionMarginBubbleChart({
  data,
  showTitles = false,
  className = "h-[30rem] w-full",
}: {
  data: ContributionMarginBubbles;
  /** Whether the own bubbles are labelled with their short title and DB %. */
  showTitles?: boolean;
  className?: string;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const config = useMemo<ChartConfig>(
    () => ({
      own: { label: t("bubbles.own"), color: BUBBLE_DEFAULT_COLOR },
      others: {
        label: t("bubbles.anonymous"),
        color: BUBBLE_OTHERS_DEFAULT_COLOR,
      },
    }),
    [t]
  );
  const rows = useMemo(() => bubbleRows(data.bubbles, ctx), [data, ctx]);
  const own = rows.filter((row) => row.own);
  const others = rows.filter((row) => !row.own);
  const xScale = useMemo(() => niceScale(rows.map((row) => row.x)), [rows]);
  const yScale = useMemo(
    () => bubbleYScale(rows, data.targetPercentage),
    [rows, data.targetPercentage]
  );
  const maxSize = Math.max(1, ...rows.map((row) => row.z));
  return (
    <div>
      <ChartContainer
        config={config}
        className={className}
        role="img"
        aria-label={t("bubbles._")}
      >
        <ScatterChart margin={{ left: 4, right: 24, top: 16, bottom: 4 }}>
          <CartesianGrid
            stroke="var(--muted-foreground)"
            strokeOpacity={0.35}
          />
          <XAxis
            type="number"
            dataKey="x"
            name={t("revenue")}
            domain={xScale.domain}
            ticks={xScale.ticks}
            tickLine={false}
            axisLine={false}
            tickMargin={8}
            tickFormatter={(value) => formatCurrency(value, ctx, 0)}
          />
          <YAxis
            yAxisId={PERCENT_AXIS}
            type="number"
            dataKey="y"
            name={t("percentage")}
            width={48}
            domain={yScale.domain}
            ticks={yScale.ticks}
            tickLine={false}
            axisLine={false}
            tickFormatter={(value) => formatNumber(value, ctx, 0)}
          />
          <ZAxis
            type="number"
            dataKey="z"
            domain={[0, maxSize]}
            range={BUBBLE_RANGE}
          />
          <ContributionMarginTargetBands
            limits={data}
            scale={yScale}
            targetLabel={t("target", { arg0: data.targetPercentage })}
          />
          <ChartTooltip
            cursor={false}
            content={<BubbleTooltipContent />}
            isAnimationActive={false}
          />
          {others.length > 0 && (
            <Scatter
              name="others"
              yAxisId={PERCENT_AXIS}
              data={others}
              fillOpacity={BUBBLE_OPACITY.others}
              isAnimationActive={false}
            >
              {others.map((row, index) => (
                <Cell
                  key={index}
                  fill={row.color ?? "var(--color-others)"}
                  stroke="none"
                />
              ))}
            </Scatter>
          )}
          <Scatter
            name="own"
            yAxisId={PERCENT_AXIS}
            data={own}
            fillOpacity={BUBBLE_OPACITY.own}
            isAnimationActive={false}
          >
            {own.map((row, index) => {
              const color = row.color ?? "var(--color-own)";
              return <Cell key={index} fill={color} stroke={color} />;
            })}
            {showTitles && (
              <LabelList
                dataKey="text"
                position="right"
                className="fill-foreground"
                fontSize={12}
              />
            )}
          </Scatter>
        </ScatterChart>
      </ChartContainer>
      <ContributionMarginBubbleLegend data={data} />
    </div>
  );
}
