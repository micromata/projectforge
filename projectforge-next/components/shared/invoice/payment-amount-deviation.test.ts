import { describe, expect, it } from "vitest";
import { deviatingGrossSum } from "./payment-amount-deviation";

/** An invoice of 1.000,00 € gross. */
const sums = { grossSum: 1000 };

describe("deviatingGrossSum", () => {
  it("says nothing about the amount the invoice asks for", () => {
    expect(deviatingGrossSum(1000, sums)).toBeNull();
  });

  it("says nothing about a discount taken, which is far inside the tolerance", () => {
    // Two percent, the usual term — and 970,00 € for a three percent one.
    expect(deviatingGrossSum(980, sums)).toBeNull();
    expect(deviatingGrossSum(970, sums)).toBeNull();
  });

  it("says nothing at exactly ten percent off, which is what was allowed", () => {
    expect(deviatingGrossSum(1100, sums)).toBeNull();
    expect(deviatingGrossSum(900, sums)).toBeNull();
  });

  it("names the gross sum once the amount is further off than that", () => {
    expect(deviatingGrossSum(1101, sums)).toBe(1000);
    expect(deviatingGrossSum(899, sums)).toBe(1000);
  });

  it("catches the digit too many and the digit too few", () => {
    expect(deviatingGrossSum(10000, sums)).toBe(1000);
    expect(deviatingGrossSum(100, sums)).toBe(1000);
  });

  it("ignores the gross sum with discount, which is the paid amount itself", () => {
    // What `RechnungCalculator.calculateGrossSumWithDiscount` answers once an amount is entered — read
    // as a yardstick it would silence every warning, which is the defect this pins.
    const answered = { grossSum: 1000, grossSumWithDiscount: 10000 };
    expect(deviatingGrossSum(10000, answered)).toBe(1000);
  });

  it("holds back while there is no amount to judge", () => {
    expect(deviatingGrossSum(null, sums)).toBeNull();
    expect(deviatingGrossSum(undefined, sums)).toBeNull();
    // A cleared box lands on null, but an explicit 0,00 is a state of its own and no keying error.
    expect(deviatingGrossSum(0, sums)).toBeNull();
  });

  it("holds back while the sums are still on their way", () => {
    expect(deviatingGrossSum(1000, undefined)).toBeNull();
    expect(deviatingGrossSum(1000, {})).toBeNull();
  });

  it("holds back where there is no gross sum to compare against", () => {
    // An invoice without positions, and a credit note whose sum is negative: neither is a yardstick.
    expect(deviatingGrossSum(1000, { grossSum: 0 })).toBeNull();
    expect(deviatingGrossSum(-1000, { grossSum: -1000 })).toBeNull();
  });
});
