"use client";

import { useEffect, useRef } from "react";
import { useMediaQuery } from "@/hooks/use-media-query";
import { useUIStore } from "@/store/ui-store";

/** The `cramped` variant of app/globals.css: narrow or low, a phone in either orientation. */
export const CRAMPED_QUERY = "(max-width: 39.99rem), (max-height: 31.99rem)";

/**
 * Whether the header of a list (toolbar, statistics, selection bar) is collapsed, and the ref of that header.
 *
 * On a phone the header alone may fill the screen. Scrolling the page instead of the table doesn't work there: iOS
 * doesn't hand a swipe from the table at its top on to the page, so the filters couldn't be reached again. The table
 * stays the only scroller, and its scroll drives the header as it drives the logo row (`logoCollapsed`, see
 * hooks/use-collapse-on-scroll.ts): gone once the table is scrolled, back when it is at its top again.
 *
 * The header's height is published as `listHeaderHeight`, so the hook declines to collapse for a table that would
 * then fit without scrolling (it would expand again at once).
 */
export function useListHeaderCollapse() {
  const cramped = useMediaQuery(CRAMPED_QUERY);
  const collapsed = useUIStore((s) => s.logoCollapsed) && cramped;
  const setListHeaderHeight = useUIStore((s) => s.setListHeaderHeight);
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const element = ref.current;
    if (!cramped || !element) return;
    const observer = new ResizeObserver(() => {
      // Collapsed (display: none) it measures 0: what is freed is still the last height.
      if (element.offsetHeight > 0) setListHeaderHeight(element.offsetHeight);
    });
    observer.observe(element);
    return () => {
      observer.disconnect();
      setListHeaderHeight(0);
    };
  }, [cramped, setListHeaderHeight]);

  return { ref, collapsed };
}
