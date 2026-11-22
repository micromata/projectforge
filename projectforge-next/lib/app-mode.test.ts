import { afterEach, describe, expect, it, vi } from "vitest";
import {
  appConfirms2FAInBrowser,
  appOpensPasskeySetupInBrowser,
  isInApp,
} from "./app-mode";

function userAgent(value: string) {
  vi.stubGlobal("navigator", { userAgent: value });
}

describe("app-mode", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("a browser is no app", () => {
    userAgent("Mozilla/5.0 Safari/605.1");
    expect(isInApp()).toBe(false);
    expect(appOpensPasskeySetupInBrowser()).toBe(false);
  });

  it("an older app does not open the passkey setup in the browser", () => {
    userAgent("Mozilla/5.0 ProjectForgeApp/1.4.0 (browser2FA)");
    expect(isInApp()).toBe(true);
    expect(appConfirms2FAInBrowser()).toBe(true);
    expect(appOpensPasskeySetupInBrowser()).toBe(false);
  });

  it("an app with the marker does", () => {
    userAgent(
      "Mozilla/5.0 ProjectForgeApp/1.5.0 (browser2FA) (browserPasskey)"
    );
    expect(appOpensPasskeySetupInBrowser()).toBe(true);
  });
});
