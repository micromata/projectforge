"use client";

import { useTranslations } from "next-intl";
import {
  CONTRIBUTION_MARGIN_TONE_BG,
  type ContributionMarginLimits,
} from "@/lib/contribution-margin";
import { cn } from "@/lib/utils";

/** What the traffic light of the contribution margin makes of the edited limits. */
export function ContributionMarginBands({
  limits,
}: {
  limits: ContributionMarginLimits;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin.config");
  return (
    <p className="flex items-center gap-2 text-xs text-muted-foreground">
      <span className="inline-flex gap-1" aria-hidden>
        {(["red", "yellow", "green"] as const).map((tone) => (
          <span
            key={tone}
            className={cn(
              "inline-block h-2.5 w-2.5 rounded-full",
              CONTRIBUTION_MARGIN_TONE_BG[tone]
            )}
          />
        ))}
      </span>
      {t("bands", {
        arg0: limits.redThreshold,
        arg1: limits.targetPercentage,
      })}
    </p>
  );
}
