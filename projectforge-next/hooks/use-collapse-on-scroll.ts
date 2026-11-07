"use client";

import { useCallback, useEffect } from "react";
import { useUIStore } from "@/store/ui-store";

/**
 * Height of the logo row in px — keep in sync with the `h-12` in components/shared/logo-row.tsx.
 * Needed here because whether collapsing is worthwhile at all depends on it (see below).
 */
export const LOGO_ROW_HEIGHT = 48;
/** Collapse once the column has scrolled this far. */
const COLLAPSE_AT = 24;
/**
 * …and expand only back at the very top. Deliberately far below COLLAPSE_AT: that gap is the
 * hysteresis, without which the row would flutter while the user rests at the threshold.
 */
const EXPAND_AT = 4;

interface ScrollMetrics {
  scrollTop: number;
  scrollHeight: number;
  clientHeight: number;
}

/**
 * What the row should do for a column in this state. Pure, so the arithmetic can be pinned in a unit
 * test instead of only being observable in a browser (see use-collapse-on-scroll.test.ts).
 */
export function nextCollapsed(
  { scrollTop, scrollHeight, clientHeight }: ScrollMetrics,
  collapsed: boolean
): boolean {
  if (collapsed) return scrollTop > EXPAND_AT;
  // Collapsing frees the row's height, which *grows* this column and makes the browser clamp its
  // scrollTop - a real scroll event carrying a smaller value. A column that overflows by barely more
  // than that height is clamped back above EXPAND_AT, so it would expand again immediately: one
  // visible jump, and the user's scroll lost. Such a column has nothing to gain here anyway.
  if (scrollHeight - clientHeight <= LOGO_ROW_HEIGHT + COLLAPSE_AT)
    return false;
  return scrollTop > COLLAPSE_AT;
}

/**
 * Lets a scroll container drive the logo row: the row collapses once this column has scrolled past a
 * small threshold and comes back when the column is at the very top again.
 *
 * Every scroll container has to opt in by hand because React attaches `scroll` to the element
 * carrying the prop rather than delegating it — the event does not bubble, so a handler further out
 * (on <main>, say) never sees an inner column scroll.
 *
 * The container is read off the event instead of from a ref, which keeps the opt-in to one attribute
 * and leaves the refs those containers already carry alone.
 *
 * @param enabled False for a column that is not the page's own: a table inside a dialog or a bounded
 * table inside a form must not move the app's header (see SelectedEntriesTable).
 */
export function useCollapseOnScroll(enabled = true) {
  const setLogoCollapsed = useUIStore((s) => s.setLogoCollapsed);

  const onScroll = useCallback(
    (event: React.UIEvent<HTMLElement>) => {
      if (!enabled) return;
      // getState() rather than a subscription: the current value is needed for the hysteresis, but
      // subscribing would re-render every scroll container on each flip for nothing.
      setLogoCollapsed(
        nextCollapsed(event.currentTarget, useUIStore.getState().logoCollapsed)
      );
    },
    [enabled, setLogoCollapsed]
  );

  // A column that goes away takes its say with it: the next page starts at the top, so the row is
  // back. This covers navigating away; PageShell handles a route change that unmounts nothing.
  useEffect(() => {
    if (!enabled) return;
    return () => setLogoCollapsed(false);
  }, [enabled, setLogoCollapsed]);

  return { onScroll };
}
