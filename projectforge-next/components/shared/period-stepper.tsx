"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { PeriodQuickSelect } from "@/components/shared/period-quick-select";
import { useFormatContext } from "@/hooks/use-format";
import type { Period, PeriodKind } from "@/lib/date-period";
import { currentAnchorOf } from "@/lib/date-period-bounds";
import { cn } from "@/lib/utils";

export interface PeriodStepperProps {
  /**
   * The arts offered. The first one is what the arrows page in while none is in effect; empty means no
   * quick access at all and nothing is rendered.
   */
  kinds: readonly PeriodKind[];
  /** The art and anchor the two bounds currently are, or null when they are no period. */
  current: Period | null;
  /**
   * Which period to name while `current` is null — the one a bound already given falls in. Null when
   * neither bound is set, and then the current period is named.
   */
  anchor?: string | null;
  /** Sets both bounds to the period named — picking an art, and paging where the art decides. */
  onSelect: (kind: PeriodKind, anchor: string) => void;
  /**
   * Where given, the arrows move *the bounds* through this instead of the anchor: a range that is no
   * period at all still pages, by the days it spans, and that is the caller's arithmetic (see
   * `shiftBounds`). Then `canStep` says whether there is anything to move.
   */
  onStep?: (steps: number) => void;
  canStep?: boolean;
  /**
   * Whether the paging arrows are offered at all. Independent of [kinds]: a period entered by hand pages
   * by the days it spans, and one that is never paged still has its arts to pick from.
   */
  paging?: boolean;
  disabled?: boolean;
  className?: string;
}

/**
 * Quick access to a period next to the two ends of a range: `◀ [J→ ⌄] ▶`.
 *
 * The art sits between the arrows ([PeriodQuickSelect]) and the arrows page the whole period, setting
 * *both* ends at once. There is no text naming the period — "August 2026" beside two date boxes that
 * already say 01.08.–31.08. is the same statement twice, and with nothing to name (a term has no name)
 * the row read as a gap between two arrows.
 *
 * The jump back to the current period, which that text used to be — Wicket's `QuickSelectPanel` —
 * is the select: picking the art already in effect sets its current period. Only where the art has one;
 * "die aktuelle Woche" is nothing one does to an agreed period of performance, which is what
 * `tooltipCurrentKey` marks.
 *
 * The arrows page relative to the period a bound already given falls in (`anchor`), or to the current one
 * while the range is empty, so from an empty filter one click is "last month" — the point of the panel.
 * On a form they move the bounds through `onStep` instead, which also pages a range that is no period at
 * all, and there they are disabled rather than hidden while there is nothing to page: an arrow appearing
 * the moment the second date is typed would shift the row under the cursor.
 *
 * Only `onSelect`/`onStep`, never a submit: inside a filter pill the stepper must not close the popover,
 * or paging twice would be impossible. The caller saves as it saves everything else.
 */
export function PeriodStepper({
  kinds,
  current,
  anchor: hint,
  onSelect,
  onStep,
  canStep = true,
  paging = true,
  disabled,
  className,
}: PeriodStepperProps) {
  const t = useTranslations();
  const ctx = useFormatContext();
  if (!kinds.length && !paging) return null;

  // The period the buttons act on: the one in effect, else the one a bound given falls in, and only with
  // nothing to go on the current one. Null where a field offers no art at all — then the arrows move the
  // bounds by the days they span, which is `onStep`'s business alone.
  const kind = current?.kind ?? kinds[0] ?? null;
  const anchor =
    current?.anchor ?? hint ?? (kind ? currentAnchorOf(kind, ctx) : null);

  const step = (steps: number) => {
    if (onStep) return onStep(steps);
    if (kind && anchor) onSelect(kind, kind.shift(anchor, steps, ctx));
  };

  const arrow = (steps: number, icon: typeof ArrowLeft01Icon, key: string) => (
    <HintTooltip text={t(key)}>
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label={t(key)}
        disabled={disabled || !canStep}
        onClick={() => step(steps)}
      >
        <HugeiconsIcon icon={icon} />
      </Button>
    </HintTooltip>
  );

  return (
    <div
      role="group"
      // One name for both sides: to the user this is the period's quick access, whether it pages calendar
      // months in a filter or a term on a form.
      aria-label={t("filter.period")}
      className={cn("flex items-center gap-0.5", className)}
    >
      {paging &&
        // The generic names where no art is in effect: those two arrows page a hand-entered range by the
        // days it spans, which is no month and no term.
        arrow(
          -1,
          ArrowLeft01Icon,
          kind?.tooltipPreviousKey ?? "duration.previous"
        )}
      <PeriodQuickSelect
        kinds={kinds}
        value={current?.kind ?? null}
        onSelect={(picked) =>
          onSelect(
            picked,
            // The art in effect picked again: the current period of it, which is the jump this replaced
            // the naming button with. Another art is read from the anchor already in play instead, so
            // "3 Monate" beside a begin of 15.03. means that term and does not move the range.
            picked === current?.kind && picked.tooltipCurrentKey
              ? currentAnchorOf(picked, ctx)
              : (current?.anchor ?? hint ?? currentAnchorOf(picked, ctx))
          )
        }
        disabled={disabled}
      />
      {paging &&
        arrow(1, ArrowRight01Icon, kind?.tooltipNextKey ?? "duration.next")}
    </div>
  );
}
