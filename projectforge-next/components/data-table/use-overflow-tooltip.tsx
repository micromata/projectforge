"use client";

import * as React from "react";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";

/** Delay before a truncated cell reveals its full content, ms. */
const DELAY = 400;

/**
 * Marks an element inside a cell or header as carrying its own tooltip: a column's declared
 * `tooltipPath`, a backend `headerTooltip`, the sort indicators. Shown by this hook rather than by a
 * [HintTooltip] of its own — one Radix root per cell would be hundreds per page (see below).
 */
export const TOOLTIP_ATTR = "data-tooltip";

interface OverflowTarget {
  /** Viewport rect of the clipped element; the tooltip is anchored to it. */
  rect: DOMRect;
  text: string;
}

/** Sub-pixel tolerance, px: a fractional text width is not clipped content. */
const EPSILON = 1;

/**
 * Whether `el` cuts its own text off — the text measured directly rather than via
 * `scrollWidth`.
 *
 * `scrollWidth` rounds the whole scrollable area up to an integer *including the
 * padding*, so it exceeds `clientWidth` on any cell whose text ends within a
 * pixel or two of the content box. That was most of them, and each offered a
 * tooltip repeating what the cell already showed. A Range over the contents gives
 * the text's real width, `clientWidth` minus the horizontal padding the room it
 * has.
 */
function clipsText(el: HTMLElement): boolean {
  const style = getComputedStyle(el);
  // Only an element hiding its overflow can clip anything — and an inline one
  // reports `clientWidth` 0, which would make every text look wider than its box.
  if (style.overflowX !== "hidden" && style.overflowX !== "clip") return false;
  const available =
    el.clientWidth -
    parseFloat(style.paddingLeft) -
    parseFloat(style.paddingRight);
  const range = document.createRange();
  range.selectNodeContents(el);
  return range.getBoundingClientRect().width > available + EPSILON;
}

/**
 * The innermost element of a cell that cuts its text off, or null while all of it
 * is readable.
 *
 * Innermost, because a Range over an element measures its children's *boxes*: on
 * a cell whose renderer truncates in a span of its own, a Range over the <td>
 * returns that span's clipped width and reports the cell as fitting. Only the
 * element directly holding the text yields the width the text wants.
 */
function findClipped(cell: HTMLElement): HTMLElement | null {
  // A header's label is marked, because the cell also holds the sort index and
  // the filter button and their text is not part of the label.
  const marked = cell.querySelector<HTMLElement>("[data-overflow-text]");
  if (marked) return clipsText(marked) ? marked : null;
  const nodes = cell.querySelectorAll<HTMLElement>("*");
  for (let i = nodes.length - 1; i >= 0; i--) {
    if (clipsText(nodes[i])) return nodes[i];
  }
  // No wrapper of its own: the <td> is `truncate` itself (the hand built columns).
  return clipsText(cell) ? cell : null;
}

/**
 * Shows a table cell's or header's tooltip on hover: its declared one
 * ([TOOLTIP_ATTR]) where there is one, the full content otherwise where the cell
 * clips it.
 *
 * One tooltip for the whole table, driven by event delegation, rather than a
 * Tooltip per cell: a page of rows times its columns is several hundred cells,
 * and each would carry its own Radix root and its own measurement. Which is why
 * the declared tooltips go through here as well instead of through
 * [HintTooltip] — the look is the same either way.
 */
export function useOverflowTooltip() {
  const [target, setTarget] = React.useState<OverflowTarget | null>(null);
  const timer = React.useRef<ReturnType<typeof setTimeout> | null>(null);
  /** The element the pending or shown tooltip belongs to. */
  const current = React.useRef<HTMLElement | null>(null);

  const clear = React.useCallback(() => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
    current.current = null;
    setTarget(null);
  }, []);

  React.useEffect(() => clear, [clear]);

  const onPointerOver = React.useCallback(
    (event: React.PointerEvent<HTMLElement>) => {
      const node = event.target as HTMLElement | null;
      const cell = node?.closest?.<HTMLElement>("th,td");
      if (!cell) {
        clear();
        return;
      }
      // A declared tooltip wins over the clipped text, and is anchored to the
      // element that declares it: the sort indicator explains itself, not the
      // column label beside it.
      const declared = node!.closest<HTMLElement>(`[${TOOLTIP_ATTR}]`);
      const anchor =
        declared && cell.contains(declared) ? declared : findClipped(cell);
      const text =
        anchor && anchor === declared
          ? declared.getAttribute(TOOLTIP_ATTR)?.trim()
          : anchor?.innerText.trim();
      if (!anchor || !text) {
        clear();
        return;
      }
      // pointerover bubbles once per descendant the pointer enters, so the same
      // anchor arrives repeatedly; re-measuring it would restart the delay and
      // the tooltip would never appear. Keyed on the anchor rather than the cell,
      // so moving from a header's label to its sort indicator does switch.
      if (anchor === current.current) return;
      if (timer.current) clearTimeout(timer.current);
      current.current = anchor;
      timer.current = setTimeout(
        () => setTarget({ rect: anchor.getBoundingClientRect(), text }),
        DELAY
      );
    },
    [clear]
  );

  return {
    /** Spread on the element that wraps (and scrolls) the table. */
    handlers: {
      onPointerOver,
      onPointerLeave: clear,
      // The anchor rect is a viewport rect, so scrolling would leave it behind.
      onScroll: clear,
    },
    tooltip: <OverflowTooltip target={target} onDismiss={clear} />,
  };
}

function OverflowTooltip({
  target,
  onDismiss,
}: {
  target: OverflowTarget | null;
  onDismiss: () => void;
}) {
  if (!target) return null;
  const { rect, text } = target;
  return (
    // Remounted per target: Radix tracks the anchor element, and this one only
    // ever changes its position, which no observer would report.
    <Tooltip
      key={`${rect.left}:${rect.top}`}
      open
      onOpenChange={(open) => !open && onDismiss()}
    >
      <TooltipTrigger asChild>
        {/* Placeholder for the clipped element itself: the cell cannot be the
            trigger without wrapping every cell in a tooltip. */}
        <span
          aria-hidden
          className="pointer-events-none fixed"
          style={{
            left: rect.left,
            top: rect.top,
            width: rect.width,
            height: rect.height,
          }}
        />
      </TooltipTrigger>
      {/* The same shape as [HintTooltip]: this is the app's tooltip, only anchored by delegation. */}
      <TooltipContent
        sideOffset={4}
        className="max-w-sm whitespace-pre-wrap break-words text-[11px] leading-relaxed"
      >
        {text}
      </TooltipContent>
    </Tooltip>
  );
}
