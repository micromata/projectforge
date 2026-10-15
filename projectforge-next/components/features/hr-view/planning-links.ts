import { monthlyReportDrillDownHref } from "@/lib/timesheet-links";
import type { HrView, HrViewRow } from "./types";

/**
 * The planned week of an employee: the existing planning, else a new one for the employee and the first
 * week of the period (the form normalizes the day to its Monday). Only for an account that may write
 * plannings, as in the Wicket view.
 */
export function planningHref(
  view: HrView,
  userId: number,
  planningId?: number | null
) {
  if (!view.fullAccess) return undefined;
  if (planningId) return `next/hrPlanning/${planningId}`;
  const params = new URLSearchParams({
    userId: String(userId),
    week: view.filter.startDay,
  });
  return `next/hrPlanning/new?${params.toString()}`;
}

/** The time sheets an employee booked in the period, on the given task (a project's) or on all of them. */
export function bookedHref(
  view: HrView,
  row: HrViewRow,
  taskId?: number | null,
  taskName?: string | null
) {
  return monthlyReportDrillDownHref({
    userId: row.userId,
    userName: row.userName ?? undefined,
    taskId: taskId ?? undefined,
    taskName: taskId ? (taskName ?? undefined) : undefined,
    startDate: view.filter.startDay,
    endDate: view.filter.stopDay,
  });
}
