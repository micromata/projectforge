import { request } from "./client";

const BASE = "/rs/timesheetTimer";

/** The timer of the user (`TimesheetTimerRest`), shared with the app; `running` false without one. */
export interface TimesheetTimer {
  running: boolean;
  /** UTC, ms since 1970 (the server's clock). */
  startedAt?: number | null;
  taskId?: number | null;
  kost2Id?: number | null;
  description?: string | null;
  location?: string | null;
  reference?: string | null;
  tag?: string | null;
  /** The server's clock when answering, ms. */
  now?: number | null;
  /** Set here, not by the server: the server's clock minus this device's when the answer came, ms. */
  clockOffset?: number;
}

/** [timer] with the offset of the clocks; the time of the answer is taken here, where it arrives. */
function withOffset(timer: TimesheetTimer): TimesheetTimer {
  return { ...timer, clockOffset: (timer.now ?? Date.now()) - Date.now() };
}

export const TIMESHEET_TIMER_QUERY_KEY = ["timesheet", "timer"] as const;

export function fetchTimesheetTimer(
  signal?: AbortSignal
): Promise<TimesheetTimer> {
  return request<TimesheetTimer>(BASE, { method: "GET" }, signal).then(
    withOffset
  );
}

/** Starts the timer now (the server's clock), replacing a running one. */
export function startTimesheetTimer(): Promise<TimesheetTimer> {
  return request<TimesheetTimer>(`${BASE}/start`, {
    method: "POST",
    body: JSON.stringify({}),
  }).then(withOffset);
}

export function clearTimesheetTimer(): Promise<TimesheetTimer> {
  return request<TimesheetTimer>(`${BASE}/clear`, { method: "POST" }).then(
    withOffset
  );
}
