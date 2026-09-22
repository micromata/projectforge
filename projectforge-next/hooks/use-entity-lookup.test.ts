import { describe, expect, it } from "vitest";
import { isNearBottom } from "./use-entity-lookup";

/**
 * Only the scroll decision, not the hook: what the picker does with the entries is Playwright's half
 * (see e2e/entity-lookup.spec.ts). Worth pinning here because both failure modes are ones a browser
 * shows only by accident — a list that never loads the next page, and one that asks for it while the
 * user is still at the top.
 */
describe("isNearBottom", () => {
  /** cmdk's list: 300px of viewport over 900px of entries. */
  const list = { scrollHeight: 900, clientHeight: 300 };

  it("is false while there are entries left to scroll through", () => {
    expect(isNearBottom({ ...list, scrollTop: 0 })).toBe(false);
    expect(isNearBottom({ ...list, scrollTop: 400 })).toBe(false);
  });

  it("is true within the last stretch, before the end is reached", () => {
    // 600 is the bottom; the next page is asked for a little before it, so the entries are there by
    // the time the user arrives.
    expect(isNearBottom({ ...list, scrollTop: 560 })).toBe(true);
    expect(isNearBottom({ ...list, scrollTop: 600 })).toBe(true);
  });

  it("is true for a list too short to scroll", () => {
    // Three entries in a 300px box: the first page is already all of it on screen, and asking for the
    // next one must not wait for a scroll that cannot happen.
    expect(
      isNearBottom({ scrollTop: 0, scrollHeight: 96, clientHeight: 300 })
    ).toBe(true);
  });
});
