import { describe, expect, it } from "vitest";
import { withSearchParams } from "./search-params";

describe("withSearchParams", () => {
  it("sets and removes parameters, keeping the others", () => {
    expect(
      withSearchParams("?id=42&tab=overview", { tab: "problems", id: null })
    ).toBe("?tab=problems");
    expect(withSearchParams("", { subsystem: "ldap" })).toBe("?subsystem=ldap");
    expect(withSearchParams("?q=a+b", { tab: "history" })).toBe(
      "?q=a+b&tab=history"
    );
  });

  it("yields an empty string without any parameter left", () => {
    expect(withSearchParams("?tab=history", { tab: null })).toBe("");
    expect(withSearchParams("", {})).toBe("");
  });
});
