import { describe, expect, it } from "vitest";
import { invoiceStatisticsEntries } from "./invoice-statistics";

/** The three entries every line carries, whatever the numbers are. */
const ALWAYS = [
  "fibu.common.brutto",
  "fibu.common.netto",
  "fibu.rechnung.offen",
];

describe("invoiceStatisticsEntries", () => {
  it("has nothing to show without statistics", () => {
    expect(invoiceStatisticsEntries(undefined)).toEqual([]);
  });

  it("keeps the three sums a reader looks for over a filter that matched nothing", () => {
    const entries = invoiceStatisticsEntries({
      counter: 0,
      brutto: 0,
      netto: 0,
      open: 0,
    });
    expect(entries.map((entry) => entry.labelKey)).toEqual(ALWAYS);
  });

  it("shows the discounted gross sum only where it differs from the gross sum", () => {
    const same = invoiceStatisticsEntries({
      brutto: 1190,
      bruttoWithDiscount: 1190,
      netto: 1000,
    });
    expect(same.map((entry) => entry.labelKey)).toEqual(ALWAYS);

    const differing = invoiceStatisticsEntries({
      brutto: 1190,
      bruttoWithDiscount: 1150,
      netto: 1000,
    });
    expect(differing.map((entry) => entry.labelKey)).toEqual([
      "fibu.common.brutto",
      "fibu.rechnung.mitSkonto",
      "fibu.common.netto",
      "fibu.rechnung.offen",
    ]);
  });

  it("drops what is zero and only looked for when there is something to look for", () => {
    const entries = invoiceStatisticsEntries({
      brutto: 1190,
      netto: 1000,
      open: 0,
      overdue: 0,
      discount: 0,
      paymentTargetAverage: 0,
      actualPaymentTargetAverage: 0,
    });
    expect(entries.map((entry) => entry.labelKey)).toEqual(ALWAYS);
  });

  it("appends the optional entries in the order of the Wicket panel", () => {
    const entries = invoiceStatisticsEntries({
      brutto: 1190,
      netto: 1000,
      open: 400,
      overdue: 200,
      discount: 40,
      paymentTargetAverage: 30,
      actualPaymentTargetAverage: 42,
    });
    expect(entries.map((entry) => entry.labelKey)).toEqual([
      ...ALWAYS,
      "fibu.rechnung.filter.ueberfaellig",
      "fibu.rechnung.skonto",
      "fibu.rechnung.zahlungsZiel",
      "fibu.rechnung.zahlungsZiel.actual",
    ]);
  });

  it("colours the two entries the Wicket page emphasizes, and reads the averages as days", () => {
    const entries = invoiceStatisticsEntries({
      brutto: 1190,
      netto: 1000,
      open: 400,
      overdue: 200,
      paymentTargetAverage: 30,
    });
    const byKey = new Map(entries.map((entry) => [entry.labelKey, entry]));
    expect(byKey.get("fibu.rechnung.offen")?.tone).toBe("open");
    expect(byKey.get("fibu.rechnung.filter.ueberfaellig")?.tone).toBe(
      "overdue"
    );
    expect(byKey.get("fibu.common.brutto")?.tone).toBe("plain");
    expect(byKey.get("fibu.rechnung.zahlungsZiel")?.kind).toBe("days");
    expect(byKey.get("fibu.common.netto")?.kind).toBe("currency");
  });
});
