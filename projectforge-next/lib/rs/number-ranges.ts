/**
 * The configured number ranges of customers and projects (`NumberRangeRest`, `/rs/numberRanges`), which
 * shape the number fields of the customer, project and cost 2 forms. See lib/number-ranges.ts for the
 * arithmetic.
 */

import { useQuery } from "@tanstack/react-query";
import { request } from "./client";

/** One number range (`NumberRangeRest.Range`, configured as `projectforge.fibu.number-ranges.ranges[n]`). */
export interface NumberRange {
  /** First digit of the project numbers and Kost2 of the range. */
  prefix: number;
  name?: string | null;
  /** Absent for a range without customers (e.g. internal projects). */
  customerMin?: number | null;
  customerMax?: number | null;
  /** Digits of the local project number (per customer, or of the whole range without customers). */
  projectDigits: number;
  /** Segments of the 8-digit Kost2, e.g. [1, 4, 1, 2] for `1.####.#.##`. */
  segments: number[];
  /** The next free customer number of the range (only with customer access). */
  nextFreeCustomerNumber?: number | null;
}

/**
 * Which ranges apply (`NumberRangeService.Mode`): the configured ones, the former ranges 4/5 of an
 * installation that used them (no ranges configured), or none (free numbers).
 */
export type NumberRangeMode = "CONFIGURED" | "LEGACY" | "FREE";

export interface NumberRangesInfo {
  /** Whether cost units (Kost2) are configured at all. */
  costConfigured: boolean;
  mode: NumberRangeMode;
  /** Whether customer numbers outside the ranges are allowed. */
  allowFreeNumbers: boolean;
  /** Segments of Kost2 whose prefix belongs to no range. */
  defaultSegments: number[];
  ranges: NumberRange[];
}

export const NUMBER_RANGES_QUERY_KEY = ["numberRanges"] as const;

export function fetchNumberRanges(
  signal?: AbortSignal
): Promise<NumberRangesInfo> {
  return request<NumberRangesInfo>(
    "/rs/numberRanges",
    { method: "GET" },
    signal
  );
}

/**
 * The number ranges; undefined while loading. Configuration only, so fetched once per session — except with
 * `fresh`, for the next free customer numbers, which every saved customer changes: then fetched again on
 * mount (the cached ones are shown meanwhile).
 */
export function useNumberRanges({
  fresh = false,
}: { fresh?: boolean } = {}): NumberRangesInfo | undefined {
  const { data } = useQuery({
    queryKey: NUMBER_RANGES_QUERY_KEY,
    queryFn: ({ signal }) => fetchNumberRanges(signal),
    staleTime: Infinity,
    refetchOnMount: fresh ? "always" : true,
  });
  return data;
}
