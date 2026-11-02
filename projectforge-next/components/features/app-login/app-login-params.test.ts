import { describe, expect, it } from "vitest";
import { readAppLoginParams } from "./app-login-params";

/** base64url without padding, as the app packs it. */
function pack(json: object): string {
  const bytes = new TextEncoder().encode(JSON.stringify(json));
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

describe("readAppLoginParams", () => {
  it("reads the packed parameter, also when the proxy dropped the single ones", () => {
    const login = pack({
      challenge: "c",
      state: "s",
      device: "Jörgs iPhone",
      platform: "ios",
      deviceId: 42,
    });
    expect(
      readAppLoginParams(new URLSearchParams(`login=${login}&challenge=c`))
    ).toEqual({
      challenge: "c",
      state: "s",
      deviceName: "Jörgs iPhone",
      platform: "ios",
      deviceId: 42,
    });
  });

  it("reads the single parameters of an older app", () => {
    expect(
      readAppLoginParams(
        new URLSearchParams("challenge=c&state=s&device=Phone&platform=android")
      )
    ).toEqual({
      challenge: "c",
      state: "s",
      deviceName: "Phone",
      platform: "android",
      deviceId: null,
    });
  });

  it("ignores an unreadable packed parameter", () => {
    expect(
      readAppLoginParams(new URLSearchParams("login=%%%&challenge=c"))
    ).toMatchObject({ challenge: "c", state: null });
    expect(
      readAppLoginParams(
        new URLSearchParams(`login=${pack({ deviceId: "x" })}`)
      )
    ).toMatchObject({ challenge: null, deviceId: null });
  });
});
