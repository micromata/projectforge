import { describe, expect, it } from "vitest";
import type { FormatContext } from "./format";
import {
  formatTimeInput,
  hourLabelOf,
  parseTimeInput,
  timeOf,
  timePartsOf,
  timePatternOf,
} from "./time-parse";

/** H24, as a German account has it by default. */
const h24: FormatContext = { locale: "de-DE", hour12: false };
/** H12 — the setting this module exists for. */
const h12: FormatContext = { locale: "en-US", hour12: true };
/** No setting sent: whatever the locale does, the module must not crash on it. */
const unset: FormatContext = { locale: "de-DE" };

describe("parseTimeInput", () => {
  it("reads a time written out in full", () => {
    expect(parseTimeInput("14:30", h24)).toBe("14:30");
    expect(parseTimeInput("09:05", h24)).toBe("09:05");
  });

  it("fills in the minutes of a bare hour", () => {
    expect(parseTimeInput("9", h24)).toBe("09:00");
    expect(parseTimeInput("14", h24)).toBe("14:00");
  });

  it("reads a time typed without a separator", () => {
    expect(parseTimeInput("930", h24)).toBe("09:30");
    expect(parseTimeInput("0930", h24)).toBe("09:30");
    expect(parseTimeInput("1430", h24)).toBe("14:30");
  });

  it("takes any separator, since layouts differ", () => {
    expect(parseTimeInput("14.30", h24)).toBe("14:30");
    expect(parseTimeInput("14 30", h24)).toBe("14:30");
  });

  it("shifts a pm time into the 24h clock", () => {
    expect(parseTimeInput("2:30 PM", h12)).toBe("14:30");
    expect(parseTimeInput("2:30pm", h12)).toBe("14:30");
    expect(parseTimeInput("2:30 p", h12)).toBe("14:30");
    expect(parseTimeInput("2:30 AM", h12)).toBe("02:30");
  });

  it("puts the two twelves where the convention wants them", () => {
    expect(parseTimeInput("12:00 AM", h12)).toBe("00:00");
    expect(parseTimeInput("12:30 PM", h12)).toBe("12:30");
  });

  it("understands the day period in the locale's own words", () => {
    // Whatever de-DE writes for the afternoon ("nachm." in most ICU versions).
    const written = formatTimeInput("14:30", { locale: "de-DE", hour12: true });
    expect(parseTimeInput(written, { locale: "de-DE", hour12: true })).toBe(
      "14:30"
    );
  });

  it("accepts a 24h time even from an H12 account, since it is unambiguous", () => {
    expect(parseTimeInput("14:30", h12)).toBe("14:30");
  });

  it("rejects what is not a time", () => {
    expect(parseTimeInput("", h24)).toBeNull();
    expect(parseTimeInput("abc", h24)).toBeNull();
    expect(parseTimeInput("24:00", h24)).toBeNull();
    expect(parseTimeInput("12:60", h24)).toBeNull();
    expect(parseTimeInput("123456", h24)).toBeNull();
    // 14 pm is not a time in either notation.
    expect(parseTimeInput("14:30 PM", h12)).toBeNull();
  });
});

describe("formatTimeInput", () => {
  it("writes the 24h clock for an H24 account", () => {
    expect(formatTimeInput("14:30", h24)).toBe("14:30");
    expect(formatTimeInput("00:00", h24)).toBe("00:00");
    expect(formatTimeInput("9:05", h24)).toBe("09:05");
  });

  it("writes AM/PM for an H12 account", () => {
    expect(formatTimeInput("14:30", h12)).toBe("2:30 pm");
    expect(formatTimeInput("00:30", h12)).toBe("12:30 am");
    expect(formatTimeInput("12:00", h12)).toBe("12:00 pm");
  });

  it("falls back to the 24h clock when the account has no setting", () => {
    expect(formatTimeInput("14:30", unset)).toBe("14:30");
  });

  it("has nothing to show for no time", () => {
    expect(formatTimeInput(null, h24)).toBe("");
    expect(formatTimeInput("nonsense", h24)).toBe("");
  });

  it("round-trips through the parser in both notations", () => {
    for (const ctx of [h24, h12, unset]) {
      for (const time of ["00:00", "07:45", "12:00", "13:01", "23:59"]) {
        expect(parseTimeInput(formatTimeInput(time, ctx), ctx)).toBe(time);
      }
    }
  });
});

describe("timePatternOf", () => {
  it("shows the notation the account expects", () => {
    expect(timePatternOf(h24)).toBe("HH:mm");
    expect(timePatternOf(h12)).toMatch(/^hh:mm /);
    expect(timePatternOf(unset)).toBe("HH:mm");
  });
});

describe("hourLabelOf", () => {
  it("writes the plain 24h hour for an H24 account", () => {
    expect(hourLabelOf(0, h24)).toBe("00");
    expect(hourLabelOf(9, h24)).toBe("09");
    expect(hourLabelOf(23, h24)).toBe("23");
  });

  it("names the half of the day for an H12 account", () => {
    // The two twelves, which are the only hours the 12h clock gets wrong when done naively.
    expect(hourLabelOf(0, h12)).toBe("12 am");
    expect(hourLabelOf(12, h12)).toBe("12 pm");
    expect(hourLabelOf(13, h12)).toBe("1 pm");
    expect(hourLabelOf(11, h12)).toBe("11 am");
  });

  it("labels every hour of the day distinctly, so no picker row is ambiguous", () => {
    for (const ctx of [h24, h12, unset]) {
      const labels = Array.from({ length: 24 }, (_, hour) =>
        hourLabelOf(hour, ctx)
      );
      expect(new Set(labels).size).toBe(24);
    }
  });

  it("labels an hour the parser reads back as that hour", () => {
    for (const ctx of [h24, h12]) {
      for (let hour = 0; hour < 24; hour++) {
        // What a picked hour means: the label plus ":00" is the time itself.
        const label = hourLabelOf(hour, ctx);
        const written = ctx.hour12
          ? label.replace(/^(\d+) /, "$1:00 ")
          : `${label}:00`;
        expect(parseTimeInput(written, ctx)).toBe(timeOf(hour, 0));
      }
    }
  });
});

describe("timeOf", () => {
  it("pads both halves, as the wire format has them", () => {
    expect(timeOf(9, 5)).toBe("09:05");
    expect(timeOf(0, 0)).toBe("00:00");
    expect(timeOf(23, 45)).toBe("23:45");
  });
});

describe("timePartsOf", () => {
  it("splits a wire time into the numbers the picker highlights", () => {
    expect(timePartsOf("13:45")).toEqual([13, 45]);
    expect(timePartsOf("00:00")).toEqual([0, 0]);
  });

  it("has no parts for a missing or impossible time", () => {
    expect(timePartsOf(null)).toBeNull();
    expect(timePartsOf("")).toBeNull();
    expect(timePartsOf("24:00")).toBeNull();
    expect(timePartsOf("12:60")).toBeNull();
  });
});
