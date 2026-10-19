/**
 * The model of the absence charts (AbsenceChartFrame, AbsenceChartRow): a period being edited — a vacation or an
 * availability entry — and the absences of its substitutes as bars over the days, and the team vacation calendar.
 */

/** A day of the axis (`org.projectforge.rest.dto.ChartDay`). */
export interface ChartDay {
  /** ISO date (yyyy-MM-dd). */
  date: string;
  /** Neither weekend nor holiday. */
  workingDay: boolean;
  holiday?: string | null;
  /** A day of the edited period on which no substitute is on duty. */
  uncovered?: boolean;
}

/** A person as the DTOs refer to one: the id, the name to show. */
export interface PersonRef {
  id: number;
  displayName?: string | null;
}

/**
 * An availability entry of a substitute (`org.projectforge.rest.dto.VacationReplacementAvailability`): who, when and
 * how reachable, as the form of a vacation or an availability entry shows it.
 */
export interface SubstituteAvailability {
  /** The substitute's employee, none for a freelancer. */
  employee?: PersonRef | null;
  /** The substitute (also a freelancer, who has no employee). */
  user?: PersonRef | null;
  startDate?: string | null;
  startDateFormatted?: string | null;
  endDate?: string | null;
  endDateFormatted?: string | null;
  status?: string | null;
  statusString?: string | null;
  /** What the entry is called for the logged-in user: its type only for the substitute and HR staff. */
  label?: string | null;
  /** The public comment. */
  comment?: string | null;
  /** Away (not available), as on vacation. */
  absent?: boolean;
}

/** A period as a bar takes it: a vacation, or an availability entry (no half days, no status). */
export interface ChartPeriod {
  startDate?: string | null;
  endDate?: string | null;
  halfDayBegin?: boolean | null;
  halfDayEnd?: boolean | null;
  status?: string | null;
}

/** A vacation (or the edited entry) on the chart: the columns it covers (indices into the days, both inclusive). */
export interface ChartBar {
  key: string;
  first: number;
  last: number;
  /** Half a day at a visible end: the bar begins or ends in the middle of that day's column. */
  halfFirst: boolean;
  halfLast: boolean;
  /** A vacation's status (approved solid, in progress pale); none for the edited availability entry. */
  status: string | null;
  /** A piece of the entry being edited on days without any substitute on duty. */
  conflict?: boolean;
  /** The whole period, also where the bar is cut off at the edge of the chart. */
  startDate: string;
  endDate: string;
}

/** An availability entry on the chart: the columns it covers and what its tooltip says. */
export interface AvailabilityBar {
  key: string;
  first: number;
  last: number;
  /** Type and status (e.g. "Workation (Reachable if needed)") for the person and HR staff, the status for others. */
  label: string;
  comment: string | null;
  /** Not available: away as on vacation, a bar of its own; the other entries are thin lines. */
  absent: boolean;
  startDate: string;
  endDate: string;
}

export interface ChartRow {
  key: string;
  name: string;
  /** The entry being edited, the first row. */
  own: boolean;
  bars: ChartBar[];
  /** The substitutes' on a form, the employees' on the team calendar if asked for. */
  availabilities?: AvailabilityBar[];
}

/** A period on the axis, cut off at its edges; null if it lies beside it. ISO dates compare as strings. */
export function barOf(
  key: string,
  days: ChartDay[],
  vacation: ChartPeriod
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
 * The entry being edited in pieces, one per run of days that are or aren't covered by a substitute, so the
 * uncovered days stand out. A half day stays with the piece at that end.
 */
export function splitByConflict(bar: ChartBar, days: ChartDay[]): ChartBar[] {
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

/** An availability entry as a bar of the chart; null if it lies beside the axis. */
export function availabilityBarOf(
  key: string,
  days: ChartDay[],
  entry: {
    startDate?: string | null;
    endDate?: string | null;
    label?: string | null;
    comment?: string | null;
    absent?: boolean;
  }
): AvailabilityBar | null {
  const bar = barOf(key, days, {
    startDate: entry.startDate ?? null,
    endDate: entry.endDate ?? null,
    halfDayBegin: false,
    halfDayEnd: false,
    status: null,
  });
  return bar
    ? {
        key: bar.key,
        first: bar.first,
        last: bar.last,
        label: entry.label ?? "",
        comment: entry.comment ?? null,
        absent: entry.absent === true,
        startDate: bar.startDate,
        endDate: bar.endDate,
      }
    : null;
}

/** A person of the chart: an employee (vacation) or a user (availability entry), keyed so. */
export interface ChartPerson {
  key: string;
  name: string;
}

/**
 * The rows of a form's chart: the entry being edited first, then every substitute — also those without an absence
 * (they are on duty) — with their vacations and availability entries overlapping it. An absence of somebody not (or
 * no longer) chosen still shows, under its own person.
 */
export function buildAbsenceChart(
  days: ChartDay[],
  own: ChartPeriod & { name: string },
  substitutes: ChartPerson[],
  vacations: (ChartPeriod & {
    id?: number | null;
    person: ChartPerson | null;
  })[],
  availabilities: (Parameters<typeof availabilityBarOf>[2] & {
    person: ChartPerson | null;
  })[] = []
): ChartRow[] {
  const ownBar = barOf("own", days, own);
  const rows: ChartRow[] = [
    {
      key: "own",
      name: own.name,
      own: true,
      bars: ownBar ? splitByConflict(ownBar, days) : [],
    },
  ];
  const people = new Map<string, string>();
  for (const s of substitutes) people.set(s.key, s.name);
  for (const { person } of [...vacations, ...availabilities]) {
    if (person && !people.has(person.key)) people.set(person.key, person.name);
  }
  for (const [key, name] of people) {
    const bars = vacations
      .filter((v) => v.person?.key === key)
      .map((v, i) => barOf(`${key}-${v.id ?? i}`, days, v))
      .filter((b): b is ChartBar => b != null);
    const entries = availabilities
      .filter((a) => a.person?.key === key)
      .map((a, i) => availabilityBarOf(`${key}-a${i}`, days, a))
      .filter((b): b is AvailabilityBar => b != null);
    rows.push({
      key,
      name,
      own: false,
      bars,
      ...(entries.length > 0 && { availabilities: entries }),
    });
  }
  return rows;
}

/** The months on the axis, each with the columns of its days (indices into the days, both inclusive). */
export function monthSpans(
  days: ChartDay[]
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
 * What a bar of the chart says to a screen reader and in its tooltip: whose entry, its period and — besides
 * the bar's colour and dashed border — its status, so the chart reads without seeing it.
 *
 * @param formatDate The date as the user writes it.
 * @param statusLabel The status' name, or null for one without a label (the status is left out then).
 */
export function barLabel(
  name: string,
  bar: Pick<ChartBar, "startDate" | "endDate" | "status">,
  formatDate: (iso: string) => string,
  statusLabel: (status: string) => string | null
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
