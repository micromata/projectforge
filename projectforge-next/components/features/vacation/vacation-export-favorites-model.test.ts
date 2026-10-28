import { describe, expect, it } from "vitest";
import {
  favoriteIdHolding,
  sameChoice,
  sameIds,
} from "./vacation-export-favorites-model";

describe("sameIds", () => {
  it("ignores order and duplicates", () => {
    expect(sameIds([1, 2, 3], [3, 1, 2])).toBe(true);
    expect(sameIds([1, 2, 2], [2, 1])).toBe(true);
    expect(sameIds([], [])).toBe(true);
  });

  it("tells different sets apart", () => {
    expect(sameIds([1, 2], [1, 2, 3])).toBe(false);
    expect(sameIds([1, 2, 3], [1, 2])).toBe(false);
    expect(sameIds([1, 2], [1, 3])).toBe(false);
  });
});

describe("sameChoice", () => {
  it("compares users and groups each as a set", () => {
    expect(
      sameChoice(
        { userIds: [2, 1], groupIds: [7] },
        { userIds: [1, 2], groupIds: [7] }
      )
    ).toBe(true);
    expect(
      sameChoice(
        { userIds: [1], groupIds: [7] },
        { userIds: [1], groupIds: [] }
      )
    ).toBe(false);
  });
});

describe("favoriteIdHolding", () => {
  const favorites = [
    { id: 1, userIds: [1], groupIds: [] },
    { id: 2, userIds: [3, 2], groupIds: [9] },
  ];

  it("finds the favorite holding the choice, in whatever order", () => {
    expect(
      favoriteIdHolding(favorites, { userIds: [2, 3], groupIds: [9] })
    ).toBe(2);
  });

  it("answers null without one", () => {
    expect(
      favoriteIdHolding(favorites, { userIds: [2], groupIds: [9] })
    ).toBeNull();
  });
});
