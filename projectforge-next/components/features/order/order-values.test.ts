import { describe, expect, it } from "vitest";
import {
  emptyScheduleValues,
  nextPositionNumber,
  nextScheduleNumber,
} from "./order-values";
import type { PaymentScheduleValues } from "./order-schema";

/** A row as the form holds it — only what the numbering looks at is given. */
function schedule(
  values: Partial<PaymentScheduleValues>
): PaymentScheduleValues {
  return { ...emptyScheduleValues(0), ...values };
}

describe("nextScheduleNumber", () => {
  it("starts at 1 for an order without a payment schedule", () => {
    expect(nextScheduleNumber([])).toBe(1);
  });

  it("counts a deleted row: its number stays taken in the database", () => {
    expect(
      nextScheduleNumber([
        schedule({ id: 1, number: 1, deleted: true }),
        schedule({ id: 2, number: 2 }),
      ])
    ).toBe(3);
  });

  it("keeps a gap left by earlier deletions rather than filling it", () => {
    expect(
      nextScheduleNumber([
        schedule({ id: 1, number: 1 }),
        schedule({ id: 5, number: 5 }),
      ])
    ).toBe(6);
  });

  it("counts a row added in this form, so two new rows do not collide", () => {
    const rows = [schedule({ id: 3, number: 3 })];
    const added = emptyScheduleValues(nextScheduleNumber(rows));
    expect(added.number).toBe(4);
    expect(nextScheduleNumber([...rows, added])).toBe(5);
  });

  it("ignores a row that carries no number at all", () => {
    expect(nextScheduleNumber([schedule({ number: null })])).toBe(1);
  });
});

describe("nextPositionNumber", () => {
  it("follows the same rule as the payment schedule", () => {
    expect(nextPositionNumber([])).toBe(1);
    expect(
      nextPositionNumber([
        { id: 1, number: 1, deleted: true } as never,
        { id: 2, number: 4 } as never,
      ])
    ).toBe(5);
  });
});
