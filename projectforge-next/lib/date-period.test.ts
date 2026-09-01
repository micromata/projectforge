import { describe, expect, it } from "vitest";
import {
  PERIOD_KINDS,
  periodKindOf,
  periodKindsOf,
  TERM_KIND_IDS,
  type PeriodKindId,
} from "./date-period";

/** The vocabulary itself: which kinds there are, and how one is looked up. What each of them computes is in ./date-period-kinds.test.ts. */
describe("PERIOD_KINDS", () => {
  it("offers the calendar week and month, the four terms and the year to date", () => {
    // The order a picker shows its selection in: the calendar week and month first, then the terms
    // rising in length, and "Jahr bis heute" after "Jahr".
    expect(PERIOD_KINDS.map((kind) => kind.id)).toEqual([
      "week",
      "month",
      "termWeek",
      "termMonth",
      "termThreeMonths",
      "termYear",
      "yearToDate",
    ]);
  });

  it("has unique ids", () => {
    const ids = PERIOD_KINDS.map((kind) => kind.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it("names every text as a key of its own, so the i18n scanner finds it", () => {
    for (const kind of PERIOD_KINDS) {
      for (const key of [
        kind.labelKey,
        kind.shortLabelKey,
        kind.tooltipPreviousKey,
        kind.tooltipNextKey,
        // Optional: a term has no current period to jump to.
        ...(kind.tooltipCurrentKey ? [kind.tooltipCurrentKey] : []),
      ]) {
        expect(key, kind.id).toMatch(/^[a-zA-Z0-9]+(\.[a-zA-Z0-9]+)+$/);
      }
    }
  });

  it("lists the terms as the ids a period of performance offers", () => {
    expect(TERM_KIND_IDS).toEqual([
      "termWeek",
      "termMonth",
      "termThreeMonths",
      "termYear",
    ]);
  });
});

describe("periodKindsOf", () => {
  it("resolves the ids named, in the order the kinds are offered in", () => {
    expect(
      periodKindsOf(["termYear", "termWeek"]).map((kind) => kind.id)
    ).toEqual(["termWeek", "termYear"]);
    expect(
      periodKindsOf(["yearToDate", "month"]).map((kind) => kind.id)
    ).toEqual(["month", "yearToDate"]);
  });

  it("offers nothing for an empty list or none at all", () => {
    expect(periodKindsOf([])).toEqual([]);
    expect(periodKindsOf(undefined)).toEqual([]);
  });

  it("drops an id that is no kind", () => {
    // "quarter" is the calendar meaning of three months and does not exist: `UIFilterTimestampElement`
    // cannot request it, and `termThreeMonths` is the other meaning.
    expect(
      periodKindsOf(["quarter" as PeriodKindId, "month"]).map((kind) => kind.id)
    ).toEqual(["month"]);
  });
});

describe("periodKindOf", () => {
  it("finds the kind by its id", () => {
    expect(periodKindOf("termThreeMonths")?.id).toBe("termThreeMonths");
  });

  it("has nothing for no selection", () => {
    expect(periodKindOf(null)).toBeNull();
    expect(periodKindOf(undefined)).toBeNull();
  });

  it("has nothing for a name that is no kind", () => {
    // What a stored filter can carry: the id comes back from the backend as a plain string, and one
    // written by an older version of the app must not resolve to something else.
    expect(periodKindOf("threeMonths")).toBeNull();
    expect(periodKindOf("")).toBeNull();
  });
});
