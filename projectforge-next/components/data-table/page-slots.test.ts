import { describe, expect, it } from "vitest";
import { PAGE_GAP, PAGE_SLOTS, pageSlots } from "./page-slots";

describe("pageSlots", () => {
  it("shows every page of a short list", () => {
    expect(pageSlots(0, 4)).toEqual([1, 2, 3, 4]);
    expect(pageSlots(0, 1)).toEqual([1]);
    // A list with nothing in it still has the page the user is looking at.
    expect(pageSlots(0, 0)).toEqual([1]);
  });

  it("keeps the same number of slots on every page of a long list", () => {
    // The point of the whole helper: the strip may not change length while the user pages through it,
    // or the arrows beside it move away from under the mouse.
    for (const pageCount of [8, 12, 40, 999]) {
      for (let pageIndex = 0; pageIndex < pageCount; pageIndex++) {
        expect(pageSlots(pageIndex, pageCount)).toHaveLength(PAGE_SLOTS);
      }
    }
  });

  it("always offers the current page, the first and the last", () => {
    for (const pageCount of [8, 12, 40, 999]) {
      for (let pageIndex = 0; pageIndex < pageCount; pageIndex++) {
        const slots = pageSlots(pageIndex, pageCount);
        expect(slots).toContain(pageIndex + 1);
        expect(slots[0]).toBe(1);
        expect(slots[slots.length - 1]).toBe(pageCount);
      }
    }
  });

  it("puts the gap where the strip skips pages, and nowhere else", () => {
    // Near the start there is nothing to skip on the left, so the window reaches from page 2.
    expect(pageSlots(0, 12)).toEqual([1, 2, 3, 4, 5, PAGE_GAP, 12]);
    // In the middle the current page sits between its neighbours, with a gap on either side.
    expect(pageSlots(5, 12)).toEqual([1, PAGE_GAP, 5, 6, 7, PAGE_GAP, 12]);
    // Near the end, mirrored.
    expect(pageSlots(11, 12)).toEqual([1, PAGE_GAP, 8, 9, 10, 11, 12]);
    // Exactly one page more than there are slots: a single page is skipped, and it is a gap rather
    // than an eighth slot.
    expect(pageSlots(0, PAGE_SLOTS + 1)).toEqual([
      1,
      2,
      3,
      4,
      5,
      PAGE_GAP,
      PAGE_SLOTS + 1,
    ]);
  });
});
