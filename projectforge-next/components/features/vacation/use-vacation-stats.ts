"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useStore } from "@tanstack/react-form";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { fetchVacationStats } from "@/lib/rs/vacation";
import type { VacationValues } from "./vacation-schema";
import type { VacationDetail, VacationFormStats } from "./types";
import { toFormValues } from "./vacation-values";

/**
 * What the answer depends on: the period with its half days for the working days, the employee and the
 * year of the start for the days left, the replacements for the conflicts, and manager, status and special
 * leave for the statuses offered. The comment cannot move any of it.
 */
function statsInput(values: VacationValues) {
  return {
    id: values.id,
    employee: values.employee ? { id: values.employee.id } : null,
    startDate: values.startDate,
    endDate: values.endDate,
    halfDayBegin: values.halfDayBegin,
    halfDayEnd: values.halfDayEnd,
    manager: values.manager ? { id: values.manager.id } : null,
    replacement: values.replacement ? { id: values.replacement.id } : null,
    otherReplacements: values.otherReplacements.map((e) => ({ id: e.id })),
    status: values.status,
    special: values.special,
  };
}

/**
 * The working days, the days left, the conflicts with the replacements and the statuses offered, as the
 * server computes them for the form while it is being edited (`/rs/vacation/stats`) — the successor of the
 * React page's watch fields. As long as the form holds what was loaded, the loaded entry's values stand in:
 * the backend computed them with it (`transformFromDB`, `newBaseDTO`).
 *
 * Debounced, because a date is typed rather than picked as often as not. Nothing is asked before the first
 * delay has passed: the fields render while the form still holds its blank defaults, which it is reset from
 * onto the loaded entry only afterwards — a request for that blank state would answer empty values.
 */
export function useVacationStats(): VacationFormStats | undefined {
  const form = useEntityEditForm();
  const data = useEntityData<VacationDetail>();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => JSON.stringify(statsInput(state.values as VacationValues))
  ) as string;
  // The serialised slice is what is debounced and what keys the query: the selector builds a new object on
  // every render, so an object identity would never settle and the debounce would never fire.
  const [key, setKey] = useState<string | null>(null);
  useEffect(() => {
    const timer = setTimeout(() => setKey(values), 300);
    return () => clearTimeout(timer);
  }, [values]);
  const loadedKey = useMemo(
    () => (data ? JSON.stringify(statsInput(toFormValues(data))) : null),
    [data]
  );
  const asLoaded = key == null || key === loadedKey;
  const query = useQuery({
    queryKey: ["vacation", "stats", key],
    queryFn: ({ signal }) => fetchVacationStats(JSON.parse(key!), signal),
    enabled: !asLoaded,
    placeholderData: keepPreviousData,
    // The answer is a pure function of what was sent (and of the stored vacations, which a save refreshes).
    staleTime: 30_000,
  });
  if (asLoaded) return data;
  // A failed request leaves nothing computed for what the form holds now: blank rather than the stored entry's
  // (or the previous answer's) values, which would read as current. The statuses stay the stored entry's; the
  // backend checks the one chosen on save anyway.
  if (query.isError) return { ...data, ...UNKNOWN_STATS };
  return query.data ?? data;
}

const UNKNOWN_STATS: VacationFormStats = {
  workingDays: null,
  workingDaysFormatted: null,
  vacationDaysLeftInYear: null,
  vacationDaysLeftInYearString: null,
  vacationDaysLeftAfterThis: null,
  vacationDaysLeftAfterThisString: null,
  conflictingVacations: null,
  conflict: null,
  chartDays: null,
};

/**
 * Keeps the period a period, as the React form did: a start after the end (or without one) moves the end
 * onto it, an end before the start (or without one) moves the start. Only the end the user did *not* just
 * change is touched.
 */
export function useVacationPeriodClamp() {
  const form = useEntityEditForm();
  const { startDate, endDate } = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => {
      const v = state.values as VacationValues;
      return { startDate: v.startDate, endDate: v.endDate };
    }
  );
  const previous = useRef({ startDate, endDate });
  useEffect(() => {
    const before = previous.current;
    previous.current = { startDate, endDate };
    // ISO dates (yyyy-MM-dd) compare as strings.
    if (startDate && startDate !== before.startDate) {
      if (!endDate || endDate < startDate) {
        form.setFieldValue("endDate", startDate);
      }
    } else if (endDate && endDate !== before.endDate) {
      if (!startDate || endDate < startDate) {
        form.setFieldValue("startDate", endDate);
      }
    }
  }, [startDate, endDate, form]);
}
