"use client";

import { useDebouncedStats } from "@/components/shared/form/use-debounced-stats";
import { fetchAvailabilityStats } from "@/lib/rs/availability";
import type { AvailabilityValues } from "./availability-schema";
import type { AvailabilityDetail, AvailabilityFormStats } from "./types";
import { toFormValues } from "./availability-values";

/** What the answer depends on: the user (no substitute of the own entry), the period and the substitutes. */
function statsInput(values: AvailabilityValues) {
  return {
    id: values.id,
    user: values.user ? { id: values.user.id } : null,
    startDate: values.startDate,
    endDate: values.endDate,
    substitutes: values.substitutes.map((u) => ({ id: u.id })),
  };
}

/**
 * The substitutes' absences and whether a day is left without any of them on duty, as the server computes them for
 * the form while it is being edited (`/rs/availability/stats`, see useDebouncedStats).
 */
export function useAvailabilityStats(): AvailabilityFormStats | undefined {
  return useDebouncedStats<
    AvailabilityDetail,
    AvailabilityValues,
    AvailabilityFormStats
  >({
    entity: "availability",
    input: statsInput,
    toFormValues,
    fetch: fetchAvailabilityStats,
    unknown: UNKNOWN_STATS,
  });
}

const UNKNOWN_STATS: AvailabilityFormStats = {
  conflict: null,
  substituteVacations: null,
  substituteAvailabilities: null,
  chartDays: null,
  unavailable: true,
};
