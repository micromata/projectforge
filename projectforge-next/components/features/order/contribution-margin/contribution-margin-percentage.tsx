"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import {
  CONTRIBUTION_MARGIN_TONE_BG,
  CONTRIBUTION_MARGIN_TONE_TEXT,
  contributionMarginTone,
  type ContributionMarginLimits,
} from "@/lib/contribution-margin";
import { formatPercentage } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * A contribution margin in % with its traffic light (see contributionMarginTone). Without revenue there is
 * no percentage: costs alone are shown as a red "–" with a hint, nothing at all is shown as nothing.
 * `large` is the key figure variant of the tiles: big, and the number coloured like its light.
 */
export function ContributionMarginPercentage({
  percentage,
  costs,
  limits,
  large = false,
}: {
  percentage: number | null;
  /** The costs of the same period, telling a loss without revenue from an empty row. */
  costs?: number;
  limits: ContributionMarginLimits;
  large?: boolean;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  if (percentage == null) {
    if (!costs) {
      return null;
    }
    return (
      <HintTooltip text={t("noRevenue")} openOnTap>
        <span className={valueClass("red", large)}>
          <Dot tone="red" large={large} />–
        </span>
      </HintTooltip>
    );
  }
  const tone = contributionMarginTone(percentage, limits);
  return (
    <span className={valueClass(tone, large)}>
      <Dot tone={tone} large={large} />
      {formatPercentage(percentage, ctx)}
    </span>
  );
}

type Tone = keyof typeof CONTRIBUTION_MARGIN_TONE_BG;

function valueClass(tone: Tone, large: boolean) {
  return cn(
    "inline-flex items-center gap-1.5 tabular-nums",
    large && cn("gap-2 text-3xl", CONTRIBUTION_MARGIN_TONE_TEXT[tone])
  );
}

function Dot({ tone, large }: { tone: Tone; large: boolean }) {
  return (
    <span
      className={cn(
        "inline-block shrink-0 rounded-full",
        large ? "h-3.5 w-3.5" : "h-2.5 w-2.5",
        CONTRIBUTION_MARGIN_TONE_BG[tone]
      )}
      aria-hidden
    />
  );
}
