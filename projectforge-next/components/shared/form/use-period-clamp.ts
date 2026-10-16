"use client";

import { useEffect, useRef } from "react";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "./form-context";

/** The two ends of a period as ISO dates (yyyy-MM-dd), unset ones empty. */
export interface PeriodValues {
  start?: string | null;
  end?: string | null;
}

/**
 * The end to move after a change of [now] against [before], or null: a start set after the end (or without
 * one) moves the end onto it, an end set before the start (or without one) moves the start. Only the end
 * the user did *not* just change is touched.
 */
export function clampPeriod(
  before: PeriodValues,
  now: PeriodValues
): { field: "start" | "end"; value: string } | null {
  const { start, end } = now;
  // ISO dates compare as strings.
  if (start && start !== before.start) {
    return !end || end < start ? { field: "end", value: start } : null;
  }
  if (end && end !== before.end) {
    return !start || end < start ? { field: "start", value: end } : null;
  }
  return null;
}

/**
 * Keeps the period of the entity form a period (see [clampPeriod]), e.g. a start without an end makes a
 * one-day period. Call it once per form, e.g. by rendering [PeriodClamp] as the form's `editIntro`.
 */
export function usePeriodClamp(startName = "startDate", endName = "endDate") {
  const form = useEntityEditForm();
  const start = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values?.[startName] as string | null | undefined
  );
  const end = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values?.[endName] as string | null | undefined
  );
  const previous = useRef<PeriodValues>({ start, end });
  useEffect(() => {
    const change = clampPeriod(previous.current, { start, end });
    previous.current = { start, end };
    if (change) {
      form.setFieldValue(
        change.field === "start" ? startName : endName,
        change.value
      );
    }
  }, [start, end, form, startName, endName]);
}

/** Renders nothing; keeps the form's `startDate`/`endDate` a period (see [usePeriodClamp]). */
export function PeriodClamp() {
  usePeriodClamp();
  return null;
}
