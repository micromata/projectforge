/**
 * The aggregates of the whole time sheet list, as `TimesheetEntityRest.TimesheetListStatistics` sends them
 * (`ResultSet.statistics`): the summed duration, where the installation tracks it the share of time
 * saved by AI, and the booked time overlapping other sheets (shared costs) with its share of the total (absent if none).
 *
 * All strings are already formatted by the backend in the user's locale and are taken as-is — the raw
 * millis are here only for a caller that wants to add durations up itself.
 */
export interface TimesheetStatistics {
  totalDurationMillis: number;
  totalDuration: string;
  aiEnabled: boolean;
  aiPercentage?: string | null;
  sharedCostDurationMillis?: number;
  sharedCostDuration?: string | null;
  sharedCostPercentage?: string | null;
}
