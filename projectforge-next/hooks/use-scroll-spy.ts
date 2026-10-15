"use client";

import { useCallback, useEffect, useRef, useState } from "react";

interface UseScrollSpyOptions {
  offset?: number;
}

/**
 * The active section of a scroll column: derived from the scroll position, but overridden by a click
 * in the tab bar until the user scrolls themselves.
 *
 * The override is not a nicety. The sections at the end of the column cannot be scrolled to the top
 * — the column runs out of scroll before they get there, and several of them can share the very same
 * bottom position. Reading the position back would therefore always name the same one of them, so a
 * click has to be remembered rather than re-derived.
 */
export function useScrollSpy(sectionCount: number, opts?: UseScrollSpyOptions) {
  const offset = opts?.offset ?? 80;

  const scrollRef = useRef<HTMLDivElement | null>(null);
  const sectionRefs = useRef<(HTMLDivElement | null)[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  /** Section a click chose, held until the user scrolls of their own accord. */
  const pinnedIndex = useRef<number | null>(null);
  /** Keeps the pinned section aligned while it unfolds (see scrollToSection). */
  const alignObserver = useRef<ResizeObserver | null>(null);

  // The observer outlives a single scroll, so a leaving page must let go of it.
  useEffect(() => () => alignObserver.current?.disconnect(), []);

  const sectionRef = useCallback(
    (index: number) => (el: HTMLDivElement | null) => {
      sectionRefs.current[index] = el;
    },
    []
  );

  const onScroll = useCallback(() => {
    // A pinned section stays pinned: this event is the animation started by the click, and the
    // gesture handlers below are what releases it.
    if (pinnedIndex.current != null) return;
    const container = scrollRef.current;
    if (!container) return;
    // The section the probe line runs through — the first one still reaching past it. Asking which
    // sections have *passed* the line instead would name the last one above it, and a section the
    // column can never scroll to the top has none above it but the first: scrolling up a little from
    // the bottom would jump the bar to section one while that is nowhere near the screen.
    const probe = container.scrollTop + offset;
    let active = 0;
    for (let i = 0; i < sectionCount; i++) {
      const bounds = sectionBounds(container, sectionRefs.current[i]);
      if (!bounds) continue;
      active = i;
      if (bounds.bottom > probe) break;
    }
    setActiveIndex(active);
  }, [sectionCount, offset]);

  /**
   * Props for the scroll column. The gestures are what tells a scroll of the user's from the
   * animation a click starts — the scroll event itself cannot, both look identical in `scrollTop`.
   */
  const scrollProps = {
    ref: scrollRef,
    onScroll,
    onWheel: () => {
      pinnedIndex.current = null;
    },
    onTouchMove: () => {
      pinnedIndex.current = null;
    },
    onPointerDown: () => {
      // The scrollbar itself: a drag on it starts on the column, not on any of its children.
      pinnedIndex.current = null;
    },
    onKeyDown: () => {
      pinnedIndex.current = null;
    },
  };

  const scrollToSection = useCallback((index: number) => {
    pinnedIndex.current = index;
    setActiveIndex(index);
    const container = scrollRef.current;
    const el = sectionRefs.current[index];
    if (!container || !el) return;

    // As far up as the column can bring it, which for the last sections is short of the top. The
    // first section lands at 0, so the column really does scroll all the way back up.
    const alignTop = (behavior: ScrollBehavior) => {
      const bounds = sectionBounds(container, el);
      if (bounds) container.scrollTo({ top: bounds.top, behavior });
    };
    alignTop("smooth");

    // A folded section unfolds when its tab is clicked, so it grows *after* this scroll — and while
    // it was still folded the column had nothing below it to scroll against, so the last section's
    // top could not reach the top of the column and the freshly revealed body hung off the bottom.
    // Track the growth and re-align, until the section settles or the user scrolls away.
    alignObserver.current?.disconnect();
    let lastHeight = el.getBoundingClientRect().height;
    const observer = new ResizeObserver(() => {
      if (pinnedIndex.current !== index) {
        observer.disconnect();
        return;
      }
      const height = el.getBoundingClientRect().height;
      // The observer fires once on connect with the current size; only a real change is the unfold.
      if (height === lastHeight) return;
      lastHeight = height;
      alignTop("auto");
    });
    observer.observe(el);
    alignObserver.current = observer;
  }, []);

  // No `scrollRef`/`onScroll` of their own: they are part of `scrollProps`, and a column wired up
  // with only those two would silently keep the clicked section pinned forever.
  return { scrollProps, sectionRef, activeIndex, scrollToSection };
}

/**
 * Where `el` sits within the scrollable content of `container`, in the same coordinates as
 * `scrollTop`: `top` is the position that puts it at the top of the column.
 *
 * Measured through the rects rather than `offsetTop`: that one counts from the nearest *positioned*
 * ancestor, which the scroll column need not be — where it isn't, every offset carries the height of
 * the page header and the tab bar with it, and the whole column is off by that much.
 */
function sectionBounds(
  container: HTMLElement | null,
  el: HTMLElement | null
): { top: number; bottom: number } | null {
  if (!container || !el) return null;
  const rect = el.getBoundingClientRect();
  const top =
    rect.top - container.getBoundingClientRect().top + container.scrollTop;
  return { top, bottom: top + rect.height };
}
