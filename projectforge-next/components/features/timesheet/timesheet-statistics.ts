/**
 * The aggregates of the whole time sheet list, as `TimesheetPagesRest.TimesheetListStatistics` sends them
 * (`ResultSet.statistics`). The two numbers the legacy list's footer shows: the summed duration and,
 * where the installation tracks it, the share of time saved by AI.
 *
 * Both strings are already formatted by the backend in the user's locale and are taken as-is — the raw
 * millis are here only for a caller that wants to add durations up itself.
 */
export interface TimesheetStatistics {
  totalDurationMillis: number;
  totalDuration: string;
  aiEnabled: boolean;
  aiPercentage?: string | null;
}
