import { describe, expect, it } from "vitest";
import { toggledRange } from "./use-attachment-selection";

/** The range rule only; the checkbox wiring is Playwright's half. */
describe("toggledRange", () => {
  const order = ["a", "b", "c", "d", "e"];

  it("toggles only the target without an anchor", () => {
    expect([...toggledRange(new Set(), order, null, "c", true)]).toEqual(["c"]);
  });

  it("adds the span between anchor and target, in either direction", () => {
    expect([...toggledRange(new Set(), order, "b", "d", true)].sort()).toEqual([
      "b",
      "c",
      "d",
    ]);
    expect([...toggledRange(new Set(), order, "d", "b", true)].sort()).toEqual([
      "b",
      "c",
      "d",
    ]);
  });

  it("keeps what was picked outside the span", () => {
    const next = toggledRange(new Set(["a", "e"]), order, "b", "c", true);
    expect([...next].sort()).toEqual(["a", "b", "c", "e"]);
  });

  it("drops the span when the target is unticked", () => {
    const next = toggledRange(new Set(order), order, "b", "d", false);
    expect([...next].sort()).toEqual(["a", "e"]);
  });

  it("falls back to the target when the anchor is gone", () => {
    expect([...toggledRange(new Set(), order, "x", "d", true)]).toEqual(["d"]);
  });
});
