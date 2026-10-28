"use client";

import { useDebouncedStats } from "@/components/shared/form/use-debounced-stats";
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
 * server computes them for the form while it is being edited (`/rs/vacation/stats`, see useDebouncedStats) — the
 * successor of the React page's watch fields. A failed request leaves nothing computed for what the form holds now;
 * the statuses stay the stored entry's, the backend checks the one chosen on save anyway.
 */
export function useVacationStats(): VacationFormStats | undefined {
  return useDebouncedStats<VacationDetail, VacationValues, VacationFormStats>({
    entity: "vacation",
    input: statsInput,
    toFormValues,
    fetch: fetchVacationStats,
    unknown: UNKNOWN_STATS,
  });
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
  replacementAvailabilities: null,
  unavailable: true,
};
