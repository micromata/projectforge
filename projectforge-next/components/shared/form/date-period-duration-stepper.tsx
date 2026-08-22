"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { Duration } from "@/lib/date-duration";
import { cn } from "@/lib/utils";
import { DatePeriodDurationSelect } from "./date-period-duration-select";

/**
 * Quick access to a period beside its two ends: `◀ [3M] ▶`.
 *
 * The length in the middle fills the end in from the begin ([DatePeriodDurationSelect]); the arrows move
 * the whole period on by its own length — by that term, or by the days it spans where it is none. Both
 * halves are optional and independent: a field may offer only the lengths, only the arrows, or neither,
 * and then nothing is rendered.
 *
 * The arrows are disabled rather than hidden while there is nothing to page: an arrow appearing the moment
 * the second date is typed would shift the row under the cursor, and disabled it says why it does nothing.
 * Unlike [PeriodStepper] there is no button between them naming the period — that place belongs to the
 * length here, and "the current week" is no sensible thing to do to a Leistungszeitraum.
 */
export function DatePeriodDurationStepper({
  durations,
  value,
  onSelect,
  paging,
  canStep,
  onStep,
  disabled,
  className,
}: {
  /** The lengths offered; empty means arrows only. */
  durations: readonly Duration[];
  /** The duration in effect, or null while the two ends are none. */
  value: Duration | null;
  onSelect: (duration: Duration) => void;
  /** Whether the paging arrows are offered at all. */
  paging: boolean;
  /** Whether the period has a length to be paged by. */
  canStep: boolean;
  onStep: (steps: number) => void;
  disabled?: boolean;
  className?: string;
}) {
  const t = useTranslations();
  if (!durations.length && !paging) return null;

  const arrow = (steps: number, icon: typeof ArrowLeft01Icon, key: string) => (
    <HintTooltip text={t(key)}>
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label={t(key)}
        disabled={disabled || !canStep}
        onClick={() => onStep(steps)}
      >
        <HugeiconsIcon icon={icon} />
      </Button>
    </HintTooltip>
  );

  return (
    <div
      role="group"
      // The same name [PeriodStepper] gives its group: to the user this is the period's quick access,
      // whether it pages calendar months in a filter or a term on a form.
      aria-label={t("filter.period")}
      className={cn("flex items-center gap-0.5", className)}
    >
      {paging && arrow(-1, ArrowLeft01Icon, "duration.previous")}
      <DatePeriodDurationSelect
        durations={durations}
        value={value}
        onSelect={onSelect}
        disabled={disabled}
      />
      {paging && arrow(1, ArrowRight01Icon, "duration.next")}
    </div>
  );
}
