import type {
  EmployeeRef,
  VacationChartDay,
  VacationDetail,
  VacationExportView,
  VacationStatus,
} from "./types";

/** A vacation on the chart: the columns it covers (indices into the days, both inclusive). */
export interface ChartBar {
  key: string;
  first: number;
  last: number;
  /** Half a day at a visible end: the bar begins or ends in the middle of that day's column. */
  halfFirst: boolean;
  halfLast: boolean;
  status: VacationStatus | null;
  /** A piece of the vacation being edited on days without any substitute on duty. */
  conflict?: boolean;
  /** The whole period, also where the bar is cut off at the edge of the chart. */
  startDate: string;
  endDate: string;
}

/** An availability entry on the team calendar: the columns it covers and what its tooltip says. */
export interface AvailabilityBar {
  key: string;
  first: number;
  last: number;
  /** Type and status, e.g. "Workation (Reachable if needed)". */
  label: string;
  comment: string | null;
  startDate: string;
  endDate: string;
}

export interface ChartRow {
  key: string;
  name: string;
  /** The vacation being edited, the first row. */
  own: boolean;
  bars: ChartBar[];
  /** Only on the team calendar, if asked for. */
  availabilities?: AvailabilityBar[];
}

/** What the form holds for the vacation being edited. */
export interface OwnVacation {
  employee: EmployeeRef | null;
  startDate: string | null;
  endDate: string | null;
  halfDayBegin: boolean;
  halfDayEnd: boolean;
  status: VacationStatus | null;
}

/** A period on the axis, cut off at its edges; null if it lies beside it. ISO dates compare as strings. */
export function barOf(
  key: string,
  days: VacationChartDay[],
  vacation: Pick<
    VacationDetail,
    "startDate" | "endDate" | "halfDayBegin" | "halfDayEnd" | "status"
  >
): ChartBar | null {
  const { startDate, endDate } = vacation;
  if (!startDate || !endDate || days.length === 0) return null;
  const firstDay = days[0].date;
  const lastDay = days[days.length - 1].date;
  if (endDate < firstDay || startDate > lastDay) return null;
  const index = (date: string) => days.findIndex((d) => d.date === date);
  const first = startDate < firstDay ? 0 : index(startDate);
  const last = endDate > lastDay ? days.length - 1 : index(endDate);
  if (first < 0 || last < first) return null;
  // As VacationService counts them: a half day only on a working day, and a single day with both boxes checked
  // is one half day (drawn as its first half; both halves cut off would leave no bar).
  const halfFirst =
    vacation.halfDayBegin === true &&
    startDate >= firstDay &&
    days[first].workingDay;
  const halfLast =
    vacation.halfDayEnd === true &&
    endDate <= lastDay &&
    days[last].workingDay &&
    !(halfFirst && first === last);
  return {
    key,
    first,
    last,
    halfFirst,
    halfLast,
    status: vacation.status ?? null,
    startDate,
    endDate,
  };
}

/**
 * The vacation being edited in pieces, one per run of days that are or aren't covered by a substitute, so the
 * uncovered days stand out. A half day stays with the piece at that end.
 */
export function splitByConflict(
  bar: ChartBar,
  days: VacationChartDay[]
): ChartBar[] {
  const pieces: ChartBar[] = [];
  for (let i = bar.first; i <= bar.last; i++) {
    const conflict = days[i].uncovered === true;
    const piece = pieces[pieces.length - 1];
    if (piece && piece.conflict === conflict) piece.last = i;
    else
      pieces.push({
        ...bar,
        key: `${bar.key}-${i}`,
        first: i,
        last: i,
        conflict,
      });
  }
  return pieces.map((piece, i) => ({
    ...piece,
    halfFirst: i === 0 && bar.halfFirst,
    halfLast: i === pieces.length - 1 && bar.halfLast,
  }));
}

