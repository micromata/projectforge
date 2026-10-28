import { describe, expect, it } from "vitest";
import { userAvatarUrl } from "./user-avatar";

describe("userAvatarUrl", () => {
  it("versions the url, so a changed avatar isn't taken from the browser cache", () => {
    expect(userAvatarUrl(42, 1700000000000)).toBe(
      "/rs/userAvatar/42?v=1700000000000"
    );
  });

  it("asks for the small preview for chat and menu", () => {
    expect(userAvatarUrl(42, 7, true)).toBe("/rs/userAvatar/42/preview?v=7");
  });
});
