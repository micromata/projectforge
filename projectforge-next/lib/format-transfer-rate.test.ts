import { describe, expect, it } from "vitest";
import { formatTransferRate, type FormatContext } from "./format";

const ctx = (locale: string) => ({ locale }) as FormatContext;

describe("formatTransferRate", () => {
  it("uses kB/s below one megabyte per second", () => {
    expect(formatTransferRate(850_400, ctx("en"))).toBe("850 kB/s");
  });

  it("uses MB/s with one decimal from one megabyte per second", () => {
    expect(formatTransferRate(1_234_000, ctx("en"))).toBe("1.2 MB/s");
    expect(formatTransferRate(1_234_000, ctx("de"))).toBe("1,2 MB/s");
  });
});