/**
 * The rows of the chart: the vacation being edited first, then every substitute — the replacement and the
 * further ones, also those without a vacation (they are on duty) — with their vacations overlapping it.
 */
export function buildVacationChart(
  days: VacationChartDay[],
  own: OwnVacation,
  substitutes: EmployeeRef[],
  vacations: VacationDetail[]
): ChartRow[] {
  const ownBar = barOf("own", days, own);
  const rows: ChartRow[] = [
    {
      key: "own",
      name: own.employee?.displayName ?? "",
      own: true,
      bars: ownBar ? splitByConflict(ownBar, days) : [],
    },
  ];
  const people = new Map<number, string>();
  for (const s of substitutes) people.set(s.id, s.displayName ?? "");
  // A vacation of somebody not (or no longer) chosen still shows, under its own employee.
  for (const v of vacations) {
    if (v.employee && !people.has(v.employee.id)) {
      people.set(v.employee.id, v.employee.displayName ?? "");
    }
  }
  for (const [id, name] of people) {
    const bars = vacations
      .filter((v) => v.employee?.id === id)
      .map((v, i) => barOf(`${id}-${v.id ?? i}`, days, v))
      .filter((b): b is ChartBar => b != null);
    rows.push({ key: String(id), name, own: false, bars });
  }
  return rows;
}

/** The months on the axis, each with the columns of its days (indices into the days, both inclusive). */
export function monthSpans(
  days: VacationChartDay[]
): { month: string; first: number; last: number }[] {
  const spans: { month: string; first: number; last: number }[] = [];
  days.forEach((day, i) => {
    const month = day.date.slice(0, 7);
    const span = spans[spans.length - 1];
    if (span?.month === month) span.last = i;
    else spans.push({ month, first: i, last: i });
  });
  return spans;
}

/**
 * What a bar of the chart says to a screen reader and in its tooltip: whose vacation, its period and — besides
 * the bar's colour and dashed border — its status, so the chart reads without seeing it.
 *
 * @param formatDate The date as the user writes it.
 * @param statusLabel The status' name, or null for one without a label (the status is left out then).
 */
export function barLabel(
  name: string,
  bar: Pick<ChartBar, "startDate" | "endDate" | "status">,
  formatDate: (iso: string) => string,
  statusLabel: (status: VacationStatus) => string | null
): string {
  const period = `${name}: ${formatDate(bar.startDate)} – ${formatDate(bar.endDate)}`;
  const status = bar.status ? statusLabel(bar.status) : null;
  return status ? `${period} (${status})` : period;
}

/** What an availability bar says to a screen reader and in its tooltip: whose, what, when and the comment. */
export function availabilityLabel(
  name: string,
  bar: Pick<AvailabilityBar, "label" | "comment" | "startDate" | "endDate">,
  formatDate: (iso: string) => string
): string {
  const text = `${name}: ${bar.label}, ${formatDate(bar.startDate)} – ${formatDate(bar.endDate)}`;
  return bar.comment ? `${text} (${bar.comment})` : text;
}

/** The rows of the team calendar: every employee, also those without a vacation in the period. */
export function teamChartRows(view: VacationExportView): ChartRow[] {
  return view.rows.map((row) => ({
    key: String(row.employee.id),
    name: row.employee.displayName ?? "",
    own: false,
    bars: row.vacations
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
    availabilities: (row.availabilities ?? []).flatMap((a, i) => {
      const bar = barOf(`${row.employee.id}-a${a.id ?? i}`, view.days, {
        startDate: a.startDate ?? null,
        endDate: a.endDate ?? null,
        halfDayBegin: false,
        halfDayEnd: false,
        status: null,
      });
      return bar
        ? [
            {
              key: bar.key,
              first: bar.first,
              last: bar.last,
              label: a.label,
              comment: a.comment ?? null,
              startDate: bar.startDate,
              endDate: bar.endDate,
            },
          ]
        : [];
    }),
  }));
}
