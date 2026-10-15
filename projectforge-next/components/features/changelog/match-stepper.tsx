"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowUp01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/** The match counter of the search ("3 / 17") and the buttons stepping to the previous and next match. */
export function MatchStepper({
  count,
  current,
  onPrevious,
  onNext,
}: {
  count: number;
  /** The current match, 0-based. */
  current: number;
  onPrevious: () => void;
  onNext: () => void;
}) {
  const t = useTranslations();
  return (
    <div className="flex items-center gap-0.5">
      <span
        className="min-w-12 text-center text-xs tabular-nums text-muted-foreground"
        aria-live="polite"
      >
        {count > 0 ? current + 1 : 0} / {count}
      </span>
      <HintTooltip text={t("changelog.previousMatch")}>
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={onPrevious}
          disabled={count === 0}
          aria-label={t("changelog.previousMatch")}
        >
          <HugeiconsIcon icon={ArrowUp01Icon} size={14} />
        </Button>
      </HintTooltip>
      <HintTooltip text={t("changelog.nextMatch")}>
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={onNext}
          disabled={count === 0}
          aria-label={t("changelog.nextMatch")}
        >
          <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
        </Button>
      </HintTooltip>
    </div>
  );
}
