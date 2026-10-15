/**
 * Which page numbers the pagination strip offers, as a row of *fixed length*.
 *
 * The length is what this is about: the arrows sit left and right of the strip, so a strip that grows
 * by a slot when the current page moves away from the ends takes the arrows with it — and paging
 * through a list means clicking the same arrow again, which then is no longer under the mouse. Hence
 * one number gives way to a gap rather than the gap being one more slot: stepping from page 2 to 3
 * changes what the strip says, never how much room it takes.
 *
 * The width of a slot is the other half of the same promise and belongs to the markup, since it is a
 * question of digits, not of pages: see `.pagination-slot` in globals.css.
 */

/**
 * How many slots the strip has once a list has more pages than that: the first page, the last one,
 * and a window of five in between. Odd, so the current page can sit in the middle of that window.
 */
export const PAGE_SLOTS = 7;

const INNER_SLOTS = PAGE_SLOTS - 2;

/** A gap the strip skips over. Rendered as one slot wide, like a page number. */
export const PAGE_GAP = "…";

export type PageSlot = number | typeof PAGE_GAP;

export function pageSlots(pageIndex: number, pageCount: number): PageSlot[] {
  // Few enough pages to show every one of them - and then the strip cannot change length at all.
  if (pageCount <= PAGE_SLOTS) return pages(1, Math.max(pageCount, 1));

  const current = pageIndex + 1;
  // Five consecutive pages inside 2..pageCount-1, sliding with the current page and clamped at both
  // ends - which is what keeps the strip as long near the ends as it is in the middle.
  const start = Math.min(Math.max(current - 2, 2), pageCount - INNER_SLOTS);
  const end = start + INNER_SLOTS - 1;
  const inner: PageSlot[] = pages(start, end);
  // The window's outermost page gives way to the gap where one is needed. Never the current page: it
  // is the middle of the window, and at the ends, where the clamp moved the window, the gap is on the
  // far side from it.
  if (start > 2) inner[0] = PAGE_GAP;
  if (end < pageCount - 1) inner[INNER_SLOTS - 1] = PAGE_GAP;
  return [1, ...inner, pageCount];
}

function pages(from: number, to: number): number[] {
  return Array.from({ length: to - from + 1 }, (_, i) => from + i);
}
