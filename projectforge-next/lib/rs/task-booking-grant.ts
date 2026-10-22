import { request } from "./client";
import type { TaskKost2 } from "./task";

/**
 * Ad hoc booking grants of a task, `TaskBookingGrantRest`: a user may book own time sheets on exactly this
 * task for a period, without being member of a group. Who may grant is the server's decision (who may edit
 * the time sheets of others on the task); every endpoint checks it itself.
 */

/** `TaskBookingGrantRest.Status`. */
export type TaskBookingGrantStatus =
  | "ACTIVE"
  | "UPCOMING"
  | "EXPIRED"
  | "PENDING"
  | "INVITATION_EXPIRED"
  | "REVOKED";

/** One grant, `TaskBookingGrantRest.Entry`. Dates as ISO `yyyy-MM-dd`. */
export interface TaskBookingGrant {
  id: number;
  userId?: number;
  userName?: string;
  validFrom?: string;
  validUntil?: string;
  kost2Id?: number;
  kost2Title?: string;
  comment?: string;
  grantedByName?: string;
  status: TaskBookingGrantStatus;
  /** Already translated by the backend. */
  statusAsString: string;
}

/** `TaskBookingGrantRest.Info`: without `mayGrant` nothing else is sent. */
export interface TaskBookingGrantInfo {
  mayGrant: boolean;
  /** Whether time sheets may be booked on the task at all. */
  bookable?: boolean;
  kost2List?: TaskKost2[];
  grants?: TaskBookingGrant[];
}

export interface TaskBookingGrantRequest {
  taskId: number;
  userId: number | null;
  validFrom: string | null;
  validUntil: string | null;
  kost2Id: number | null;
  comment: string | null;
  asInvitation: boolean;
}

/** `TaskBookingGrantRest.Result`: a refusal comes with its translated message, not as an HTTP error. */
export interface TaskBookingGrantResult {
  success: boolean;
  message?: string;
  /** The link of an invitation, only known in this answer (the token isn't stored). */
  invitationUrl?: string;
  info?: TaskBookingGrantInfo;
}

/** What the invited user sees of the grant before accepting it, `TaskBookingGrantRest.Invitation`. */
export interface TaskBookingInvitation {
  taskId: number;
  taskPath: string;
  validFrom?: string;
  validUntil?: string;
  grantedByName?: string;
  comment?: string;
}

export interface TaskBookingInvitationAcceptResult {
  success: boolean;
  message?: string;
  taskId?: number;
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

export function revokeTaskBookingGrant(
  id: number
): Promise<TaskBookingGrantResult> {
  return request<TaskBookingGrantResult>(
    `/rs/taskBookingGrant/revoke?id=${id}`,
    { method: "POST" }
  );
}

/** A 404 for an invalid, expired or somebody else's token: the caller shows that one message. */
export function fetchTaskBookingInvitation(
  token: string,
  signal?: AbortSignal
): Promise<TaskBookingInvitation> {
  return request<TaskBookingInvitation>(
    `/rs/taskBookingGrant/invitation?token=${encodeURIComponent(token)}`,
    { method: "GET" },
    signal
  );
}

export function acceptTaskBookingInvitation(
  token: string
): Promise<TaskBookingInvitationAcceptResult> {
  return request<TaskBookingInvitationAcceptResult>(
    "/rs/taskBookingGrant/accept",
    { method: "POST", body: JSON.stringify({ token }) }
  );
}
