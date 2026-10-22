import { describe, expect, it } from "vitest";
import { sweptIndices, type ChipRect } from "./chip-sweep";

/** Chips 40 px wide with 10 px between, `perLine` to a line of 24 px height and 6 px between lines. */
function layout(count: number, perLine: number): ChipRect[] {
  return Array.from({ length: count }, (_, i) => {
    const left = 10 + (i % perLine) * 50;
    const top = 2 + Math.floor(i / perLine) * 30;
    return { left, right: left + 40, top, bottom: top + 24 };
  });
}

describe("sweptIndices", () => {
  const rects = layout(6, 3); // line 1: 0,1,2 — line 2: 3,4,5

  it("selects from before the first chip up to the one pointed at", () => {
    expect(sweptIndices(rects, { x: 3, y: 14 }, { x: 70, y: 14 })).toEqual([
      0, 2,
    ]);
  });

  it("selects from a gap to a gap, over a wrapped line", () => {
    // From between 0 and 1 to between 3 and 4.
    expect(sweptIndices(rects, { x: 55, y: 14 }, { x: 55, y: 44 })).toEqual([
      1, 4,
    ]);
  });

  it("selects backwards as well", () => {
    // From between 3 and 4 back to chip 1.
    expect(sweptIndices(rects, { x: 55, y: 44 }, { x: 70, y: 14 })).toEqual([
      1, 4,
    ]);
  });

  it("selects nothing while the pointer stays in the gap it started in", () => {
    expect(sweptIndices(rects, { x: 55, y: 14 }, { x: 57, y: 18 })).toEqual([
      1, 1,
    ]);
  });

  it("takes the rest of a line when the pointer is right of its last chip", () => {
    expect(sweptIndices(rects, { x: 3, y: 14 }, { x: 300, y: 14 })).toEqual([
      0, 3,
    ]);
  });

  it("selects backwards from behind the last chip", () => {
    expect(sweptIndices(rects, { x: 300, y: 44 }, { x: 80, y: 44 })).toEqual([
      4, 6,
    ]);
  });
});
