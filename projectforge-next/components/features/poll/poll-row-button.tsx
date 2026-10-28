"use client";

import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/** An icon button of a question or option row (move, remove), named by its tooltip and aria-label. */
export function PollRowButton({
  icon,
  label,
  disabled,
  onClick,
}: {
  icon: IconSvgElement;
  label: string;
  disabled?: boolean;
  onClick: () => void;
}) {
  return (
    <HintTooltip text={label}>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        aria-label={label}
        disabled={disabled}
        onClick={onClick}
      >
        <HugeiconsIcon icon={icon} aria-hidden />
      </Button>
    </HintTooltip>
  );
}
