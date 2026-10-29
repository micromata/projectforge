"use client";

import { InformationCircleIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/**
 * An info icon whose tooltip says who may do what on the page — next to the menu of an area, where the
 * eye already is. Nothing without a [text].
 */
export function RightsHint({
  text,
  label,
}: {
  text?: string;
  /** The accessible name of the icon, e.g. "Rights on this page". */
  label: string;
}) {
  if (!text) return null;
  return (
    <HintTooltip text={text} side="bottom" openOnTap>
      <button
        type="button"
        aria-label={label}
        className="text-muted-foreground hover:text-foreground"
      >
        <HugeiconsIcon icon={InformationCircleIcon} className="size-4" />
      </button>
    </HintTooltip>
  );
}
