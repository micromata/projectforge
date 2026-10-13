"use client";

import { useEffect, useRef } from "react";
import { useStore } from "@tanstack/react-form";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { fetchVacationStats } from "@/lib/rs/vacation";
import type { VacationValues } from "./vacation-schema";
import type { VacationDetail, VacationFormStats } from "./types";

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
 * React page's watch fields. Until the first answer the loaded entry's values (computed when it was read)
 * stand in.
 *
 * Debounced, because a date is typed rather than picked as often as not.
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
  const key = useDebouncedValue(values, 300);
  const query = useQuery({
    queryKey: ["vacation", "stats", key],
    queryFn: ({ signal }) => fetchVacationStats(JSON.parse(key), signal),
    placeholderData: keepPreviousData,
    // The answer is a pure function of what was sent (and of the stored vacations, which a save refreshes).
    staleTime: 30_000,
  });
  return query.data ?? data;
}

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
