"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatPercentage } from "@/lib/format";
import type { ContributionMarginBubble } from "@/lib/rs/order";

/**
 * A bubble as plotted: `x` its revenue (0 if negative), `y` its DB % (0 without revenue), `z` its DB1 as size (0
 * for a loss), `text` its label with the DB %, null if anonymous.
 */
export interface BubbleRow extends ContributionMarginBubble {
  x: number;
  y: number;
  z: number;
  text: string | null;
}

/**
 * The hover card of a bubble: its name (or "other projects" for an anonymous one), revenue, costs, DB1 and DB %,
 * and the number and names of the projects of an own group bubble. Recharts injects `active` and `payload`.
 */
export function BubbleTooltipContent({
  active,
  payload,
}: {
  active?: boolean;
  payload?: ReadonlyArray<{ payload?: BubbleRow }>;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const bubble = active ? payload?.[0]?.payload : undefined;
  if (!bubble) return null;
  const rows: [string, string][] = [
    [t("revenue"), formatCurrency(bubble.revenue, ctx, 0)],
    [t("costs"), formatCurrency(bubble.costs, ctx, 0)],
    [t("profit"), formatCurrency(bubble.profit, ctx, 0)],
    [
      t("percentage"),
      bubble.percentage == null
        ? t("bubbles.noRevenue")
        : formatPercentage(bubble.percentage, ctx),
    ],
  ];
  if (bubble.projectCount != null && bubble.projectCount > 1) {
    rows.push([t("bubbles.projectCount"), String(bubble.projectCount)]);
  }
  // A single project named like the bubble itself is no news.
  const projects = (bubble.projects ?? []).filter(
    (name, _, all) => all.length > 1 || name !== bubble.label
  );
  return (
    <div className="grid min-w-48 gap-1.5 rounded-lg border bg-background px-2.5 py-1.5 text-xs shadow-xl">
      <div className="font-medium">
        {bubble.label ?? t("bubbles.anonymous")}
      </div>
      {rows.map(([label, value]) => (
        <span key={label} className="flex w-full justify-between gap-2">
          <span className="text-muted-foreground">{label}</span>
          <span className="font-mono font-medium tabular-nums">{value}</span>
        </span>
      ))}
      {projects.length > 0 && (
        <div className="max-w-72 border-t pt-1.5 text-muted-foreground">
          {projects.join(", ")}
        </div>
      )}
    </div>
  );
}
