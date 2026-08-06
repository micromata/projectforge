"use client";

import { useCallback, useEffect, useRef, useState } from "react";

type OverflowOptions = {
  /** Number of items rendered into the measurement row. */
  itemCount: number;
  /** Horizontal gap between items in px, must match the container's `gap-*` class. */
  gap?: number;
  /** Width to keep free for the "show more" trigger when not everything fits, in px. */
  triggerWidth?: number;
};

/**
 * Decides how many of a horizontal list's items fit into the available width.
 *
 * The items are measured off-screen (the caller renders them once into `measureRef`, hidden), so
 * the widths stay known even while an item sits in the overflow menu. Recomputes whenever the
 * container resizes or the item count changes.
 */
export function useOverflowCount({
  itemCount,
  gap = 4,
  triggerWidth = 40,
}: OverflowOptions) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const measureRef = useRef<HTMLDivElement | null>(null);
  const [visibleCount, setVisibleCount] = useState(itemCount);

  const measure = useCallback(() => {
    const container = containerRef.current;
    const measureRow = measureRef.current;
    if (!container || !measureRow) return;

    const widths = Array.from(measureRow.children).map(
      (child) => (child as HTMLElement).getBoundingClientRect().width
    );
    // Measurement row not laid out yet (e.g. display:none parent) — keep the current guess.
    if (widths.length === 0 || widths.every((width) => width === 0)) return;

    const available = container.getBoundingClientRect().width;
    let used = 0;
    let fitting = 0;
    for (const width of widths) {
      const next = used + width + (fitting > 0 ? gap : 0);
      if (next > available) break;
      used = next;
      fitting += 1;
    }

    if (fitting < widths.length) {
      // Everything did not fit: the trigger needs room too, so drop items until it has some.
      const budget = available - triggerWidth - (fitting > 0 ? gap : 0);
      while (fitting > 0 && used > budget) {
        fitting -= 1;
        used -= widths[fitting] + (fitting > 0 ? gap : 0);
      }
    }

    setVisibleCount(fitting);
  }, [gap, triggerWidth]);

  useEffect(() => {
    measure();
    const container = containerRef.current;
    if (!container) return;
    const observer = new ResizeObserver(measure);
    observer.observe(container);
    if (measureRef.current) observer.observe(measureRef.current);
    return () => observer.disconnect();
  }, [measure, itemCount]);

  return { containerRef, measureRef, visibleCount };
}
