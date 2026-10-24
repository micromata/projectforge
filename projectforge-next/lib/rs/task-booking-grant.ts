import { request } from "./client";
import type { TaskKost2 } from "./task";

/**
 * Ad hoc booking grants of a task and its descendants, `TaskBookingGrantRest`: a user may book own time
 * sheets on some tasks for a period, without being member of a group. Who may grant is the server's
 * decision (who may edit the time sheets of others on all tasks of a grant); every endpoint checks it
 * itself.
 */

/** `TaskBookingGrantRest.Status`. */
export type TaskBookingGrantStatus =
  | "ACTIVE"
  | "UPCOMING"
  | "EXPIRED"
  | "REVOKED";

/** A task of a grant, `TaskBookingGrantRest.Position`. */
export interface TaskBookingGrantPosition {
  taskId: number;
  /** From the task of the form down to this task. */
  path: string;
  /** The cost units it may be booked on, absent for all. */
  kost2Titles?: string[];
}

/** One grant, `TaskBookingGrantRest.Entry`. Dates as ISO `yyyy-MM-dd`. */
export interface TaskBookingGrant {
  id: number;
  userId?: number;
  userName?: string;
  validFrom?: string;
  validUntil?: string;
  positions: TaskBookingGrantPosition[];
  /** Tasks of the grant the logged-in user doesn't see. */
  hiddenPositions: number;
  comment?: string;
  grantedByName?: string;
  status: TaskBookingGrantStatus;
  /** Already translated by the backend. */
  statusAsString: string;
  revocable: boolean;
}

/** A task a grant may be given on, `TaskBookingGrantRest.GrantableTask`. */
export interface TaskBookingGrantableTask {
  taskId: number;
  path: string;
  kost2List?: TaskKost2[];
}

/** `TaskBookingGrantRest.Info`: without `mayGrant` nothing else is sent. */
export interface TaskBookingGrantInfo {
  mayGrant: boolean;
  /** The task itself first (if bookable), then its bookable descendants. */
  tasks?: TaskBookingGrantableTask[];
  tasksTruncated?: boolean;
  grants?: TaskBookingGrant[];
}

export interface TaskBookingGrantPositionRequest {
  taskId: number;
  /** Empty or absent for all cost units of the task. */
  kost2Ids: number[];
}

export interface TaskBookingGrantRequest {
  /** The task of the form: the answer is its info. */
  taskId: number;
  userIds: number[];
  positions: TaskBookingGrantPositionRequest[];
  validFrom: string | null;
  validUntil: string | null;
  comment: string | null;
}

/** `TaskBookingGrantRest.Result`: a refusal comes with its translated message, not as an HTTP error. */
export interface TaskBookingGrantResult {
  success: boolean;
  message?: string;
  info?: TaskBookingGrantInfo;
}

export function fetchTaskBookingGrants(
  taskId: number,
  signal?: AbortSignal
): Promise<TaskBookingGrantInfo> {
  return request<TaskBookingGrantInfo>(
    `/rs/taskBookingGrant/info?taskId=${taskId}`,
    { method: "GET" },
    signal
  );
}

export function grantTaskBooking(
  body: TaskBookingGrantRequest
): Promise<TaskBookingGrantResult> {
  return request<TaskBookingGrantResult>("/rs/taskBookingGrant/grant", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** @param taskId The task of the form: the answer is its info. */
export function revokeTaskBookingGrant(
  id: number,
  taskId: number
): Promise<TaskBookingGrantResult> {
  return request<TaskBookingGrantResult>(
    `/rs/taskBookingGrant/revoke?id=${id}&taskId=${taskId}`,
    { method: "POST" }
  );
}
