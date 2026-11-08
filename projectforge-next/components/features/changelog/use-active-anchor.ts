"use client";

import { type RefObject, useEffect, useState } from "react";

/** A heading counts as passed above this line, measured from the top of the scroll column. */
const ACTIVE_OFFSET = 48;

/**
 * The id of the last entry whose element has passed the top of the scroll column [container] (`<main>` of
 * PageShell); at the end of the column the last one, which can't reach the line (as `doc-toc.js` of the website).
 */
export function useActiveAnchor(
  container: RefObject<HTMLElement | null>,
  ids: string[]
): string | null {
  const [active, setActive] = useState<string | null>(null);
  useEffect(() => {
    const scroller = container.current?.closest("main");
    if (!scroller) return;
    let frame = 0;
    const update = () => {
      frame = 0;
      const line = scroller.getBoundingClientRect().top + ACTIVE_OFFSET;
      let current: string | null = null;
      for (const id of ids) {
        const element = document.getElementById(id);
        if (!element) continue;
        if (element.getBoundingClientRect().top > line) break;
        current = id;
      }
      if (
        ids.length > 0 &&
        scroller.scrollTop + scroller.clientHeight >= scroller.scrollHeight - 2
      ) {
        current = ids[ids.length - 1];
      }
      setActive(current);
    };
    const schedule = () => {
      if (!frame) frame = window.requestAnimationFrame(update);
    };
    scroller.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", schedule);
    update();
    return () => {
      scroller.removeEventListener("scroll", schedule);
      window.removeEventListener("resize", schedule);
      if (frame) window.cancelAnimationFrame(frame);
    };
  }, [container, ids]);
  return active;
}
