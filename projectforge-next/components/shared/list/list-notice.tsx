"use client";

import type { ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { AlertCircleIcon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";

interface ListNoticeProps {
  /**
   * `destructive` for a result that is wrong as shown (cut off by the row cap), `warning` for one the
   * user has to act on to get any (a filter too vague to list anything).
   */
  tone?: "destructive" | "warning";
  children: ReactNode;
}

/**
 * A note about the result of the current filter, in the list toolbar's notice slot above the filter
 * pills — where the user changes the filter it is about. Coloured so it is not overlooked the way a
 * line under the table was.
 */
export function ListNotice({
  tone = "destructive",
  children,
}: ListNoticeProps) {
  return (
    <div
      // Not an alert role: it is the state of the result on screen, not an event, so a screen reader
      // gets it in reading order beside the filter rather than as an interruption.
      className={cn(
        "flex items-center gap-2 rounded-md border px-3 py-1.5 text-xs font-medium",
        tone === "destructive"
          ? "border-destructive/30 bg-destructive/10 text-destructive"
          : "border-warning/40 bg-warning/10 text-warning"
      )}
    >
      <HugeiconsIcon icon={AlertCircleIcon} size={14} aria-hidden />
      <span>{children}</span>
    </div>
  );
}
