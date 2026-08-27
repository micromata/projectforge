"use client";

import { useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { cn } from "@/lib/utils";
import { parseTooltipHtml } from "./tooltip-html";
import type { CalendarEventExtendedProps } from "@/lib/rs/calendar-types";

/** Gap between the cursor and the card, and the least space kept to the viewport edge. */
const OFFSET = 14;
const MARGIN = 8;

/**
 * The hover card for an event: the backend's tooltip parsed into a labelled table (see `tooltip-html.ts`
 * for why it is parsed rather than injected), with the event's duration as a footer.
 *
 * It is a plain fixed-position box pinned to `anchor` — the viewport point where the pointer entered the
 * event — not a popper anchored to the event element. That is deliberate: a popper re-tracks its trigger
 * on every frame, so as the event scrolled away the card rode up with it, over the toolbar. A frozen
 * viewport point does not move when the grid scrolls, so the card stays where it opened until the pointer
 * leaves (the caller closes it then, and on scroll). Measured once to flip against the viewport edges.
 */
export function CalendarEventTooltip({
  props,
  anchor,
}: {
  props: CalendarEventExtendedProps;
  anchor: { x: number; y: number };
}) {
  const rows = useMemo(
    () => (props.tooltip ? parseTooltipHtml(props.tooltip.text) : []),
    [props.tooltip]
  );
  const ref = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState({
    left: anchor.x + OFFSET,
    top: anchor.y + OFFSET,
  });

  // Place below-right of the cursor, flipping to the other side of it when the card would otherwise
  // overflow the viewport, then clamping so it never sits under the edge.
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    const { width, height } = el.getBoundingClientRect();
    let left = anchor.x + OFFSET;
    let top = anchor.y + OFFSET;
    if (left + width + MARGIN > window.innerWidth)
      left = anchor.x - width - OFFSET;
    if (top + height + MARGIN > window.innerHeight)
      top = anchor.y - height - OFFSET;
    left = Math.max(MARGIN, Math.min(left, window.innerWidth - width - MARGIN));
    top = Math.max(MARGIN, Math.min(top, window.innerHeight - height - MARGIN));
    setPos({ left, top });
  }, [anchor.x, anchor.y]);

  return createPortal(
    <div
      ref={ref}
      role="tooltip"
      style={{ left: pos.left, top: pos.top }}
      className="pointer-events-none fixed z-50 max-w-sm rounded-md border bg-popover px-4 py-3 text-sm text-popover-foreground shadow-md"
    >
      {props.tooltip?.title && (
        <p className="mb-2 font-semibold">{props.tooltip.title}</p>
      )}
      <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1">
        {rows.map((row, index) => (
          <div key={`${row.label}-${index}`} className="contents">
            <dt className="text-muted-foreground">{row.label}</dt>
            {/* `min-w-0` lets the 1fr cell shrink below its content, `break-words` breaks a long
                value with no spaces (a structure path's dotted segments) so it wraps inside the card
                instead of running past its border. */}
            <dd
              className={cn(
                "min-w-0 break-words",
                row.multiline && "whitespace-pre-wrap"
              )}
            >
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
    </div>,
    document.body
  );
}
