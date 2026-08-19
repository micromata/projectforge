"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import {
  currentAnchorOf,
  type Period,
  type PeriodUnit,
} from "@/lib/date-period";
import { cn } from "@/lib/utils";

export interface PeriodStepperProps {
  /**
   * The granularities offered. The first one is what the arrows page in while no period is in effect;
   * empty means no quick access at all and nothing is rendered.
   */
  units: readonly PeriodUnit[];
  /** The period the bounds currently are, or null when they are not a whole one. */
  current: Period | null;
  /**
   * Which period to name while `current` is null — the one a bound already given falls in. Null when
   * neither bound is set, and then the current period is named.
   */
  anchor?: string | null;
  /** Sets both bounds to the period named. */
  onSelect: (unit: PeriodUnit, anchor: string) => void;
  className?: string;
}

/**
 * Quick access to a whole period next to the two ends of a range — Wicket's `QuickSelectPanel`:
 * `◀ [ August 2026 ] ▶`.
 *
 * The arrows page the period by one unit and set *both* ends at once; the middle button names the
 * period in effect and jumps to the current one. While the bounds are not a whole period the middle
 * button is a hint rather than a state, shown muted: it names the period a bound already given falls
 * in (`anchor`), or the current one while the range is empty. The arrows page relative to whatever it
 * names, so what one click does is always what the label says — from an empty filter that is "last
 * month", which is the point of the panel. (Disabling them there would leave a label that does
 * nothing.)
 *
 * Only `onSelect`, never a submit: inside a filter pill the stepper must not close the popover, or
 * paging twice would be impossible. The caller saves as it saves everything else.
 *
 * With one unit this is a plain button. When a second granularity is offered it needs to become
 * switchable — a caret opening `calendar.week` / `calendar.month` / … next to a "current period"
 * entry — which is a branch here and nothing for the callers to know about.
 */
export function PeriodStepper({
  units,
  current,
  anchor: hint,
  onSelect,
  className,
}: PeriodStepperProps) {
  const t = useTranslations();
  const ctx = useFormatContext();
  if (!units.length) return null;

  // The period the buttons act on: the one in effect, else the one a bound given falls in, and only
  // with nothing to go on the current one.
  const unit = current?.unit ?? units[0];
  const anchor = current?.anchor ?? hint ?? currentAnchorOf(unit, ctx);

  const step = (steps: number) =>
    onSelect(unit, unit.shift(anchor, steps, ctx));

  return (
    <div
      role="group"
      aria-label={t("filter.period")}
      className={cn("flex items-center gap-0.5", className)}
    >
      <HintTooltip text={t(unit.tooltipPreviousKey)}>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={t(unit.tooltipPreviousKey)}
          onClick={() => step(-1)}
        >
          <HugeiconsIcon icon={ArrowLeft01Icon} />
        </Button>
      </HintTooltip>
      <HintTooltip text={t(unit.tooltipCurrentKey)}>
        <Button
          type="button"
          // Outlined only while it reports a state; as a hint it must not look like the period in
          // effect.
          variant={current ? "outline" : "ghost"}
          size="sm"
          aria-label={t(unit.tooltipCurrentKey)}
          className={cn(
            "h-6 min-w-28 px-2 text-xs font-normal",
            !current && "text-muted-foreground"
          )}
          onClick={() => onSelect(unit, currentAnchorOf(unit, ctx))}
        >
          {unit.label(anchor, ctx)}
        </Button>
      </HintTooltip>
      <HintTooltip text={t(unit.tooltipNextKey)}>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={t(unit.tooltipNextKey)}
          onClick={() => step(1)}
        >
          <HugeiconsIcon icon={ArrowRight01Icon} />
        </Button>
      </HintTooltip>
    </div>
  );
}
