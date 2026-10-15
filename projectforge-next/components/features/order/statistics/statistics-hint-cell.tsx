"use client";

import type { ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { InformationCircleIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";

/**
 * A table cell content with an explanation behind it, e.g. the lost budget warnings: the whole cell is
 * the trigger and an (i) after the content shows that there is something to read. A text comes from the
 * backend and is shown verbatim; a node (e.g. a table of warnings) as it is.
 */
export function StatisticsHintCell({
  hint,
  className,
  children,
}: {
  hint: ReactNode;
  className?: string;
  children: ReactNode;
}) {
  return (
    <HintTooltip
      text={typeof hint === "string" ? hint : undefined}
      content={typeof hint === "string" ? undefined : hint}
      plain
      wide
      openOnTap
    >
      <div className={cn("flex w-full items-center gap-1", className)}>
        <span className="min-w-0 truncate">{children}</span>
        <HugeiconsIcon
          icon={InformationCircleIcon}
          size={13}
          strokeWidth={2}
          className="shrink-0"
        />
      </div>
    </HintTooltip>
  );
}
