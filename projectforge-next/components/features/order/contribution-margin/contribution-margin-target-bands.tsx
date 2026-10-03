"use client";

import { ReferenceArea, ReferenceLine } from "recharts";
import { CHART_ROLE } from "@/lib/charts/roles";
import type { AxisScale } from "@/lib/chart-scale";
import type { ContributionMarginLimits } from "@/lib/contribution-margin";
import { PERCENT_AXIS } from "./contribution-margin-chart-data";

/** Faint enough that the bars and lines in front of the bands stay legible. */
const BAND_OPACITY = 0.07;

/**
 * The traffic light of the DB % on the percent axis: a faint red band below the red threshold, a yellow
 * one up to the target, and the target as a dashed line with its label. Recharts 3 collects these as
 * children of the chart, so they may sit in a component of their own.
 */
export function ContributionMarginTargetBands({
  limits,
  scale,
  targetLabel,
}: {
  limits: ContributionMarginLimits;
  scale: AxisScale;
  targetLabel: string;
}) {
  const [min] = scale.domain;
  return (
    <>
      <ReferenceArea
        yAxisId={PERCENT_AXIS}
        y1={min}
        y2={limits.redThreshold}
        fill="var(--destructive)"
        fillOpacity={BAND_OPACITY}
        ifOverflow="hidden"
      />
      <ReferenceArea
        yAxisId={PERCENT_AXIS}
        y1={limits.redThreshold}
        y2={limits.targetPercentage}
        fill="var(--warning)"
        fillOpacity={BAND_OPACITY}
        ifOverflow="hidden"
      />
      <ReferenceLine
        yAxisId={PERCENT_AXIS}
        y={limits.targetPercentage}
        stroke={CHART_ROLE.positive}
        strokeDasharray="6 4"
        strokeWidth={1.5}
        label={{
          value: targetLabel,
          position: "insideTopRight",
          fill: "var(--muted-foreground)",
          fontSize: 12,
        }}
      />
    </>
  );
}
