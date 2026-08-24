"use client";

import { useMemo } from "react";
import { HoverCardContent } from "@/components/ui/hover-card";
import { cn } from "@/lib/utils";
import { parseTooltipHtml } from "./tooltip-html";
import type { CalendarEventExtendedProps } from "@/lib/rs/calendar-types";

/**
 * The hover-card body for an event: the backend's tooltip parsed into a labelled table (see
 * `tooltip-html.ts` for why it is parsed rather than injected), with the event's duration as a footer.
 * Nothing is rendered when the event carries no tooltip — the caller only mounts the card then.
 */
export function CalendarEventTooltip({
  props,
}: {
  props: CalendarEventExtendedProps;
}) {
  const rows = useMemo(
    () => (props.tooltip ? parseTooltipHtml(props.tooltip.text) : []),
    [props.tooltip]
  );

  return (
    <HoverCardContent className="w-auto max-w-sm text-sm">
      {props.tooltip?.title && (
        <p className="mb-2 font-semibold">{props.tooltip.title}</p>
      )}
      <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1">
        {rows.map((row, index) => (
          <div key={`${row.label}-${index}`} className="contents">
            <dt className="text-muted-foreground">{row.label}</dt>
            <dd className={cn(row.multiline && "whitespace-pre-wrap")}>
              {row.value}
            </dd>
          </div>
        ))}
      </dl>
      {props.duration && (
        <p className="mt-2 text-right text-xs text-muted-foreground">
          {props.duration}
        </p>
      )}
    </HoverCardContent>
  );
}
