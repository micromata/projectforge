"use client";

import { useEffect, useState } from "react";
import { isInApp } from "@/lib/app-mode";

/** Below this (in px of the finger) a move is no pull yet: a tap or the start of a scroll. */
const PULL_START = 8;
/** The indicator follows the finger at half its way, as the native controls do. */
const DAMPING = 0.5;
/** A pull this far (in px of the indicator) reloads on release. */
export const PULL_THRESHOLD = 70;

/**
 * Where a pull may not start: fields (a drag there selects text or moves the caret), an open dialog, menu or
 * popover, and what marks itself with `data-no-pull-refresh` (the chat's message list, which loads older messages
 * at its top).
 */
const EXCLUDED =
  "input, textarea, select, [contenteditable=''], [contenteditable='true'], [role='dialog'], [role='alertdialog'], " +
  "[role='menu'], [role='listbox'], [data-radix-popper-content-wrapper], [data-no-pull-refresh]";

/** One element from the touched one up to the root, as far as the decision needs it. */
export interface PullAncestor {
  /** The element scrolls vertically and is scrolled down. */
  scrolledDown: boolean;
  excluded: boolean;
}

/**
 * Whether a touch on the first element of [chain] may become a pull: no element up to the root is scrolled down
 * (the pull would then be the user scrolling back up) or excluded. Pure, so it can be tested without a DOM.
 */
export function canStartPull(chain: PullAncestor[]): boolean {
  return chain.every((it) => !it.scrolledDown && !it.excluded);
}

/**
 * The state of a touch after a move by (dx, dy) from its start: "undecided" while the finger hasn't gone
 * [PULL_START] px yet, "pull" with the distance of the indicator once it goes down more than sideways, "none" for
 * any other direction (the touch is then left to the page: scrolling, a swipe, a drag).
 */
export function pullStep(
  dx: number,
  dy: number
):
  | { kind: "undecided" }
  | { kind: "none" }
  | { kind: "pull"; distance: number } {
  if (Math.hypot(dx, dy) < PULL_START) return { kind: "undecided" };
  if (dy <= 0 || Math.abs(dx) >= dy) return { kind: "none" };
  return { kind: "pull", distance: (dy - PULL_START) * DAMPING };
}

function ancestors(target: EventTarget | null): PullAncestor[] {
  const chain: PullAncestor[] = [];
  let element = target instanceof Element ? target : null;
  while (element) {
    const overflowY = getComputedStyle(element).overflowY;
    const scrolls =
      (overflowY === "auto" || overflowY === "scroll") &&
      element.scrollHeight > element.clientHeight;
    chain.push({
      scrolledDown: scrolls && element.scrollTop > 0,
      excluded: element.matches(EXCLUDED),
    });
    element = element.parentElement;
  }
  // The document itself (a page scrolling its body, a short viewport).
  chain.push({
    scrolledDown: (document.scrollingElement?.scrollTop ?? 0) > 0,
    excluded: false,
  });
  return chain;
}

/**
 * Pull to refresh for the pages in the app (lib/app-mode.ts). The pages scroll in columns inside a box of the
 * window's height (components/shared/page-shell.tsx), the document never: the WebView's own pull to refresh sees no
 * overscroll and never fires (iOS), or would fire on every downward swipe, even in a column scrolled down
 * (Android). The app therefore switches its own off on these pages, and the pages do it here, where the column
 * under the finger is known.
 *
 * Returns the distance of the indicator (null: no pull) and whether the page is reloading.
 */
export function usePullToRefresh(): {
  distance: number | null;
  reloading: boolean;
} {
  const [distance, setDistance] = useState<number | null>(null);
  const [reloading, setReloading] = useState(false);

  useEffect(() => {
    if (!isInApp()) return;
    let start: { x: number; y: number } | null = null;
    let pulling = false;
    let current = 0;

    const reset = () => {
      start = null;
      pulling = false;
      current = 0;
      setDistance(null);
    };

    const onTouchStart = (event: TouchEvent) => {
      reset();
      const touch = event.touches[0];
      // A second finger (pinch to zoom) ends any pull.
      if (event.touches.length !== 1 || !touch) return;
      // An open modal locks the scrolling of the page (react-remove-scroll).
      if (document.body.hasAttribute("data-scroll-locked")) return;
      if (!canStartPull(ancestors(event.target))) return;
      start = { x: touch.clientX, y: touch.clientY };
    };

    const onTouchMove = (event: TouchEvent) => {
      const touch = event.touches[0];
      if (!start || !touch) return;
      const step = pullStep(touch.clientX - start.x, touch.clientY - start.y);
      if (step.kind === "undecided") return;
      if (step.kind === "none" && !pulling) {
        // Scrolling, a swipe or a drag: the page's from now on.
        start = null;
        return;
      }
      pulling = true;
      // The column at its top would otherwise rubber-band along (iOS).
      if (event.cancelable) event.preventDefault();
      current = step.kind === "pull" ? step.distance : 0;
      setDistance(current);
    };

    const onTouchEnd = () => {
      if (pulling && current >= PULL_THRESHOLD) {
        setReloading(true);
        setDistance(PULL_THRESHOLD);
        start = null;
        pulling = false;
        window.location.reload();
        return;
      }
      reset();
    };

    document.addEventListener("touchstart", onTouchStart, { passive: true });
    // Not passive: a pull stops the rubber band of the column (preventDefault).
    document.addEventListener("touchmove", onTouchMove, { passive: false });
    document.addEventListener("touchend", onTouchEnd, { passive: true });
    document.addEventListener("touchcancel", reset, { passive: true });
    return () => {
      document.removeEventListener("touchstart", onTouchStart);
      document.removeEventListener("touchmove", onTouchMove);
      document.removeEventListener("touchend", onTouchEnd);
      document.removeEventListener("touchcancel", reset);
    };
  }, []);

  return { distance, reloading };
}
