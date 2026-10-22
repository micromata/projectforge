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

/** The area range of the bubbles in px²: the highest costs get the biggest one, the others scale with it. */
const BUBBLE_RANGE: [number, number] = [40, 2500];

/** Above this many own bubbles their labels would overlap: the name is in the tooltip only (unless asked for). */
const MAX_LABELLED_BUBBLES = 15;

/**
 * The contribution margin bubble chart, as controlling built it in Excel: revenue on the x axis, DB % on the y
 * axis over the red/yellow bands and the target line, the costs as the size of the bubble, coloured by business
 * unit. The own bubbles carry "name; DB %" (if not too many) or, on request, their short title (always); the
 * anonymous ones of the other projects are pale and unlabelled. A bubble without (or with negative) revenue sits
 * on the 0 line, its tooltip has the real value.
 */
export function ContributionMarginBubbleChart({
  data,
  showTitles = false,
  className = "h-[30rem] w-full",
}: {
  data: ContributionMarginBubbles;
  /** Whether the labels are the short titles (instead of name and DB %), even for many bubbles. */
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
  const rows = useMemo(
    () => bubbleRows(data.bubbles, ctx, showTitles),
    [data, ctx, showTitles]
  );
  const own = rows.filter((row) => row.own);
  const others = rows.filter((row) => !row.own);
  const labelled = showTitles || own.length <= MAX_LABELLED_BUBBLES;
  const xScale = useMemo(() => niceScale(rows.map((row) => row.x)), [rows]);
  const yScale = useMemo(
    () => bubbleYScale(rows, data.targetPercentage),
    [rows, data.targetPercentage]
  );
  const maxCosts = Math.max(1, ...rows.map((row) => row.costs));
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
            dataKey="costs"
            domain={[0, maxCosts]}
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
            {labelled && (
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
