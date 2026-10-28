/** The box of a chip on the screen, as `getBoundingClientRect` gives it. */
export interface ChipRect {
  left: number;
  right: number;
  top: number;
  bottom: number;
}

export interface Point {
  x: number;
  y: number;
}

/**
 * Where a point lies between the chips, as a caret lies between characters: the number of chips before
 * it, in reading order. A chip is before the point when its line is above the point, or when it is on
 * the point's line and its middle is left of it.
 */
function caretIndex(rects: readonly ChipRect[], point: Point): number {
  return rects.filter(
    (rect) =>
      rect.bottom <= point.y ||
      (rect.top <= point.y && (rect.left + rect.right) / 2 < point.x)
  ).length;
}

function chipAt(rects: readonly ChipRect[], point: Point): number {
  return rects.findIndex(
    (rect) =>
      rect.left <= point.x &&
      point.x <= rect.right &&
      rect.top <= point.y &&
      point.y <= rect.bottom
  );
}

/**
 * The chips swept over by a drag from `start` (between chips) to `current`, as text is selected: from
 * the caret at the start to the caret at the end, over wrapped lines too. A chip under `current` counts
 * whole — the user points at the last chip they want.
 *
 * Returns the indices `[from, to)` of `rects`, which are in the order shown; empty when nothing is
 * between the two.
 */
export function sweptIndices(
  rects: readonly ChipRect[],
  start: Point,
  current: Point
): [number, number] {
  const from = caretIndex(rects, start);
  const hit = chipAt(rects, current);
  if (hit >= 0) {
    return hit >= from ? [from, hit + 1] : [hit, from];
  }
  const to = caretIndex(rects, current);
  return from <= to ? [from, to] : [to, from];
}
