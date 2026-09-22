import { describe, expect, it } from "vitest";
import {
  formatDateRange,
  formatPercentageDecimal,
  formatTimestampRange,
  type FormatContext,
} from "./format";

/** A German account, as the local test account is — the layout the bounds are read in. */
const CTX: FormatContext = {
  locale: "de-DE",
  timeZone: "Europe/Berlin",
  currency: "EUR",
};

describe("formatDateRange", () => {
  it("reads both ends in the user's layout", () => {
    expect(formatDateRange("2026-01-01", "2026-12-31", CTX)).toBe(
      "01.01.2026 – 31.12.2026"
    );
  });

  it("stands an ellipsis in for the open end of a half-open period", () => {
    expect(formatDateRange("2026-01-01", null, CTX)).toBe("01.01.2026 – …");
    expect(formatDateRange(undefined, "2026-12-31", CTX)).toBe(
      "… – 31.12.2026"
    );
  });

  it("is empty for a period with neither end, so a caller can render nothing at all", () => {
    expect(formatDateRange(null, undefined, CTX)).toBe("");
    expect(formatDateRange("", "", CTX)).toBe("");
  });
});

describe("formatTimestampRange", () => {
  it("carries the time of each end, in the user's time zone", () => {
    expect(
      formatTimestampRange("2026-06-17T10:33:00Z", "2026-06-17T12:00:00Z", CTX)
    ).toBe("17.06.2026, 12:33 – 17.06.2026, 14:00");
  });
});

describe("formatPercentageDecimal", () => {
  /** The space `Intl` puts before the sign is a non-breaking one, and the layout is the user's. */
  const percent = (text: string) => text.replace(" ", " ");

  it("shows one digit behind the separator by default", () => {
    expect(formatPercentageDecimal(0.1925, CTX)).toBe(percent("19,3 %"));
  });

  it("rounds to whole percent where it is asked to — the share of a cost assignment", () => {
    expect(formatPercentageDecimal(1 / 3, CTX, 0)).toBe(percent("33 %"));
    expect(formatPercentageDecimal(1, CTX, 0)).toBe(percent("100 %"));
    // Signed: a row of the wrong sign against a positive net sum reads as one.
    expect(formatPercentageDecimal(-0.2, CTX, 0)).toBe(percent("-20 %"));
  });
});
