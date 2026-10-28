import {
  ABSENCE_KINDS,
  absenceKindOfStatus,
  type AbsenceKind,
} from "@/lib/rs/absence-kinds";
import {
  availabilityBarOf,
  barOf,
  type AvailabilityBar,
  type ChartBar,
  type ChartDay,
  type ChartPeriod,
  type ChartRow,
  type PersonRef,
} from "./absence-chart-model";

/**
 * What the team charts show (`org.projectforge.rest.dto.VacationExportView`): the days and a row per user (of the team
 * vacation calendar or the quick view, AvailabilityQuickViewDialog; an employee or not) with their vacations and
 * availability entries cut to the period.
 */
export interface TeamChartView {
  days: ChartDay[];
  rows: {
    user: PersonRef;
    vacations: (ChartPeriod & { id?: number | null })[];
    availabilities?: (Parameters<typeof availabilityBarOf>[2] & {
      id?: number | null;
      status?: string | null;
    })[];
  }[];
}

/**
 * The rows of the team calendar: every user, also those without a vacation in the period. Only the absences of
 * [kinds] (vacations, availability entries by their status): the view of the previous choice may still be shown
 * while the next one loads.
 */
export function teamChartRows(
  view: TeamChartView,
  kinds: readonly AbsenceKind[] = ABSENCE_KINDS
): ChartRow[] {
  const showVacations = kinds.includes("VACATION");
  const shown = (status: string | null | undefined) => {
    const kind = absenceKindOfStatus(status);
    return kind != null && kinds.includes(kind);
  };
  return view.rows.map((row) => ({
    key: String(row.user.id),
    name: row.user.displayName ?? "",
    own: false,
    inactive: row.user,
    bars: (showVacations ? row.vacations : [])
      .map((v, i) =>
        barOf(`${row.user.id}-${v.id ?? i}`, view.days, {
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
        availabilityBarOf(`${row.user.id}-a${a.id ?? i}`, view.days, a)
      )
      .filter((bar): bar is AvailabilityBar => bar != null),
  }));
}
