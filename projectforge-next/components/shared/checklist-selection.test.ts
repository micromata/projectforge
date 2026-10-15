import { describe, expect, it } from "vitest";
import {
  matchesTerm,
  toggled,
  visibleSelectionState,
  withVisible,
} from "./checklist-selection";

describe("matchesTerm", () => {
  it("matches every word anywhere, ignoring case", () => {
    expect(matchesTerm("473 - Air Liquide GmbH", "liquide gmbh")).toBe(true);
    expect(matchesTerm("473 - Air Liquide GmbH", "air ag")).toBe(false);
  });

  it("lets an empty term match all", () => {
    expect(matchesTerm("anything", "  ")).toBe(true);
  });
});

describe("visibleSelectionState", () => {
  it("is ticked, unticked or indeterminate over the visible entries only", () => {
    expect(visibleSelectionState(["a", "b"], ["a", "b"])).toBe(true);
    expect(visibleSelectionState(["a", "x"], ["a", "b"])).toBe("indeterminate");
    expect(visibleSelectionState(["x"], ["a", "b"])).toBe(false);
    expect(visibleSelectionState(["a"], [])).toBe(false);
  });
});

describe("withVisible", () => {
  it("adds the visible entries to the picks kept outside the search", () => {
    expect(withVisible(["x", "a"], ["a", "b"], true)).toEqual(["x", "a", "b"]);
  });

  it("removes only the visible entries", () => {
    expect(withVisible(["x", "a", "b"], ["a", "b"], false)).toEqual(["x"]);
  });
});

describe("toggled", () => {
  it("flips one entry", () => {
    expect(toggled(["a"], "b")).toEqual(["a", "b"]);
    expect(toggled(["a", "b"], "a")).toEqual(["b"]);
  });
});
