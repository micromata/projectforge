/**
 * The monthly employee report ("Monatsbericht"), mirroring the backend DTO
 * (`org.projectforge.rest.dto.MonthlyEmployeeReport*`). All durations, percentages and dates arrive
 * pre-formatted in the user's locale — they are taken as-is (see projectforge-next CLAUDE.md).
 */

/** One matrix row: a cost unit, a task, or the pseudo task ("******") for foreign, unreadable sheets. */
export interface MonthlyReportRow {
  type: "kost2" | "task" | "pseudoTask";
  kost2Id?: number | null;
  taskId?: number | null;
  /** Cost unit number, or the task path (plain). */
  label: string;
  customer?: string | null;
  project?: string | null;
  /** Shown instead of customer/project when the cost unit has no project. */
  description?: string | null;
  kost2Art?: string | null;
  /** Per-week cell, in `weeks` order (an empty cell for a week without hours). */
  perWeek: MonthlyReportCell[];
  sum: string;
  aiTimeSavings: string;
}

/**
 * One week cell of a data row. Normally only `value` (the counted, net hours) is set. When the counted hours
 * are reduced — a working time fraction below 1 and/or the shared-cost split of overlapping time sheets —
 * `gross` carries the raw booked hours (shown in parentheses), `factor` the effective factor (`value / gross`)
 * and `sharedCosts` whether the split was involved, which together drive the explaining tooltip.
 */
export interface MonthlyReportCell {
  /** Counted (net) hours, formatted; "" for an empty cell or purely non-working (factor 0) time. */
  value: string;
  /** Raw booked (gross) hours, formatted; set only when it differs from `value`. */
  gross?: string | null;
  /** Effective factor `value / gross`, formatted (e.g. "0,5"); set together with `gross`. */
  factor?: string | null;
  /** True when the reduction involves the shared-cost overlap split (not only the working time fraction). */
  sharedCosts: boolean;
}

/** One calendar week bucket of the month (a matrix column). */
export interface MonthlyReportWeek {
  fromDay: string;
  toDay: string;
  totalDuration: string;
  grossDuration: string;
  timeSavedByAI: string;
  /** Drill-down week bounds as `yyyy-MM-dd` (clipped to the month). */
  startDate: string;
  endDate: string;
}

export interface MonthlyReport {
  userId?: number | null;
  userName: string;
  maySelectOtherUsers: boolean;
  year: number;
  /** 1-based: 1 = January, ..., 12 = December. */
  month: number;
  availableYears: number[];
  costConfigured: boolean;
  kost1?: string | null;
  numberOfWorkingDays?: string | null;
  /** Target working hours of the month (weekly hours × working days ÷ 5), formatted, or null. */
  targetWorkingHours?: string | null;
  formattedUnbookedDays?: string | null;
  averageWorkingTimeStats?: string | null;
  timeSavingsByAIEnabled: boolean;
  /** True only when the AI feature is on AND some non-zero saving exists — gates the AI column/row and stat. */
  hasTimeSavingsByAI: boolean;
  hasKost2Rows: boolean;
  weeks: MonthlyReportWeek[];
  rows: MonthlyReportRow[];
  totalNetDuration: string;
  showGrossRow: boolean;
  totalGrossDuration: string;
  totalTimeSavedByAI: string;
  timeSavedByAIPercentage: string;
  vacationAvailable: boolean;
  vacationCount?: string | null;
  vacationPlannedCount?: string | null;
  /** Fakturaquote (invoicing quota), formatted as a percentage, or null if not enabled. */
  invoicingQuota?: string | null;
  invoicingQuotaTooltip?: string | null;
  /** Drill-down month bounds as `yyyy-MM-dd`. */
  startDate: string;
  endDate: string;
}

/** The three parameters that select a report; `undefined` lets the backend default them. */
export interface MonthlyReportQuery {
  userId?: number;
  year?: number;
  month?: number;
}
