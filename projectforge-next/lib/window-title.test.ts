import { describe, expect, it } from "vitest";
import { APP_TITLE, windowTitle } from "./window-title";

describe("windowTitle", () => {
  it("puts the page before the application", () => {
    expect(windowTitle("Kunden")).toBe("Kunden – ProjectForge");
  });

  it("trims the page title", () => {
    expect(windowTitle("  Kunden ")).toBe("Kunden – ProjectForge");
  });

  it("is the bare application name without a page title", () => {
    expect(windowTitle(undefined)).toBe(APP_TITLE);
    expect(windowTitle(null)).toBe(APP_TITLE);
    expect(windowTitle("")).toBe(APP_TITLE);
    expect(windowTitle("   ")).toBe(APP_TITLE);
  });
});
