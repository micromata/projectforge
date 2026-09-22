"use client";

import * as React from "react";
import { createPortal } from "react-dom";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
} from "@/components/ui/popover";
import { useCoarsePointer } from "@/hooks/use-coarse-pointer";
import { cn } from "@/lib/utils";

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
  // On a touch device the tooltip opens on tap and dismisses as a popover, not on hover: a table has
  // no hover, so the declared and clipped explanations would be unreachable there.
  const coarsePointer = useCoarsePointer();

  const clear = React.useCallback(() => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
    current.current = null;
    setTarget(null);
  }, []);

  React.useEffect(() => clear, [clear]);

  /**
   * The element under `node` that carries a tooltip and its text — the cell's declared tooltip where
   * there is one, its clipped content otherwise. Null where the pointer/tap is over no cell, or over a
   * cell that explains nothing. Shared by the hover and the tap paths.
   */
  const resolve = React.useCallback(
    (
      node: HTMLElement | null
    ): { anchor: HTMLElement; text: string } | null => {
      const cell = node?.closest?.<HTMLElement>("th,td");
      if (!cell) return null;
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
      if (!anchor || !text) return null;
      return { anchor, text };
    },
    []
  );

  const onPointerOver = React.useCallback(
    (event: React.PointerEvent<HTMLElement>) => {
      // The tap path drives touch; a pointerover from a finger would open the tooltip a beat before
      // the tap it belongs to and then leave it hanging.
      if (coarsePointer) return;
      const found = resolve(event.target as HTMLElement | null);
      if (!found) {
        clear();
        return;
      }
      const { anchor, text } = found;
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
    [clear, coarsePointer, resolve]
  );

  const onClick = React.useCallback(
    (event: React.MouseEvent<HTMLElement>) => {
      // Only touch opens on tap; a mouse click on a cell must not raise the hover tooltip a second way.
      if (!coarsePointer) return;
      const found = resolve(event.target as HTMLElement | null);
      // A second tap on the same anchor closes it; a tap on a cell that explains nothing does too.
      if (!found || found.anchor === current.current) {
        clear();
        return;
      }
      current.current = found.anchor;
      // No delay on tap: the finger is already there, and a phone has no hover to disambiguate.
      setTarget({
        rect: found.anchor.getBoundingClientRect(),
        text: found.text,
      });
    },
    [clear, coarsePointer, resolve]
  );

  return {
    /** Spread on the element that wraps (and scrolls) the table. */
    handlers: {
      onPointerOver,
      onClick,
      // The pointer leaving dismisses the hover tooltip; on touch it would fire as the finger lifts and
      // close the tap tooltip at once, so the tap path dismisses as a popover (outside tap) instead.
      onPointerLeave: coarsePointer ? undefined : clear,
      // The anchor rect is a viewport rect, so scrolling would leave it behind.
      onScroll: clear,
    },
    tooltip: (
      <OverflowTooltip
        target={target}
        onDismiss={clear}
        coarsePointer={coarsePointer}
      />
    ),
  };
}

/** The shared look of the delegated tooltip and its tap popover — the same shape as [HintTooltip]. */
const OVERFLOW_CONTENT_CLASS =
  "max-w-sm whitespace-pre-wrap break-words text-[11px] leading-relaxed";

function OverflowTooltip({
  target,
  onDismiss,
  coarsePointer,
}: {
  target: OverflowTarget | null;
  onDismiss: () => void;
  /** Render as a tap-dismissable popover rather than a hover tooltip. */
  coarsePointer: boolean;
}) {
  if (!target) return null;
  const { rect, text } = target;
  // The placeholder for the clipped element itself: the cell cannot be the trigger without wrapping
  // every cell. Portalled to the body, not left where the table renders it: the placeholder is
  // positioned `fixed` in viewport coordinates, but `position: fixed` is relative to any transformed
  // ancestor (the centred DialogContent uses a `translate`), so inside a dialog the placeholder — and
  // with it the tooltip — would be shifted by the dialog's offset. The body has no such ancestor.
  // `createPortal` keeps the React context, so the Radix providers still apply.
  const placeholder = (
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
  );

  // On touch a popover: it opens on the tap (the hook sets the target) and dismisses on a tap outside
  // or Escape — a hover tooltip has neither on a phone. Anchored, not triggered, so the invisible
  // placeholder positions it without being pressed itself.
  const content = coarsePointer ? (
    <Popover
      key={`${rect.left}:${rect.top}`}
      open
      onOpenChange={(open) => !open && onDismiss()}
    >
      <PopoverAnchor asChild>{placeholder}</PopoverAnchor>
      <PopoverContent
        sideOffset={4}
        className={cn(OVERFLOW_CONTENT_CLASS, "w-auto")}
      >
        {text}
      </PopoverContent>
    </Popover>
  ) : (
    // Remounted per target: Radix tracks the anchor element, and this one only ever changes its
    // position, which no observer would report.
    <Tooltip
      key={`${rect.left}:${rect.top}`}
      open
      onOpenChange={(open) => !open && onDismiss()}
    >
      <TooltipTrigger asChild>{placeholder}</TooltipTrigger>
      <TooltipContent sideOffset={4} className={OVERFLOW_CONTENT_CLASS}>
        {text}
      </TooltipContent>
    </Tooltip>
  );

  return createPortal(content, document.body);
}
