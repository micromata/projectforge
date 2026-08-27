import { describe, expect, it } from "vitest";
import {
  emptyRecurrence,
  parseRecurrence,
  recurrenceMode,
  serializeRecurrence,
  untilInstant,
  type RecurrenceModel,
} from "./recurrence-rrule";

/**
 * The round trip between the stored `recurrenceRule` string and the form's model is the whole risk here:
 * a rule the parser widens or narrows would, on the next save, rewrite a series the user never touched.
 * These pin the mapping — and, deliberately, that a rule the UI cannot express (an empty one, an hourly
 * one) reads as "no recurrence" rather than being mangled.
 */
describe("parseRecurrence", () => {
  it("reads an empty or FREQ-less rule as no recurrence", () => {
    expect(parseRecurrence(null)).toEqual(emptyRecurrence());
    expect(parseRecurrence("")).toEqual(emptyRecurrence());
    expect(parseRecurrence("INTERVAL=2")).toEqual(emptyRecurrence());
  });

  it("reads a frequency the form does not offer as no recurrence", () => {
    expect(parseRecurrence("FREQ=HOURLY;INTERVAL=1").freq).toBeNull();
  });

  it("tolerates the RRULE: prefix the backend strips", () => {
    expect(parseRecurrence("RRULE:FREQ=DAILY;INTERVAL=1").freq).toBe("DAILY");
  });

  it("reads frequency, interval and weekdays", () => {
    expect(parseRecurrence("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR")).toEqual({
      freq: "WEEKLY",
      interval: 2,
      byWeekday: ["MO", "WE", "FR"],
      until: null,
    });
  });

  it("keeps weekdays in calendar order regardless of the rule's order", () => {
    expect(
      parseRecurrence("FREQ=WEEKLY;INTERVAL=1;BYDAY=FR,MO").byWeekday
    ).toEqual(["MO", "FR"]);
  });

  it("reads UNTIL as the day it names", () => {
    expect(
      parseRecurrence("FREQ=DAILY;INTERVAL=1;UNTIL=20261231T235959Z").until
    ).toBe("2026-12-31");
  });
});

describe("serializeRecurrence", () => {
  it("writes the empty string for no recurrence", () => {
    expect(serializeRecurrence(emptyRecurrence())).toBe("");
  });

  it("always writes an interval, as the legacy form did", () => {
    expect(serializeRecurrence({ ...emptyRecurrence(), freq: "DAILY" })).toBe(
      "FREQ=DAILY;INTERVAL=1"
    );
  });

  it("writes BYDAY only for a weekly rule with days picked", () => {
    const weekly: RecurrenceModel = {
      freq: "WEEKLY",
      interval: 1,
      byWeekday: ["MO", "WE"],
      until: null,
    };
    expect(serializeRecurrence(weekly)).toContain("BYDAY=MO,WE");
    // A monthly rule ignores whatever days linger in the model.
    expect(serializeRecurrence({ ...weekly, freq: "MONTHLY" })).not.toContain(
      "BYDAY"
    );
  });

  it("round-trips a rule with every field set", () => {
    const rule = "FREQ=WEEKLY;INTERVAL=3;UNTIL=20270115T235959Z;BYDAY=TU,TH";
    expect(parseRecurrence(serializeRecurrence(parseRecurrence(rule)))).toEqual(
      parseRecurrence(rule)
    );
  });
});

describe("recurrenceMode", () => {
  it("is none without a frequency", () => {
    expect(recurrenceMode(emptyRecurrence())).toBe("NONE");
  });

  it("is the plain frequency for a bare rule with interval 1", () => {
    expect(recurrenceMode(parseRecurrence("FREQ=WEEKLY;INTERVAL=1"))).toBe(
      "WEEKLY"
    );
    expect(recurrenceMode(parseRecurrence("FREQ=DAILY;INTERVAL=1"))).toBe(
      "DAILY"
    );
  });

  it("is customized once an interval, weekdays or an end date is set", () => {
    expect(recurrenceMode(parseRecurrence("FREQ=DAILY;INTERVAL=2"))).toBe(
      "CUSTOMIZED"
    );
    expect(
      recurrenceMode(parseRecurrence("FREQ=WEEKLY;INTERVAL=1;BYDAY=MO"))
    ).toBe("CUSTOMIZED");
    expect(
      recurrenceMode(
        parseRecurrence("FREQ=DAILY;INTERVAL=1;UNTIL=20261231T235959Z")
      )
    ).toBe("CUSTOMIZED");
  });
});

describe("untilInstant", () => {
  it("is null without an until", () => {
    expect(untilInstant(null)).toBeNull();
  });

  it("is the last second of the day in UTC", () => {
    expect(untilInstant("2026-12-31")).toBe("2026-12-31T23:59:59.000Z");
  });
});
