"use client";

import { type RefObject, useCallback } from "react";

/** Below this distance from the end, a list counts as "at the end": new messages scroll it along. */
export const AT_END_PX = 80;

export function isAtEnd(element: HTMLElement) {
  return (
    element.scrollHeight - element.scrollTop - element.clientHeight < AT_END_PX
  );
}

/**
 * Keeps a scrolling list at its end while its content grows later, e.g. by an image loaded after it was scrolled to
 * the end, as long as [atEnd] says it is there. Returns the ref of the content inside the scrolling element.
 */
export function useFollowEnd(
  scrollRef: RefObject<HTMLElement | null>,
  atEnd: RefObject<boolean>
) {
  // A ref callback: the content may be rendered later than the list (a thread in a sheet).
  return useCallback(
    (content: HTMLElement | null) => {
      if (!content) return undefined;
      const observer = new ResizeObserver(() => {
        const element = scrollRef.current;
        if (element && atEnd.current) element.scrollTop = element.scrollHeight;
      });
      observer.observe(content);
      return () => observer.disconnect();
    },
    [scrollRef, atEnd]
  );
}
