import { describe, expect, it } from "vitest";
import { orderStatisticsEntries } from "./order-statistics";

describe("orderStatisticsEntries", () => {
  it("has nothing to show without statistics", () => {
    expect(orderStatisticsEntries(undefined)).toEqual([]);
  });

  it("keeps the net sum even over a filter that matched nothing", () => {
    const entries = orderStatisticsEntries({ netSum: 0, counter: 0 });
    expect(entries.map((entry) => entry.labelKey)).toEqual([
      "fibu.common.netto",
    ]);
  });

  it("drops a breakdown line whose counter is 0, as the legacy list hides it", () => {
    const entries = orderStatisticsEntries({
      netSum: 1000,
      counter: 3,
      akquiseSum: 0,
      counterAkquise: 0,
      commissionedSum: 700,
      counterCommissioned: 2,
      toBeInvoicedSum: 300,
      counterToBeInvoiced: 1,
    });
    expect(entries.map((entry) => entry.labelKey)).toEqual([
      "fibu.common.netto",
      "fibu.auftrag.status.beauftragt",
      "fibu.toBeInvoiced",
    ]);
  });

  it("colours the two lines the Wicket page emphasizes", () => {
    const entries = orderStatisticsEntries({
      netSum: 1000,
      counter: 3,
      commissionedSum: 700,
      counterCommissioned: 2,
      toBeInvoicedSum: 300,
      counterToBeInvoiced: 1,
    });
    expect(
      Object.fromEntries(entries.map((entry) => [entry.labelKey, entry.tone]))
    ).toEqual({
      "fibu.common.netto": "plain",
      "fibu.auftrag.status.beauftragt": "commissioned",
      "fibu.toBeInvoiced": "toBeInvoiced",
    });
  });
});
