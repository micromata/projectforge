import { describe, expect, it } from "vitest";
import { withMaxResults } from "./autocomplete-url";

describe("withMaxResults", () => {
  it("adds the cap to a url that has a query string", () => {
    expect(withMaxResults("user/autosearch?search=:search", 50)).toBe(
      "user/autosearch?search=:search&maxResults=50"
    );
  });

  it("adds the cap to a url without one", () => {
    expect(withMaxResults("user/autosearch", 50)).toBe(
      "user/autosearch?maxResults=50"
    );
  });

  it("replaces the cap the backend put there, wherever it stands", () => {
    // AbstractPagesRest.quickSelectUrl, verbatim.
    expect(
      withMaxResults("customer/autosearch?maxResults=30&search=:search", 100)
    ).toBe("customer/autosearch?maxResults=100&search=:search");
    expect(
      withMaxResults("kost2/autosearch?search=:search&maxResults=30", 100)
    ).toBe("kost2/autosearch?search=:search&maxResults=100");
  });

  it("leaves the search placeholder alone", () => {
    expect(withMaxResults("user/autosearch?search=:search", 50)).toContain(
      ":search"
    );
  });
});
