import { niceScale, type AxisScale } from "@/lib/chart-scale";
import { formatNumber, type FormatContext } from "@/lib/format";
import type { ContributionMarginBubble } from "@/lib/rs/order";
import type { BubbleRow } from "./contribution-margin-bubble-tooltip";

/** Percentage points above the top tick if a bubble sits on it. */
const Y_HEADROOM = 8;

/**
 * The bubbles as plotted, see [BubbleRow]: the label is "name; DB %" or, with [showTitles], only the short
 * title (the group name, the identifier of a project by itself).
 */
export function bubbleRows(
  bubbles: ContributionMarginBubble[],
  ctx: FormatContext,
  showTitles = false
): BubbleRow[] {
  return bubbles.map((bubble) => ({
    ...bubble,
    x: Math.max(0, bubble.revenue),
    y: bubble.percentage ?? 0,
    text:
      bubble.label == null
        ? null
        : showTitles
          ? (bubble.title ?? bubble.label)
          : `${bubble.label}; ${formatNumber(bubble.percentage ?? 0, ctx, 0)}`,
  }));
}

/**
 * The DB % axis up to the target and the highest bubble, with headroom above the highest one (often 100 %), so
 * it isn't cut in half by the top edge; no tick there.
 */
export function bubbleYScale(
  rows: BubbleRow[],
  targetPercentage: number
): AxisScale {
  const scale = niceScale(
    rows.map((row) => row.y).concat(targetPercentage + 10)
  );
  const [min, max] = scale.domain;
  const top = Math.max(...rows.map((row) => row.y));
  return top < max
    ? scale
    : { domain: [min, max + Y_HEADROOM], ticks: scale.ticks };
}
