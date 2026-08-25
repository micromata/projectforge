import { afterEach, describe, expect, it, vi } from "vitest";
import type { FormatContext } from "@/lib/format";
import { periodKindsOf } from "@/lib/date-period";
import {
  editedDateValue,
  editedInstantValue,
  pageablePeriodOf,
  steppedPeriodValue,
} from "./filter-period";

const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
// The two arts the invoice list offers; `month` snaps, `yearToDate` moves with the calendar.
const kinds = periodKindsOf(["month", "yearToDate"]);

describe("pageablePeriodOf", () => {
  it("names the art a stored periodKind carries", () => {
    const period = pageablePeriodOf(
      { from: "2026-05-01", to: "2026-05-31", periodKind: "month" },
      kinds,
      berlin
    );
    expect(period?.kind.id).toBe("month");
    expect(period?.anchor).toBe("2026-05-01");
  });

  it("infers the art from a range that is a whole period", () => {
    // A calendar month typed by hand, no periodKind stored — still pageable.
    expect(
      pageablePeriodOf({ from: "2026-05-01", to: "2026-05-31" }, kinds, berlin)
        ?.kind.id
    ).toBe("month");
  });

  it("is null for a range that is no period", () => {
    expect(
      pageablePeriodOf({ from: "2026-05-03", to: "2026-05-20" }, kinds, berlin)
    ).toBeNull();
    expect(pageablePeriodOf(undefined, kinds, berlin)).toBeNull();
    // A half-open range is not a period, however it is written.
    expect(pageablePeriodOf({ from: "2026-05-01" }, kinds, berlin)).toBeNull();
  });
});

describe("steppedPeriodValue", () => {
  const may = { from: "2026-05-01", to: "2026-05-31", periodKind: "month" };

  it("pages a month forward and keeps the art on the value", () => {
    expect(steppedPeriodValue(may, 1, kinds, berlin)).toEqual({
      from: "2026-06-01",
      to: "2026-06-30",
      periodKind: "month",
    });
  });

  it("pages a month back", () => {
    expect(steppedPeriodValue(may, -1, kinds, berlin)).toEqual({
      from: "2026-04-01",
      to: "2026-04-30",
      periodKind: "month",
    });
  });

  it("stamps the inferred art onto a hand-typed whole month", () => {
    expect(
      steppedPeriodValue(
        { from: "2026-05-01", to: "2026-05-31" },
        1,
        kinds,
        berlin
      )
    ).toEqual({ from: "2026-06-01", to: "2026-06-30", periodKind: "month" });
  });

  it("is null when there is nothing to page", () => {
    expect(
      steppedPeriodValue(
        { from: "2026-05-03", to: "2026-05-20" },
        1,
        kinds,
        berlin
      )
    ).toBeNull();
  });
});

describe("editedDateValue", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  // All the arts an invoice list can offer, so a begin edit finds whichever one is in effect.
  const all = periodKindsOf([
    "month",
    "termThreeMonths",
    "termYear",
    "yearToDate",
  ]);

  it("drags a term's end along a begin typed by hand, keeping the art", () => {
    // termYear from 15.03. ends on 14.03. a year on; the begin stays where it was typed.
    expect(
      editedDateValue(
        { from: "2026-05-01", to: "2027-04-30", periodKind: "termYear" },
        "from",
        "2026-03-15",
        all,
        berlin
      )
    ).toEqual({
      from: "2026-03-15",
      to: "2027-03-14",
      periodKind: "termYear",
    });
  });

  it("snaps a calendar month's begin to the first and the end to the last", () => {
    expect(
      editedDateValue(
        { from: "2026-05-01", to: "2026-05-31", periodKind: "month" },
        "from",
        "2026-03-15",
        all,
        berlin
      )
    ).toEqual({
      from: "2026-03-01",
      to: "2026-03-31",
      periodKind: "month",
    });
  });

  it("keeps 'Jahr bis heute' ending today when its begin is typed", () => {
    vi.useFakeTimers().setSystemTime(new Date("2026-08-22T10:00:00.000Z"));
    expect(
      editedDateValue(
        { from: "2025-11-01", to: "2026-08-22", periodKind: "yearToDate" },
        "from",
        "2026-03-15",
        all,
        berlin
      )
    ).toEqual({
      from: "2026-03-15",
      to: "2026-08-22",
      periodKind: "yearToDate",
    });
  });

  it("dissolves the art when the end is typed by hand", () => {
    expect(
      editedDateValue(
        { from: "2026-05-01", to: "2026-05-31", periodKind: "month" },
        "to",
        "2026-06-15",
        all,
        berlin
      )
    ).toEqual({ from: "2026-05-01", to: "2026-06-15", periodKind: undefined });
  });

  it("dissolves the art when the begin is cleared", () => {
    expect(
      editedDateValue(
        { from: "2026-05-01", to: "2026-05-31", periodKind: "month" },
        "from",
        null,
        all,
        berlin
      )
    ).toEqual({ from: undefined, to: "2026-05-31", periodKind: undefined });
  });

  it("is undefined once no bound is left", () => {
    expect(
      editedDateValue(
        { from: "2026-05-01", periodKind: "month" },
        "from",
        null,
        all,
        berlin
      )
    ).toBeUndefined();
  });
});

describe("editedInstantValue", () => {
  const all = periodKindsOf(["month", "termYear", "yearToDate"]);

  it("re-anchors a period on the date of a typed begin instant, keeping the art", () => {
    const edited = editedInstantValue(
      {
        from: "2026-05-01T00:00:00.000+02:00",
        to: "2026-05-31T23:59:00.000+02:00",
        periodKind: "month",
      },
      "from",
      "2026-03-15T09:00:00.000+01:00",
      all,
      berlin
    );
    expect(edited?.periodKind).toBe("month");
    // A whole month becomes 00:00 of its first day until 23:59 of its last in the zone, carried as UTC:
    // 01.03. 00:00 +01:00 is 28.02. 23:00Z, and 31.03. 23:59 +02:00 (after the DST switch) is 21:59Z.
    expect(edited?.from).toBe("2026-02-28T23:00:00.000Z");
    expect(edited?.to).toBe("2026-03-31T21:59:00.000Z");
  });

  it("dissolves the art when the end is typed by hand", () => {
    expect(
      editedInstantValue(
        {
          from: "2026-05-01T00:00:00.000+02:00",
          to: "2026-05-31T23:59:00.000+02:00",
          periodKind: "month",
        },
        "to",
        "2026-06-15T23:59:00.000+02:00",
        all,
        berlin
      )?.periodKind
    ).toBeUndefined();
  });
});
