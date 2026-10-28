import { describe, expect, it } from "vitest";
import { isRouteDisabled } from "./feature-routes";

describe("isRouteDisabled", () => {
  const routes = {
    disabled: ["address", "datatransfer/-1"],
    enabled: ["address/public"],
  };

  it("blocks the route and the pages below it", () => {
    expect(isRouteDisabled("/address", routes)).toBe(true);
    expect(isRouteDisabled("/address/", routes)).toBe(true);
    expect(isRouteDisabled("/address/42/", routes)).toBe(true);
    expect(isRouteDisabled("/datatransfer/-1/", routes)).toBe(true);
  });

  it("compares whole segments", () => {
    expect(isRouteDisabled("/addressBook/", routes)).toBe(false);
    expect(isRouteDisabled("/datatransfer/", routes)).toBe(false);
    expect(isRouteDisabled("/datatransfer/-10/", routes)).toBe(false);
  });

  it("lets the longest route decide", () => {
    expect(isRouteDisabled("/address/public/1", routes)).toBe(false);
  });

  it("blocks nothing without routes", () => {
    expect(isRouteDisabled("/address/", null)).toBe(false);
    expect(isRouteDisabled("/address/", { disabled: [], enabled: [] })).toBe(false);
  });
});
