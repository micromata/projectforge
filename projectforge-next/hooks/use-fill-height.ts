"use client";

import { useEffect, useState, type RefObject } from "react";

/** The nearest ancestor that scrolls vertically, or null if only the document does. */
function scrollParentOf(element: HTMLElement): HTMLElement | null {
  for (
    let parent = element.parentElement;
    parent;
    parent = parent.parentElement
  ) {
    const { overflowY } = getComputedStyle(parent);
    if (overflowY === "auto" || overflowY === "scroll") return parent;
  }
  return null;
}

/** The child of the scroll column holding the element — on an edit page its section, the target of its tab. */
function blockOf(element: HTMLElement, column: HTMLElement): HTMLElement {
  let block = element;
  while (block.parentElement && block.parentElement !== column) {
    block = block.parentElement;
  }
  return block;
}

/**
 * The height an element may take for its whole block to be seen at once in the column it scrolls in
 * (its nearest scrolling ancestor, else the viewport): the column's height less everything else in the
 * block — a section's heading and card padding, a field's label. Scrolled (or jumped by its tab) to the
 * top, the block then fills the column exactly.
 *
 * Follows a resize of the column (window, collapsing logo row) and of the block (a wrapping heading) via
 * a `ResizeObserver`. Returns `0` until mounted and measured — "not known yet", not "no room".
 */
export function useFillHeight(
  ref: RefObject<HTMLElement | null>,
  enabled = true
): number {
  const [height, setHeight] = useState(0);

  useEffect(() => {
    const element = ref.current;
    if (!enabled || !element) return;
    const column = scrollParentOf(element);
    const block = column ? blockOf(element, column) : element;
    const measure = () => {
      // The rest of the block doesn't depend on the element's own height, so setting it settles at once.
      const rest = block.offsetHeight - element.offsetHeight;
      const room = column ? column.clientHeight : window.innerHeight;
      setHeight(Math.max(0, room - rest));
    };
    measure();
    const observer = new ResizeObserver(measure);
    if (column) observer.observe(column);
    observer.observe(block);
    window.addEventListener("resize", measure);
    return () => {
      observer.disconnect();
      window.removeEventListener("resize", measure);
    };
  }, [ref, enabled]);

  return height;
}
