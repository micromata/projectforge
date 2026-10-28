/**
 * The arithmetic of customer, project and Kost2 numbers by the configured number ranges — the counterpart
 * of `NumberRanges.kt`, so the forms can show a number while it is typed. The backend stays the authority
 * (`ProjektDao.assignProjektnummer`).
 *
 * A project of a range has a 6-digit project number:
 * `prefix·10⁵ + (customer mod 10^(5 − projectDigits))·10^projectDigits + local` (without customers:
 * `prefix·10⁵ + local`); its Kost2 are `projectNumber·100 + Kost2 type`. The customer number itself is
 * part of it (its last digits), whatever `customerMin` is: customer 10001 is "1.0001", 123 is "5.123".
 */

import type { NumberRange, NumberRangesInfo } from "@/lib/rs/number-ranges";

const PREFIX_FACTOR = 100_000;

export function hasCustomers(range: NumberRange): boolean {
  return range.customerMin != null && range.customerMax != null;
}

/** Highest local project number of the range, e.g. 99 for 2 digits. */
export function maxLocalNumber(range: NumberRange): number {
  return 10 ** range.projectDigits - 1;
}

export function rangeForCustomer(
  info: NumberRangesInfo | undefined,
  customerNumber: number | null | undefined
): NumberRange | undefined {
  if (info == null || customerNumber == null) return undefined;
  return info.ranges.find(
    (r) =>
      hasCustomers(r) &&
      customerNumber >= r.customerMin! &&
      customerNumber <= r.customerMax!
  );
}

/** The range without customers, if exactly one is configured (e.g. internal projects). */
export function rangeWithoutCustomers(
  info: NumberRangesInfo | undefined
): NumberRange | undefined {
  const ranges = info?.ranges.filter((r) => !hasCustomers(r)) ?? [];
  return ranges.length === 1 ? ranges[0] : undefined;
}

export function rangeForProjectNumber(
  info: NumberRangesInfo | undefined,
  projectNumber: number | null | undefined
): NumberRange | undefined {
  if (info == null || projectNumber == null) return undefined;
  return info.ranges.find(
    (r) =>
      projectNumber >= r.prefix * PREFIX_FACTOR &&
      projectNumber < (r.prefix + 1) * PREFIX_FACTOR
  );
}

/** The project number of a local number, null if it doesn't fit the range (or the customer doesn't). */
export function projectNumber(
  range: NumberRange,
  customerNumber: number | null,
  localNumber: number | null
): number | null {
  if (localNumber == null || localNumber < 0) return null;
  if (localNumber > maxLocalNumber(range)) return null;
  const first = range.prefix * PREFIX_FACTOR;
  if (!hasCustomers(range)) return first + localNumber;
  if (
    customerNumber == null ||
    customerNumber < range.customerMin! ||
    customerNumber > range.customerMax!
  ) {
    return null;
  }
  const customerPart = customerNumber % 10 ** (5 - range.projectDigits);
  return first + customerPart * 10 ** range.projectDigits + localNumber;
}

function segmentsForPrefix(
  info: NumberRangesInfo | undefined,
  prefix: number
): number[] {
  return (
    info?.ranges.find((r) => r.prefix === prefix)?.segments ??
    info?.defaultSegments ?? [1, 3, 2, 2]
  );
}

function split(digits: string, segments: number[]): string[] {
  let pos = 0;
  return segments.map((len) => {
    const part = digits.substring(pos, pos + len);
    pos += len;
    return part;
  });
}

/** E.g. "1.2345.6.78" for 12345678 and segments 1,4,1,2; the Kost2 type may be "##" (still unknown). */
export function formatKost2Digits(
  info: NumberRangesInfo | undefined,
  digits: string
): string {
  if (digits.length !== 8) return digits;
  return split(digits, segmentsForPrefix(info, Number(digits[0]))).join(".");
}

/**
 * A Kost2 given in the positional storage format of Kost2DO (1/3/2/2 digits), segmented by its range.
 * Null parts are shown as "?" so the number keeps its shape while typed.
 */
export function formatKost2(
  info: NumberRangesInfo | undefined,
  nummernkreis: number | null,
  bereich: number | null,
  teilbereich: number | null,
  kost2Art: number | null
): string {
  const pad = (v: number | null, len: number) =>
    v == null ? "?".repeat(len) : String(v).padStart(len, "0");
  const digits =
    pad(nummernkreis, 1) +
    pad(bereich, 3) +
    pad(teilbereich, 2) +
    pad(kost2Art, 2);
  if (nummernkreis == null || digits.length !== 8) {
    return [
      pad(nummernkreis, 1),
      pad(bereich, 3),
      pad(teilbereich, 2),
      pad(kost2Art, 2),
    ].join(".");
  }
  return split(digits, segmentsForPrefix(info, nummernkreis)).join(".");
}

/** E.g. "5.123.45" or "1.2345.6" (the Kost2 without its type); free project numbers unchanged. */
export function formatProjectNumber(
  info: NumberRangesInfo | undefined,
  projectNumber: number
): string {
  const range = rangeForProjectNumber(info, projectNumber);
  if (!range) return String(projectNumber);
  return split(String(projectNumber), range.segments.slice(0, -1)).join(".");
}

/** E.g. "5.123" or "1.2345" (the first two Kost2 segments of its projects); free numbers unchanged. */
export function formatCustomerNumber(
  info: NumberRangesInfo | undefined,
  customerNumber: number
): string {
  const range = rangeForCustomer(info, customerNumber);
  if (!range) return String(customerNumber);
  const number = projectNumber(range, customerNumber, 0)!;
  return formatProjectNumber(info, number).split(".").slice(0, 2).join(".");
}
