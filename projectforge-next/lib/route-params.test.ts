import { describe, expect, it } from "vitest";
import { matchRoute } from "./route-params";

describe("matchRoute", () => {
  it("reads a single param", () => {
    expect(matchRoute("/book/[id]", "/book/25219084")).toEqual({
      id: "25219084",
    });
  });

  it("accepts a trailing slash, as trailingSlash: true produces", () => {
    expect(matchRoute("/book/[id]", "/book/42/")).toEqual({ id: "42" });
  });

  it("keeps the literal segments apart from the params", () => {
    expect(matchRoute("/order/[id]/forecast", "/order/42/forecast")).toEqual({
      id: "42",
    });
    // /order/[id] must not swallow the deeper route, or a deep link to the forecast page would
    // render the edit form with id "42".
    expect(matchRoute("/order/[id]", "/order/42/forecast")).toBeNull();
    expect(matchRoute("/order/[id]/forecast", "/order/42")).toBeNull();
  });

  it("rejects a path that is short or of another route", () => {
    expect(matchRoute("/book/[id]", "/book")).toBeNull();
    expect(matchRoute("/book/[id]", "/demo/42")).toBeNull();
    expect(matchRoute("/book/[id]", "/")).toBeNull();
  });

  it("collects a catch-all into a list and needs at least one part", () => {
    expect(
      matchRoute("/[category]/[type]/[...params]", "/address/edit/42")
    ).toEqual({ category: "address", type: "edit", params: ["42"] });
    expect(
      matchRoute("/[category]/[type]/[...params]", "/address/edit/42/extra")
    ).toEqual({ category: "address", type: "edit", params: ["42", "extra"] });
    expect(
      matchRoute("/[category]/[type]/[...params]", "/address/edit")
    ).toBeNull();
  });

  it("decodes a segment, as Next's own params are decoded", () => {
    expect(matchRoute("/[category]", "/time%20sheet")).toEqual({
      category: "time sheet",
    });
    // A malformed escape is passed through rather than thrown on.
    expect(matchRoute("/[category]", "/100%")).toEqual({ category: "100%" });
  });
});
