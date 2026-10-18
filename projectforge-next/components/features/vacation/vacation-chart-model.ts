import {
  buildAbsenceChart,
  type ChartRow,
  type PersonRef,
} from "@/components/shared/absence-chart/absence-chart-model";
import type {
  EmployeeRef,
  VacationChartDay,
  VacationDetail,
  VacationReplacementAvailability,
  VacationStatus,
} from "./types";

/** What the form holds for the vacation being edited. */
export interface OwnVacation {
  employee: EmployeeRef | null;
  startDate: string | null;
  endDate: string | null;
  halfDayBegin: boolean;
  halfDayEnd: boolean;
  status: VacationStatus | null;
}

/**
 * The rows of the chart: the vacation being edited first, then every substitute — the replacement and the
 * further ones, also those without a vacation (they are on duty) — with their vacations and availability entries
 * overlapping it, keyed by employee.
 */
export function buildVacationChart(
  days: VacationChartDay[],
  own: OwnVacation,
  substitutes: EmployeeRef[],
  vacations: VacationDetail[],
  availabilities: VacationReplacementAvailability[] = []
): ChartRow[] {
  const person = (e: PersonRef | null | undefined) =>
    e ? { key: String(e.id), name: e.displayName ?? "" } : null;
  return buildAbsenceChart(
    days,
    { ...own, name: own.employee?.displayName ?? "" },
    substitutes.map((s) => person(s)!),
    vacations.map((v) => ({ ...v, person: person(v.employee) })),
    availabilities.map((a) => ({ ...a, person: person(a.employee) }))
  );
}
