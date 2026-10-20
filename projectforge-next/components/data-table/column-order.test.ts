import { describe, expect, it } from "vitest";
import {
  withDeclaredColumns,
  withLockedFirst,
  withPinnedFirst,
} from "./column-order";

const ALL = ["a", "b", "c", "d"];

describe("withLockedFirst", () => {
  it("leaves the order untouched where nothing is locked", () => {
    expect(withLockedFirst(["c", "a"], [], ALL)).toEqual(["c", "a"]);
  });

  it("puts a locked column the stored order does not hold in front", () => {
    // The case of every user whose layout predates the selection column: TanStack would render it
    // last, so it has to be prepended rather than left to the table.
    expect(withLockedFirst(["a", "b"], ["__select"], ALL)).toEqual([
      "__select",
      "a",
      "b",
    ]);
  });

  it("moves a locked column the stored order holds elsewhere to the front", () => {
    expect(withLockedFirst(["a", "__select", "b"], ["__select"], ALL)).toEqual([
      "__select",
      "a",
      "b",
    ]);
  });

  it("starts from the declared columns where no order is stored", () => {
    expect(withLockedFirst([], ["c"], ALL)).toEqual(["c", "a", "b", "d"]);
  });
});

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

describe("withDeclaredColumns", () => {
  it("leaves an empty order empty, meaning the declared one", () => {
    expect(withDeclaredColumns([], ALL)).toEqual([]);
  });

  it("keeps a stored order that holds exactly the declared columns", () => {
    expect(withDeclaredColumns(["d", "c", "b", "a"], ALL)).toEqual([
      "d",
      "c",
      "b",
      "a",
    ]);
  });

  it("drops ids the table does not declare", () => {
    // A layout of the legacy React grid, stored under the same category with its own ids.
    expect(
      withDeclaredColumns(["b", "statusAsString", "a", "c", "d"], ALL)
    ).toEqual(["b", "a", "c", "d"]);
  });

  it("puts a missing column after its declared predecessor, not at the end", () => {
    expect(withDeclaredColumns(["d", "a", "c"], ALL)).toEqual([
      "d",
      "a",
      "b",
      "c",
    ]);
  });

  it("puts a missing first column in front", () => {
    expect(withDeclaredColumns(["c", "b", "d"], ALL)).toEqual([
      "a",
      "c",
      "b",
      "d",
    ]);
  });

  it("falls back to the declared order where no stored id is declared", () => {
    expect(withDeclaredColumns(["x", "y"], ALL)).toEqual(ALL);
  });
});
