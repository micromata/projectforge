"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { CollapsedOnly } from "./collapsed-only";

export interface CollapsibleSummaryProps {
  /** The line that identifies the row — always visible, open or closed. */
  primary: ReactNode;
  /**
   * Everything else the row holds, one chip per entry, wrapped into a single line below the primary one
   * and shown only while the row is closed. Falsy entries are dropped, so a caller can list a chip
   * conditionally without counting first.
   */
  details?: ReactNode[];
  /** A block that needs more than one line (a nested list, say); again only while closed. */
  extra?: ReactNode;
  className?: string;
  /**
   * Layout of the primary line, for a row whose identifying line does not fit one — a history entry
   * wraps it rather than truncating, because none of its parts is the one that may be cut.
   */
  primaryClassName?: string;
}

/**
 * The header of a collapsible row: what identifies it, and — while it is closed — everything its content
 * would say.
 *
 * That double duty is why this exists: a folded row has to be complete, because folded is the normal
 * state of a stored order or invoice ([RepeatableRow] opens only what was just added), and anything left
 * out of the header is a field nobody reads back. Unfolded, the same values sit in the fields right
 * below, so the summary steps aside ([CollapsedOnly]).
 *
 * Spans rather than divs throughout: the header is rendered inside a `CollapsibleTrigger`, i.e. inside a
 * button, where block elements are not allowed.
 */
export function CollapsibleSummary({
  primary,
  details,
  extra,
  className,
  primaryClassName,
}: CollapsibleSummaryProps) {
  const chips = (details ?? []).filter(Boolean);

  return (
    <span className={cn("flex min-w-0 flex-1 flex-col gap-0.5", className)}>
      <span
        className={cn(
          "flex min-w-0 flex-1 items-center gap-2 text-sm",
          primaryClassName
        )}
      >
        {primary}
      </span>
      {chips.length > 0 && (
        <CollapsedOnly className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-muted-foreground">
          {chips.map((chip, index) => (
            // Never squeezed, but never wider than the row either: a chip that is a whole sentence — a
            // remark, a list of changed field names — truncates (where the caller says so) instead of
            // pushing the line open.
            <span key={index} className="min-w-0 max-w-full shrink-0">
              {chip}
            </span>
          ))}
        </CollapsedOnly>
      )}
      {extra && <CollapsedOnly className="min-w-0">{extra}</CollapsedOnly>}
    </span>
  );
}
