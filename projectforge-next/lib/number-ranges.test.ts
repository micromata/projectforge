import { describe, expect, it } from "vitest";
import type { NumberRangesInfo } from "@/lib/rs/number-ranges";
import {
  formatCustomerNumber,
  formatKost2,
  formatProjectNumber,
  maxLocalNumber,
  projectNumber,
  rangeForCustomer,
  rangeForProjectNumber,
  rangeWithoutCustomers,
} from "./number-ranges";

/** The ranges Micromata configures (application-test.properties). */
const INFO: NumberRangesInfo = {
  costConfigured: true,
  mode: "CONFIGURED",
  allowFreeNumbers: false,
  defaultSegments: [1, 3, 2, 2],
  ranges: [
    {
      prefix: 1,
      customerMin: 10000,
      customerMax: 19999,
      projectDigits: 1,
      segments: [1, 4, 1, 2],
    },
    { prefix: 4, name: "Intern", projectDigits: 5, segments: [1, 3, 2, 2] },
    {
      prefix: 5,
      customerMin: 0,
      customerMax: 999,
      projectDigits: 2,
      segments: [1, 3, 2, 2],
    },
  ],
};

describe("number ranges", () => {
  it("takes the customer number itself into the project number, whatever customerMin is", () => {
    const from10001 = {
      prefix: 1,
      customerMin: 10001,
      customerMax: 19999,
      projectDigits: 1,
      segments: [1, 4, 1, 2],
    };
    const from1 = {
      prefix: 5,
      customerMin: 1,
      customerMax: 999,
      projectDigits: 2,
      segments: [1, 3, 2, 2],
    };
    expect(projectNumber(from10001, 10001, 3)).toBe(100013);
    expect(projectNumber(from1, 123, 45)).toBe(512345);
    expect(projectNumber(from10001, 10000, 3)).toBeNull();
  });

  it("finds the range of a customer, a project number and the one without customers", () => {
    expect(rangeForCustomer(INFO, 123)?.prefix).toBe(5);
    expect(rangeForCustomer(INFO, 10007)?.prefix).toBe(1);
    expect(rangeForCustomer(INFO, 4711)).toBeUndefined();
    expect(rangeForProjectNumber(INFO, 412345)?.prefix).toBe(4);
    expect(rangeForProjectNumber(INFO, 42)).toBeUndefined();
    expect(rangeWithoutCustomers(INFO)?.prefix).toBe(4);
  });

  it("computes project numbers within the bounds of the range", () => {
    const [r1, r4, r5] = INFO.ranges;
    expect(maxLocalNumber(r1)).toBe(9);
    expect(projectNumber(r1, 10007, 3)).toBe(100073);
    expect(projectNumber(r1, 10007, 10)).toBeNull();
    expect(projectNumber(r5, 123, 45)).toBe(512345);
    expect(projectNumber(r4, null, 12345)).toBe(412345);
    expect(projectNumber(r5, 1000, 1)).toBeNull();
  });

  it("formats by the segments of the range", () => {
    expect(formatProjectNumber(INFO, 100073)).toBe("1.0007.3");
    expect(formatProjectNumber(INFO, 512345)).toBe("5.123.45");
    expect(formatProjectNumber(INFO, 47110001)).toBe("47110001");
    expect(formatCustomerNumber(INFO, 10007)).toBe("1.0007");
    expect(formatCustomerNumber(INFO, 123)).toBe("5.123");
    expect(formatCustomerNumber(INFO, 4711)).toBe("4711");
    expect(formatKost2(INFO, 1, 0, 73, 2)).toBe("1.0007.3.02");
    expect(formatKost2(INFO, 6, 100, 1, 2)).toBe("6.100.01.02");
    expect(formatKost2(undefined, 5, 123, 45, 67)).toBe("5.123.45.67");
  });
});
