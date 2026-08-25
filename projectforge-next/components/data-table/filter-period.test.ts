import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import { periodKindsOf } from "@/lib/date-period";
import { pageablePeriodOf, steppedPeriodValue } from "./filter-period";

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
