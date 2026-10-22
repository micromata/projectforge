"use client";

import { useTranslations } from "next-intl";
import type { ContributionMarginBubbles } from "@/lib/rs/order";
import { cn } from "@/lib/utils";
import {
  BUBBLE_DEFAULT_COLOR,
  BUBBLE_OPACITY,
  BUBBLE_OTHERS_DEFAULT_COLOR,
} from "./contribution-margin-bubble-style";

/**
 * The legend of the bubble chart: the colours of the business units occurring in it, the default colour (if a
 * bubble has none) and, with anonymous bubbles, that they are the pale ones.
 */
export function ContributionMarginBubbleLegend({
  data,
}: {
  data: ContributionMarginBubbles;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin.bubbles");
  const units = data.businessUnits ?? [];
  const withoutColor = data.bubbles.some((bubble) => !bubble.color);
  const anonymous = data.bubbles.some((bubble) => !bubble.own);
  return (
    <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-1 pt-1 text-xs">
      {units.map((unit) => (
        <Item key={unit.name} color={unit.color} label={unit.name} />
      ))}
      {withoutColor && units.length > 0 && (
        <Item color={BUBBLE_DEFAULT_COLOR} label={t("legend.default")} />
      )}
      {anonymous && (
        <Item
          color={BUBBLE_OTHERS_DEFAULT_COLOR}
          label={t("legend.anonymous")}
          pale
        />
      )}
    </div>
  );
}

function Item({
  color,
  label,
  pale = false,
}: {
  color: string;
  label: string;
  pale?: boolean;
}) {
  return (
    <div className="flex items-center gap-1.5">
      <div
        className={cn("size-2.5 shrink-0 rounded-full", !pale && "border")}
        style={{
          backgroundColor: color,
          borderColor: color,
          opacity: pale ? BUBBLE_OPACITY.others : BUBBLE_OPACITY.own,
        }}
      />
      <span className="text-muted-foreground">{label}</span>
    </div>
  );
}
