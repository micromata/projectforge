import { rawRequest, request, RsError } from "./client";
import type { ValidationError } from "./types";

/**
 * The Kost2 reallocation (`Kost2ReallocationRest`, `/rs/kost2Reallocation`), finance only: a run reallocates
 * accounting records of a period from collective cost units to a target project; the time sheets behind the salary
 * records are moved to the target project when the run is applied.
 */

export type Kost2ReallocationStatus = "DRAFT" | "APPLIED" | "REVERTED";

export interface Kost2ReallocationRef {
  id: number;
  displayName: string;
}

/** `Kost2ReallocationRest.RunRow`. */
export interface Kost2ReallocationRunRow {
  id: number;
  description?: string | null;
  projekt?: Kost2ReallocationRef | null;
  fromYear?: number | null;
  fromMonth?: number | null;
  toYear?: number | null;
  toMonth?: number | null;
  status: Kost2ReallocationStatus;
  appliedAt?: string | null;
}

/** `Kost2ReallocationRest.RuleData`: the records of the source cost unit go completely to the target. */
export interface Kost2ReallocationRule {
  sourceKost2?: Kost2ReallocationRef | null;
  targetKost2?: Kost2ReallocationRef | null;
  kontoFrom?: number | null;
  kontoTo?: number | null;
}

/** `Kost2ReallocationRest.LogRow`: a time sheet moved by an applied run. */
export interface Kost2ReallocationLogRow {
  timesheetId?: number | null;
  user?: string | null;
  year?: number | null;
  month?: number | null;
  oldTask?: string | null;
  newTask?: string | null;
  oldKost2?: string | null;
  newKost2?: string | null;
  hours: number;
}

/** `Kost2ReallocationRest.RunData`. */
export interface Kost2ReallocationRun {
  id?: number | null;
  description?: string | null;
  projekt?: Kost2ReallocationRef | null;
  fromYear?: number | null;
  fromMonth?: number | null;
  toYear?: number | null;
  toMonth?: number | null;
  comment?: string | null;
  status?: Kost2ReallocationStatus | null;
  appliedAt?: string | null;
  rules: Kost2ReallocationRule[];
  /** Some shares of the applied run were discarded by a DATEV reimport. */
  needsReapply: boolean;
  log: Kost2ReallocationLogRow[];
}

/** `Kost2ReallocationRest.TimesheetRow`. */
export interface Kost2ReallocationTimesheet {
  id: number;
  user?: string | null;
  startTime?: string | null;
  stopTime?: string | null;
  hours: number;
  task?: string | null;
  kost2?: string | null;
  description?: string | null;
}

export interface Kost2ReallocationTimesheets {
  detected: Kost2ReallocationTimesheet[];
}

export interface Kost2ReallocationAmount {
  kost2: string;
  amount: number;
}

/** `Kost2ReallocationRest.RecordRow`: an accounting record with its effective amounts before and after. */
export interface Kost2ReallocationRecord {
  id?: number | null;
  satznr?: string | null;
  konto?: number | null;
  text?: string | null;
  beleg?: string | null;
  betrag?: number | null;
  salary: boolean;
  oldAmounts: Kost2ReallocationAmount[];
  newAmounts: Kost2ReallocationAmount[];
}

/** `Kost2ReallocationRest.MoveRow`. */
export interface Kost2ReallocationMove {
  timesheetId: number;
  user?: string | null;
  year: number;
  month: number;
  oldTask?: string | null;
  newTask?: string | null;
  oldKost2?: string | null;
  newKost2?: string | null;
  hours: number;
}

export interface Kost2ReallocationSum {
  kost2: string;
  before: number;
  after: number;
}

/** `Kost2ReallocationRest.PlanResponse`: what a run changes. */
export interface Kost2ReallocationPlan {
  records: Kost2ReallocationRecord[];
  timesheets: Kost2ReallocationMove[];
  sums: Kost2ReallocationSum[];
  warnings: string[];
  /** Errors refuse apply and re-apply (a salary which can't be split by the moved time sheets). */
  errors: string[];
}

/** A refused action (HTTP 406) carries its reason; nothing was written then. */
export type Kost2ReallocationResult<T> =
  | { kind: "ok"; value: T }
  | { kind: "refused"; validationErrors: ValidationError[] };

const BASE = "/rs/kost2Reallocation";

async function post<T>(
  action: string,
  body: unknown,
  signal?: AbortSignal
): Promise<Kost2ReallocationResult<T>> {
  const path = `${BASE}/${action}`;
  const res = await rawRequest(
    path,
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
  if (res.status === 406) {
    const action = (await res.json()) as {
      validationErrors?: ValidationError[];
    };
    return {
      kind: "refused",
      validationErrors: action.validationErrors ?? [],
    };
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return { kind: "ok", value: (await res.json()) as T };
}

export function fetchKost2ReallocationRuns(
  signal?: AbortSignal
): Promise<Kost2ReallocationRunRow[]> {
  return request<Kost2ReallocationRunRow[]>(
    `${BASE}/list`,
    { method: "GET" },
    signal
  );
}

export function fetchKost2ReallocationRun(
  id: number,
  signal?: AbortSignal
): Promise<Kost2ReallocationRun> {
  return request<Kost2ReallocationRun>(
    `${BASE}/run?id=${id}`,
    { method: "GET" },
    signal
  );
}

/** Saves a new draft or a draft (with its rules). */
export function saveKost2ReallocationRun(
  run: Kost2ReallocationRun
): Promise<Kost2ReallocationResult<Kost2ReallocationRun>> {
  return post<Kost2ReallocationRun>("save", run);
}

export function deleteKost2ReallocationRun(
  runId: number
): Promise<Kost2ReallocationResult<boolean>> {
  return post<boolean>("delete", { runId });
}

/** The default rules of the target project for the given source cost units and projects (same cost 2 type). */
export function fetchKost2ReallocationDefaultRules(
  projektId: number,
  sourceKost2Ids: number[],
  sourceProjektIds: number[],
  signal?: AbortSignal
): Promise<Kost2ReallocationRule[]> {
  return request<Kost2ReallocationRule[]>(
    `${BASE}/defaultRules`,
    {
      method: "POST",
      body: JSON.stringify({ projektId, sourceKost2Ids, sourceProjektIds }),
    },
    signal
  );
}

/** The detected time sheets of the run. */
export function fetchKost2ReallocationTimesheets(
  body: { runId: number },
  signal?: AbortSignal
): Promise<Kost2ReallocationResult<Kost2ReallocationTimesheets>> {
  return post<Kost2ReallocationTimesheets>("timesheets", body, signal);
}

export function previewKost2Reallocation(
  runId: number,
  signal?: AbortSignal
): Promise<Kost2ReallocationResult<Kost2ReallocationPlan>> {
  return post<Kost2ReallocationPlan>("preview", { runId }, signal);
}

export function applyKost2Reallocation(
  runId: number
): Promise<Kost2ReallocationResult<Kost2ReallocationPlan>> {
  return post<Kost2ReallocationPlan>("apply", { runId });
}

/** Restores the shares discarded by a DATEV reimport. */
export function reapplyKost2Reallocation(
  runId: number
): Promise<Kost2ReallocationResult<Kost2ReallocationPlan>> {
  return post<Kost2ReallocationPlan>("reapply", { runId });
}

export function revertKost2Reallocation(
  runId: number
): Promise<Kost2ReallocationResult<Kost2ReallocationRun>> {
  return post<Kost2ReallocationRun>("revert", { runId });
}
