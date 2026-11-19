import { describe, expect, it } from "vitest";
import { hrefWithParams } from "./url-param";

describe("hrefWithParams", () => {
  it("sets params and keeps the others", () => {
    expect(
      hrefWithParams("/address", new URLSearchParams("q=ab"), {
        view: "7",
        x: "1",
      })
    ).toBe("/address?q=ab&view=7&x=1");
  });

  it("replaces and removes params, leaving no empty query", () => {
    const params = new URLSearchParams("view=3");
    expect(hrefWithParams("/address", params, { view: "7" })).toBe(
      "/address?view=7"
    );
    expect(hrefWithParams("/address", params, { view: null })).toBe("/address");
  });
});
