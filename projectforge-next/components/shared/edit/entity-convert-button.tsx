"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDataTransferHorizontalIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";

interface Props {
  /** The already-translated button label — the backend's own (see EditConvert.labelKey). */
  label: string;
  /** Runs the conversion and opens the target's add page. Reporting a failure is the caller's job. */
  onConvert: () => void | Promise<void>;
  disabled?: boolean;
  /** The already-translated hover explanation, if any (see EditConvert.tooltipKey). */
  tooltip?: string;
}

/**
 * Turns the entry being edited into an entry of another entity — a time sheet into a calendar event and
 * back (Wicket's `switchToTeamEventButton` / `switchToTimesheetButton`).
 *
 * A sibling of [EntityCloneButton] and placed beside it: nothing is destroyed and nothing is saved,
 * the prepared target opens as an unsaved new entry, so the way back is to leave the page. Its label
 * varies by direction, so it is passed in rather than fixed here.
 */
export function EntityConvertButton({
  label,
  onConvert,
  disabled,
  tooltip,
}: Props) {
  return (
    <HintTooltip text={tooltip}>
      <Button
        type="button"
        variant="secondary"
        size="sm"
        onClick={() => void onConvert()}
        disabled={disabled}
        className="gap-1.5"
      >
        <HugeiconsIcon icon={ArrowDataTransferHorizontalIcon} size={14} />
        {label}
      </Button>
    </HintTooltip>
  );
}
