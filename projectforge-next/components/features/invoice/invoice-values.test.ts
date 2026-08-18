import { describe, expect, it } from "vitest";
import {
  emptyKostZuweisungValues,
  nextKostZuweisungIndex,
  remainingNet,
  shareOfNetSum,
} from "./invoice-values";
import type { KostZuweisungValues } from "./invoice-schema";

/** A row as the form holds it — only what the arithmetic looks at is given. */
function assignment(values: Partial<KostZuweisungValues>): KostZuweisungValues {
  return { ...emptyKostZuweisungValues(0), ...values };
}

describe("remainingNet", () => {
  it("proposes the whole net sum for the first row of a position", () => {
    expect(remainingNet(2000, [])).toBe(2000);
  });

  it("proposes what the rows already there leave over", () => {
    expect(remainingNet(2000, [assignment({ netto: 1500 })])).toBe(500);
  });

  it("leaves a deleted row out, as RechnungCalculator does", () => {
    expect(
      remainingNet(2000, [
        assignment({ id: 1, netto: 1500, deleted: true }),
        assignment({ id: 2, netto: 500 }),
      ])
    ).toBe(1500);
  });

  it("is null once the position adds up: an empty box, not a 0,00 to be cleared", () => {
    expect(remainingNet(2000, [assignment({ netto: 2000 })])).toBeNull();
  });

  it("is null while the sums are still on their way, so nothing wrong is proposed", () => {
    expect(remainingNet(undefined, [])).toBeNull();
    expect(remainingNet(null, [assignment({ netto: 100 })])).toBeNull();
  });

  it("is negative where the position is over-assigned — the row that is one too many", () => {
    expect(remainingNet(2000, [assignment({ netto: 2500 })])).toBe(-500);
  });

  it("rounds to the two digits an amount has, rather than to what floating point produces", () => {
    // 2000 - 1900.1 is 99.90000000000009 in binary floating point.
    expect(remainingNet(2000, [assignment({ netto: 1900.1 })])).toBe(99.9);
    // A position split in thirds: two rows of 666,67 leave 666,66.
    expect(
      remainingNet(2000, [
        assignment({ netto: 666.67 }),
        assignment({ netto: 666.67 }),
      ])
    ).toBe(666.66);
  });

  it("treats a row whose amount is not filled in yet as assigning nothing", () => {
    expect(remainingNet(2000, [assignment({ netto: null })])).toBe(2000);
  });
});

describe("emptyKostZuweisungValues", () => {
  it("carries the proposed amount along with the predecessor's cost units", () => {
    const rows = [
      assignment({ id: 1, index: 0, netto: 1500, kost1: { id: 7 } }),
    ];
    const added = emptyKostZuweisungValues(
      nextKostZuweisungIndex(rows),
      rows[0],
      remainingNet(2000, rows)
    );
    expect(added).toMatchObject({ index: 1, netto: 500, kost1: { id: 7 } });
  });

  it("stays empty where there is nothing to propose", () => {
    expect(emptyKostZuweisungValues(0, undefined, null).netto).toBeNull();
    expect(emptyKostZuweisungValues(0).netto).toBeNull();
  });
});

describe("shareOfNetSum", () => {
  it("is the fraction of the position the row carries", () => {
    expect(shareOfNetSum(500, 2000)).toBe(0.25);
  });

  it("has none where either number is missing or zero — the division stays defined", () => {
    expect(shareOfNetSum(null, 2000)).toBeNull();
    expect(shareOfNetSum(0, 2000)).toBeNull();
    expect(shareOfNetSum(500, undefined)).toBeNull();
    expect(shareOfNetSum(500, 0)).toBeNull();
  });
});
