import { describe, expect, it } from "vitest";
import { RsError } from "@/lib/rs/client";
import { isAccessDenied } from "./use-read-access-guard";

/**
 * Only the predicate: the hooks around it render and redirect, which is Playwright's half (see
 * vitest.config.mts). What is worth pinning here is that exactly one status means "denied" — the
 * whole guard hangs off it, and both a false positive (a page that redirects on a lost connection)
 * and a false negative (an empty list where the user should see nothing) are silent failures.
 */
describe("isAccessDenied", () => {
  it("holds for the 403 the backend refuses a read with", () => {
    expect(
      isAccessDenied(new RsError(403, "403 Forbidden: /rs/employee/list"))
    ).toBe(true);
  });

  it("does not hold for any other status", () => {
    // 401 is not logged in, which AuthGuard answers with the login page, not a redirect home; 404 and
    // 500 are not about rights at all.
    for (const status of [400, 401, 404, 406, 500]) {
      expect(isAccessDenied(new RsError(status, `${status}`))).toBe(false);
    }
  });

  it("does not hold for a failure that carries no status", () => {
    // An aborted or offline fetch rejects with a plain Error - never a reason to take the page away.
    expect(isAccessDenied(new Error("Failed to fetch"))).toBe(false);
    expect(isAccessDenied(undefined)).toBe(false);
    expect(isAccessDenied(null)).toBe(false);
  });
});
