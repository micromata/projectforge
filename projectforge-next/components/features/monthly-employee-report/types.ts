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
  /** Formatted duration per week, in `weeks` order ("" for an empty cell). */
  perWeek: string[];
  sum: string;
  aiTimeSavings: string;
}

/** One calendar week bucket of the month (a matrix column). */
export interface MonthlyReportWeek {
  fromDay: string;
  toDay: string;
  totalDuration: string;
  grossDuration: string;
  timeSavedByAI: string;
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
  formattedUnbookedDays?: string | null;
  averageWorkingTimeStats?: string | null;
  timeSavingsByAIEnabled: boolean;
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
