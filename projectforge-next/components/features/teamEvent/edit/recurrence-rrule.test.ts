import { describe, expect, it } from "vitest";
import {
  emptyRecurrence,
  indicesToOnTheDay,
  onTheDayToIndices,
  type RecurrenceModel,
} from "./recurrence-model";
import {
  parseRecurrence,
  recurrenceMode,
  serializeRecurrence,
  untilInstant,
} from "./recurrence-rrule";

/** A model with the fields under test set over the defaults, for the round-trip assertions. */
function model(over: Partial<RecurrenceModel>): RecurrenceModel {
  return { ...emptyRecurrence(), ...over };
}

/**
 * The whole risk is the two conversions: a rule the mapping widens or narrows would, on the next save,
 * rewrite a series the user never touched. Each case pins a shape the legacy `react-rrule-generator`
 * produced and is asserted in both directions — the stored rule read into the UI model (`rule` → `model`,
 * `parseRecurrence`) and the model written back to the canonical rule string (`model` → `serialized`,
 * `serializeRecurrence`). `serialized` defaults to `rule`; it is only spelt out where the canonical field
 * order differs from the order the incoming `rule` happened to use (`rrule` fixes `UNTIL` after `BYDAY`).
 */
describe("rule ↔ model conversion", () => {
  const cases: {
    name: string;
    rule: string;
    /** The exact `serializeRecurrence` output, when it differs from `rule`; otherwise `rule` is used. */
    serialized?: string;
    model: Partial<RecurrenceModel>;
  }[] = [
    {
      name: "yearly on a month and day",
      rule: "FREQ=YEARLY;BYMONTH=3;BYMONTHDAY=15",
      model: {
        freq: "YEARLY",
        yearlyMode: "ON",
        yearlyMonth: 3,
        yearlyDay: 15,
      },
    },
    {
      name: "yearly on the last weekday of a month",
      rule: "FREQ=YEARLY;BYSETPOS=-1;BYDAY=MO,TU,WE,TH,FR;BYMONTH=6",
      model: {
        freq: "YEARLY",
        yearlyMode: "ONTHE",
        yearlyMonth: 6,
        which: -1,
        onTheDay: "WEEKDAY",
      },
    },
    {
      name: "yearly on the first day (the DAY group) of a month",
      rule: "FREQ=YEARLY;BYSETPOS=1;BYDAY=MO,TU,WE,TH,FR,SA,SU;BYMONTH=1",
      model: {
        freq: "YEARLY",
        yearlyMode: "ONTHE",
        yearlyMonth: 1,
        which: 1,
        onTheDay: "DAY",
      },
    },
    {
      name: "monthly on a day of month",
      rule: "FREQ=MONTHLY;INTERVAL=2;BYMONTHDAY=1",
      model: { freq: "MONTHLY", interval: 2, monthlyMode: "ON", monthlyDay: 1 },
    },
    {
      name: "monthly on the first Monday",
      rule: "FREQ=MONTHLY;INTERVAL=1;BYSETPOS=1;BYDAY=MO",
      model: {
        freq: "MONTHLY",
        monthlyMode: "ONTHE",
        which: 1,
        onTheDay: "MO",
      },
    },
    {
      name: "monthly on the last weekend day (the WEEKENDDAY group)",
      rule: "FREQ=MONTHLY;INTERVAL=1;BYSETPOS=-1;BYDAY=SA,SU",
      model: {
        freq: "MONTHLY",
        monthlyMode: "ONTHE",
        which: -1,
        onTheDay: "WEEKENDDAY",
      },
    },
    {
      name: "weekly on several days",
      rule: "FREQ=WEEKLY;INTERVAL=3;BYDAY=TU,TH",
      model: { freq: "WEEKLY", interval: 3, weeklyDays: ["TU", "TH"] },
    },
    {
      name: "daily",
      rule: "FREQ=DAILY;INTERVAL=1",
      model: { freq: "DAILY" },
    },
    {
      name: "an end after a count",
      rule: "FREQ=DAILY;INTERVAL=1;COUNT=10",
      model: { freq: "DAILY", endMode: "COUNT", count: 10 },
    },
    {
      name: "an end on a date",
      rule: "FREQ=WEEKLY;INTERVAL=1;UNTIL=20261231T235959Z;BYDAY=FR",
      serialized: "FREQ=WEEKLY;INTERVAL=1;BYDAY=FR;UNTIL=20261231T235959Z",
      model: {
        freq: "WEEKLY",
        weeklyDays: ["FR"],
        endMode: "UNTIL",
        until: "2026-12-31",
      },
    },
  ];

  it.each(cases)("reads $name into the model", ({ rule, model: expected }) => {
    expect(parseRecurrence(rule)).toEqual(model(expected));
  });

  it.each(cases)(
    "writes $name to the exact rule string",
    ({ rule, serialized, model: m }) => {
      expect(serializeRecurrence(model(m))).toBe(serialized ?? rule);
    }
  );

  it.each(cases)("round trips $name model → rule → model", ({ model: m }) => {
    expect(parseRecurrence(serializeRecurrence(model(m)))).toEqual(model(m));
  });

  it("tolerates the RRULE: prefix the backend strips", () => {
    expect(parseRecurrence("RRULE:FREQ=DAILY;INTERVAL=1").freq).toBe("DAILY");
  });

  it("reads an empty, FREQ-less or hourly rule as no recurrence", () => {
    expect(parseRecurrence(null)).toEqual(emptyRecurrence());
    expect(parseRecurrence("")).toEqual(emptyRecurrence());
    expect(parseRecurrence("INTERVAL=2")).toEqual(emptyRecurrence());
    expect(parseRecurrence("FREQ=HOURLY;INTERVAL=1").freq).toBeNull();
  });

  it("writes the empty string for no recurrence", () => {
    expect(serializeRecurrence(emptyRecurrence())).toBe("");
  });

  it("clamps an out-of-range BYSETPOS to the five the UI offers", () => {
    // The dropdown only has first..fourth and last; a stored BYSETPOS=2 stays second, a 5 clamps to fourth.
    expect(
      parseRecurrence("FREQ=MONTHLY;INTERVAL=1;BYSETPOS=2;BYDAY=MO").which
    ).toBe(2);
    expect(
      parseRecurrence("FREQ=MONTHLY;INTERVAL=1;BYSETPOS=5;BYDAY=MO").which
    ).toBe(4);
  });

  it("omits BYDAY when a weekly rule has no weekday picked", () => {
    // No day toggled: the rule falls back to the event's own weekday, so BYDAY must be absent.
    expect(serializeRecurrence(model({ freq: "WEEKLY", weeklyDays: [] }))).toBe(
      "FREQ=WEEKLY;INTERVAL=1"
    );
  });
});

