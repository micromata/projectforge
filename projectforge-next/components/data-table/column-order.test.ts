import { describe, expect, it } from "vitest";
import { withPinnedFirst } from "./column-order";

const ALL = ["a", "b", "c", "d"];

describe("withPinnedFirst", () => {
  it("leaves the order untouched where nothing is pinned", () => {
    expect(withPinnedFirst(["c", "a"], {}, ALL)).toEqual(["c", "a"]);
  });

  it("pulls the left-pinned columns to the front, in pinning order", () => {
    expect(
      withPinnedFirst(["a", "b", "c", "d"], { left: ["c", "b"] }, ALL)
    ).toEqual(["c", "b", "a", "d"]);
  });

  it("pushes the right-pinned columns to the end", () => {
    expect(withPinnedFirst(["a", "b", "c"], { right: ["a"] }, ALL)).toEqual([
      "b",
      "c",
      "a",
    ]);
  });

  it("starts from the declared columns where no order is stored", () => {
    expect(withPinnedFirst([], { left: ["d"] }, ALL)).toEqual([
      "d",
      "a",
      "b",
      "c",
    ]);
  });

  it("ignores a pinned column the order does not hold — a stored state of a column since removed", () => {
    expect(withPinnedFirst(["a", "b"], { left: ["gone", "b"] }, ALL)).toEqual([
      "b",
      "a",
    ]);
  });
});
