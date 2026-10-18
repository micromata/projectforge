import { describe, expect, it } from "vitest";
import { dialogListOf } from "./use-address-edit-outcome";

describe("dialogListOf", () => {
  it("returns an address opened from the dialog of a list to that list", () => {
    expect(dialogListOf(42, "/address")).toBe("/address");
    expect(dialogListOf(42, "/address-campaign-value")).toBe(
      "/address-campaign-value"
    );
  });

  it("leaves a new address to its view, although the add button of the list names the list", () => {
    expect(dialogListOf(null, "/address")).toBeNull();
  });

  it("ignores a list that doesn't open the dialog", () => {
    expect(dialogListOf(42, "/calendar")).toBeNull();
    expect(dialogListOf(42, null)).toBeNull();
  });
});