describe("weekday group mapping", () => {
  it("expands a group to its BYDAY indices", () => {
    expect(onTheDayToIndices("DAY")).toEqual([0, 1, 2, 3, 4, 5, 6]);
    expect(onTheDayToIndices("WEEKDAY")).toEqual([0, 1, 2, 3, 4]);
    expect(onTheDayToIndices("WEEKENDDAY")).toEqual([5, 6]);
    expect(onTheDayToIndices("WE")).toEqual([2]);
  });

  it("reads BYDAY indices back to a group or single weekday", () => {
    expect(indicesToOnTheDay([0, 1, 2, 3, 4, 5, 6])).toBe("DAY");
    expect(indicesToOnTheDay([0, 1, 2, 3, 4])).toBe("WEEKDAY");
    expect(indicesToOnTheDay([5, 6])).toBe("WEEKENDDAY");
    expect(indicesToOnTheDay([3])).toBe("TH");
  });
});

describe("recurrenceMode", () => {
  it("is none without a frequency", () => {
    expect(recurrenceMode(null)).toBe("NONE");
    expect(recurrenceMode("INTERVAL=2")).toBe("NONE");
    expect(recurrenceMode("FREQ=HOURLY")).toBe("NONE");
  });

  it("is the plain frequency for a bare rule with interval 1", () => {
    expect(recurrenceMode("FREQ=WEEKLY;INTERVAL=1")).toBe("WEEKLY");
    expect(recurrenceMode("FREQ=DAILY")).toBe("DAILY");
  });

  it("is customized once an interval, a BY-field, a count or an until is set", () => {
    expect(recurrenceMode("FREQ=DAILY;INTERVAL=2")).toBe("CUSTOMIZED");
    expect(recurrenceMode("FREQ=WEEKLY;INTERVAL=1;BYDAY=MO")).toBe(
      "CUSTOMIZED"
    );
    expect(recurrenceMode("FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=1")).toBe(
      "CUSTOMIZED"
    );
    expect(recurrenceMode("FREQ=DAILY;INTERVAL=1;COUNT=5")).toBe("CUSTOMIZED");
    expect(recurrenceMode("FREQ=DAILY;INTERVAL=1;UNTIL=20261231T235959Z")).toBe(
      "CUSTOMIZED"
    );
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
