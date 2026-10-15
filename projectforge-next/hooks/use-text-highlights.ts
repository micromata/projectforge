"use client";

import { type RefObject, useCallback, useEffect, useState } from "react";

/** The names of the highlights, styled by `::highlight(…)` in globals.css. */
const MATCH = "search-match";
const CURRENT = "search-match-current";

/**
 * The paint of the highlights, the tint of `.text-match` (globals.css tokens). Added at runtime rather
 * than in globals.css: the CSS build (Lightning CSS) does not know `::highlight()` yet and flags it.
 */
const STYLE_ID = "search-highlight-style";
const STYLE = `::highlight(${MATCH}) { background-color: var(--search-match-bg); }
::highlight(${CURRENT}) { background-color: var(--search-match-current-bg); }`;

function ensureStyle() {
  if (document.getElementById(STYLE_ID)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = STYLE;
  document.head.appendChild(style);
}

/**
 * Every occurrence of a term in the text below [root], in document order; occurrences of different
 * terms that overlap ("ldap" in "ldaps" for "ldap ldaps") are merged into one.
 */
function findRanges(root: HTMLElement, terms: string[]): Range[] {
  const ranges: Range[] = [];
  if (terms.length === 0) return ranges;
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    const text = node.textContent?.toLowerCase() ?? "";
    const hits: [number, number][] = [];
    for (const term of terms) {
      for (
        let at = text.indexOf(term);
        at >= 0;
        at = text.indexOf(term, at + term.length)
      ) {
        hits.push([at, at + term.length]);
      }
    }
    hits.sort((a, b) => a[0] - b[0]);
    let last: Range | null = null;
    for (const [start, end] of hits) {
      if (last && start < last.endOffset) {
        last.setEnd(node, Math.max(end, last.endOffset));
        continue;
      }
      last = document.createRange();
      last.setStart(node, start);
      last.setEnd(node, end);
      ranges.push(last);
    }
  }
  return ranges;
}

/**
 * Find in page for a search over rendered content: highlights every occurrence of the (lower-case)
 * [terms] in the text below [ref], the current one set apart, and steps through them.
 *
 * Painted with the CSS Custom Highlight API, so the DOM React renders is not touched — it works in
 * markdown and any other markup alike. Where the browser lacks the API, counting and stepping still work,
 * only without the paint. Content appearing or disappearing below [ref] (a section folded or unfolded)
 * is picked up on its own.
 */
export function useTextHighlights(
  ref: RefObject<HTMLElement | null>,
  terms: string[]
) {
  const [ranges, setRanges] = useState<Range[]>([]);
  const [index, setIndex] = useState(0);

  useEffect(() => {
    const root = ref.current;
    if (!root) return;
    let frame = 0;
    const update = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => setRanges(findRanges(root, terms)));
    };
    update();
    setIndex(0);
    const observer = new MutationObserver(update);
    observer.observe(root, {
      childList: true,
      subtree: true,
      characterData: true,
    });
    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, [ref, terms]);

  const current = Math.min(index, Math.max(ranges.length - 1, 0));

  useEffect(() => {
    if (typeof CSS === "undefined" || !("highlights" in CSS)) return;
    ensureStyle();
    CSS.highlights.set(
      MATCH,
      new Highlight(...ranges.filter((_, i) => i !== current))
    );
    if (ranges[current])
      CSS.highlights.set(CURRENT, new Highlight(ranges[current]));
    return () => {
      CSS.highlights.delete(MATCH);
      CSS.highlights.delete(CURRENT);
    };
  }, [ranges, current]);

  const step = useCallback(
    (by: number) => {
      if (ranges.length === 0) return;
      const target = (current + by + ranges.length) % ranges.length;
      setIndex(target);
      ranges[target].startContainer.parentElement?.scrollIntoView({
        block: "center",
        behavior: "smooth",
      });
    },
    [ranges, current]
  );

  return {
    count: ranges.length,
    /** The current match, 0-based; 0 also when there is none. */
    current,
    next: useCallback(() => step(1), [step]),
    previous: useCallback(() => step(-1), [step]),
  };
}
