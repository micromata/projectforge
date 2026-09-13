"use client";

import { useEffect, useState, type RefObject } from "react";

/**
 * Tracks an element's live `clientWidth` via a `ResizeObserver`.
 *
 * `clientWidth` (not `getBoundingClientRect().width`) so the value excludes a vertical scrollbar — that
 * is the true room content has to lay out in. Returns `0` until the element is mounted and measured, so
 * callers should treat `0` as "not known yet" rather than "zero width".
 */
export function useElementWidth(ref: RefObject<HTMLElement | null>): number {
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;
    const measure = () => setWidth(element.clientWidth);
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, [ref]);

  return width;
}
