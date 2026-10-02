import { describe, expect, it } from "vitest";
import {
  TRANSIENT_PAGE_SIZE,
  DEFAULT_PAGE_SIZE,
  storablePageSize,
} from "./page-size-options";

describe("storablePageSize", () => {
  it("keeps a numeric page size", () => {
    expect(storablePageSize(25)).toBe(25);
    expect(storablePageSize(1000, 50)).toBe(1000);
  });

  it("never stores the transient size, falling back to the given size", () => {
    expect(storablePageSize(TRANSIENT_PAGE_SIZE, 200)).toBe(200);
    expect(storablePageSize(TRANSIENT_PAGE_SIZE)).toBe(DEFAULT_PAGE_SIZE);
  });

  it("seeds a list that has no stored size with the default", () => {
    expect(storablePageSize(undefined)).toBe(DEFAULT_PAGE_SIZE);
  });
});
