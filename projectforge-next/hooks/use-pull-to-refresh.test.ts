import { describe, expect, it } from "vitest";
import { canStartPull, pullStep } from "./use-pull-to-refresh";

/**
 * Only the decisions, not the touch wiring: whether a touch may become a pull, and what a move makes of it. Both
 * fail in ways a phone shows only now and then: a reload while the user scrolls a list back up, or a horizontal
 * swipe through a table that reloads the page.
 */
describe("canStartPull", () => {
  const plain = { scrolledDown: false, excluded: false };

  it("starts on a page at its top", () => {
    expect(canStartPull([plain, plain, plain])).toBe(true);
  });

  it("doesn't start in a column scrolled down, however deep the touched element sits", () => {
    expect(
      canStartPull([plain, { scrolledDown: true, excluded: false }, plain])
    ).toBe(false);
  });

  it("doesn't start in a field, a dialog or a marked element", () => {
    expect(canStartPull([plain, { scrolledDown: false, excluded: true }])).toBe(
      false
    );
  });
});

describe("pullStep", () => {
  it("waits until the finger has gone a few px", () => {
    expect(pullStep(2, 5).kind).toBe("undecided");
  });

  it("pulls downwards, at half the finger's way", () => {
    expect(pullStep(0, 108)).toEqual({ kind: "pull", distance: 50 });
  });

  it("leaves upward, sideways and diagonal-sideways moves to the page", () => {
    expect(pullStep(0, -40).kind).toBe("none");
    expect(pullStep(40, 0).kind).toBe("none");
    expect(pullStep(40, 30).kind).toBe("none");
  });
});
