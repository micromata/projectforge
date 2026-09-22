import { describe, expect, it } from "vitest";
import {
  emptyKostZuweisungValues,
  emptyPositionValues,
  nextKostZuweisungIndex,
  referencedOrders,
  remainingNet,
  shareOfNetSum,
} from "./invoice-values";
import type {
  InvoicePositionValues,
  KostZuweisungValues,
} from "./invoice-schema";

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

  it("falls back to the project's first cost unit only on the first row", () => {
    const projectKost2 = { id: 42 };
    // The first row of a position has no predecessor, so the project decides
    // (`RechnungCostEditTablePanel.newKostZuweisung`).
    expect(
      emptyKostZuweisungValues(0, undefined, null, projectKost2).kost2
    ).toEqual(projectKost2);
    // From then on the row above wins: a split that was moved to another cost unit stays there instead of
    // snapping back to the project's default on every added row.
    const predecessor = assignment({ id: 1, index: 0, kost2: { id: 7 } });
    expect(
      emptyKostZuweisungValues(1, predecessor, null, projectKost2).kost2
    ).toEqual({ id: 7 });
  });
});

describe("emptyPositionValues", () => {
  it("takes the VAT rate from the row above, and the configuration only without one", () => {
    // Wicket presets the configured rate on the first position only
    // (`AbstractRechnungEditForm.refreshPositions`); carrying the predecessor's over is kept on purpose.
    expect(emptyPositionValues(1, undefined, 0.19).vat).toBe(0.19);
    const predecessor = emptyPositionValues(1, undefined, 0.19);
    predecessor.vat = 0.07;
    expect(emptyPositionValues(2, predecessor, 0.19).vat).toBe(0.07);
    // No configured rate is no error: the field starts empty, as it did before there was a default.
    expect(emptyPositionValues(1, undefined, null).vat).toBeNull();
    expect(emptyPositionValues(1).vat).toBeNull();
  });
});

describe("referencedOrders", () => {
  /** A position as the form holds it — only the order reference and the deleted flag matter here. */
  function position(
    auftrag: {
      auftragId?: number | null;
      auftragNummer?: number | null;
    } | null,
    deleted = false
  ): InvoicePositionValues {
    const pos = emptyPositionValues(1);
    pos.deleted = deleted;
    pos.auftragsPosition = auftrag;
    return pos;
  }

  it("reduces the positions to each order once, sorted by number", () => {
    const orders = referencedOrders([
      position({ auftragId: 5, auftragNummer: 200 }),
      position({ auftragId: 3, auftragNummer: 100 }),
      // A second position of the order already seen — the order appears once.
      position({ auftragId: 5, auftragNummer: 200 }),
    ]);
    expect(orders).toEqual([
      { id: 3, nummer: 100 },
      { id: 5, nummer: 200 },
    ]);
  });

  it("skips deleted positions and those without a resolved order id", () => {
    expect(
      referencedOrders([
        position({ auftragId: 5, auftragNummer: 200 }, true),
        position({ auftragId: null, auftragNummer: 200 }),
        position(null),
      ])
    ).toEqual([]);
    expect(referencedOrders(undefined)).toEqual([]);
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
