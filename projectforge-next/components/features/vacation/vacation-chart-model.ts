import {
  ABSENCE_KINDS,
  absenceKindOfStatus,
  type AbsenceKind,
} from "@/lib/rs/absence-kinds";
import {
  availabilityBarOf,
  barOf,
  buildAbsenceChart,
  type AvailabilityBar,
  type ChartBar,
  type ChartRow,
  type PersonRef,
} from "@/components/shared/absence-chart/absence-chart-model";
import type {
  EmployeeRef,
  VacationChartDay,
  VacationDetail,
  VacationExportView,
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

/**
 * The rows of the team calendar: every employee, also those without a vacation in the period. Only the absences of
 * [kinds] (vacations, availability entries by their status): the view of the previous choice may still be shown
 * while the next one loads.
 */
export function teamChartRows(
  view: VacationExportView,
  kinds: readonly AbsenceKind[] = ABSENCE_KINDS
): ChartRow[] {
  const showVacations = kinds.includes("VACATION");
  const shown = (status: string | null | undefined) => {
    const kind = absenceKindOfStatus(status);
    return kind != null && kinds.includes(kind);
  };
  return view.rows.map((row) => ({
    key: String(row.employee.id),
    name: row.employee.displayName ?? "",
    own: false,
    bars: (showVacations ? row.vacations : [])
      .map((v, i) =>
        barOf(`${row.employee.id}-${v.id ?? i}`, view.days, {
          startDate: v.startDate ?? null,
          endDate: v.endDate ?? null,
          halfDayBegin: v.halfDayBegin,
          halfDayEnd: v.halfDayEnd,
          status: v.status ?? null,
        })
      )
      .filter((bar): bar is ChartBar => bar != null),
    availabilities: (row.availabilities ?? [])
      .filter((a) => shown(a.status))
      .map((a, i) =>
        availabilityBarOf(`${row.employee.id}-a${a.id ?? i}`, view.days, a)
      )
      .filter((bar): bar is AvailabilityBar => bar != null),
  }));
}
