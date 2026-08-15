"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export interface RangeBoundsProps {
  /** The two ends, in order; each is laid out as one box. */
  children: [ReactNode, ReactNode];
  /**
   * Container width from which the two ends sit next to each other, as a Tailwind container-query
   * variant prefix. Two date boxes need `@2xs`, two date-plus-time boxes `@xl`.
   */
  breakpoint: "@2xs" | "@xl";
  className?: string;
}

/**
 * The two ends of a range: side by side with a dash between them where there is room, stacked
 * underneath each other where there is not.
 *
 * Container queries rather than viewport ones, because the same range is rendered in a third of the
 * "all filters" dialog and in a pill popover a third of that width. Squeezed side by side, the boxes
 * would truncate the very dates they show ("24.07.2").
 *
 * Mirrors what [DatePeriodField] does for the edit forms; that one stays separate, as it carries a
 * fieldset, the form bindings and the shared error line of a validated field.
 */
export function RangeBounds({
  children,
  breakpoint,
  className,
}: RangeBoundsProps) {
  const row = breakpoint === "@2xs" ? "@2xs:flex-row" : "@xl:flex-row";
  const inline = breakpoint === "@2xs" ? "@2xs:inline" : "@xl:inline";

  return (
    <div className={cn("@container min-w-0", className)}>
      <div className={cn("flex min-w-0 flex-col gap-1", row)}>
        {children.map((bound, index) => (
          // Not stretched: both bounds cap their own width ([DateInput] at `max-w-32`, [TimeInput]
          // at the width of its notation), so a box that grew would only put a gap before the dash.
          <div key={index} className="flex min-w-0 items-center gap-1.5">
            {index > 0 && (
              // Decorative: every box is named by its own `aria-label`. Only between the two, so
              // stacked they simply sit under each other.
              <span
                aria-hidden
                className={cn("hidden text-muted-foreground", inline)}
              >
                –
              </span>
            )}
            <div className="min-w-0">{bound}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
