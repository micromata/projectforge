import { describe, expect, it } from "vitest";
import { isRefArray } from "./ref-list";

describe("isRefArray", () => {
  it("takes a non-empty list of objects as references", () => {
    expect(
      isRefArray([{ id: 1, displayName: "Fink", deactivated: true }])
    ).toBe(true);
  });

  it("leaves plain values, empty and mixed lists alone", () => {
    expect(isRefArray([])).toBe(false);
    expect(isRefArray(["a", "b"])).toBe(false);
    expect(isRefArray([{ id: 1 }, "b"])).toBe(false);
    expect(isRefArray({ id: 1 })).toBe(false);
    expect(isRefArray(null)).toBe(false);
  });
});
