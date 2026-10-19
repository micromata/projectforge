import { rawRequest, request, RsError } from "./client";
import type { TaskWizardGroupType, TaskWizardResult } from "./task";
import type { ValidationError } from "./types";

/** `ProjectWizardRest.InitResponse`: whether the logged-in user may run the wizard at all. */
export interface ProjectWizardInit {
  allowed: boolean;
  rootTaskId?: number | null;
}

/** `ProjectWizardService.CustomerData`: an existing customer by `id`, or a new one. */
export interface ProjectWizardCustomer {
  /** An existing customer is meant, so a missing `id` is reported rather than taken for a new one. */
  existing?: boolean;
  id?: number | null;
  nummer?: number | null;
  name?: string | null;
  identifier?: string | null;
  division?: string | null;
  status?: string | null;
  description?: string | null;
}

/** `ProjectWizardService.ProjectData`. */
export interface ProjectWizardProject {
  nummer: number;
  projektnummer?: number | null;
  name?: string | null;
  identifier?: string | null;
  status?: string | null;
  description?: string | null;
  contactPersonId?: number | null;
  /** The further contacts next to `contactPersonId`; duplicates and the main contact are dropped server-side. */
  additionalContactIds?: number[];
  kost2ArtIds?: number[];
}

/** `ProjectWizardService.GroupData`: an existing group by `id`, or a new one by `name` with its members. */
export interface ProjectWizardGroup {
  id?: number | null;
  name?: string | null;
  userIds?: number[];
}

/**
 * `ProjectWizardService.Data`. The field ids of the validation errors are the paths into it:
 * `customer.nummer`, `project.name`, `parentTaskId`, `teamGroup.name`, ...
 */
export interface ProjectWizardRequest {
  customer: ProjectWizardCustomer;
  project: ProjectWizardProject;
  parentTaskId?: number | null;
  managerGroup?: ProjectWizardGroup | null;
  teamGroup?: ProjectWizardGroup | null;
  externalGroup?: ProjectWizardGroup | null;
}

/** `ProjectWizardRest.ValidateResponse`. */
export interface ProjectWizardValidation {
  validationErrors: ValidationError[];
  /** The access rights the wizard would write, null as long as no parent element is picked. */
  preview?: TaskWizardResult | null;
}

/** `ProjectWizardRest.ResultGroup`. */
export interface ProjectWizardResultGroup {
  groupType: TaskWizardGroupType;
  id: number;
  name?: string | null;
  created: boolean;
}

/** `ProjectWizardRest.ExecuteResponse`. */
export interface ProjectWizardResult {
  customerId: number;
  customerName?: string | null;
  customerCreated: boolean;
  /** The customer's structure element, the parent of the project's. */
  customerTaskId: number;
  projectId: number;
  projectName?: string | null;
  taskId: number;
  groups: ProjectWizardResultGroup[];
  access: TaskWizardResult;
}

export type ProjectWizardExecuteResult =
  | { kind: "ok"; result: ProjectWizardResult }
  | { kind: "validationErrors"; validationErrors: ValidationError[] };

export function fetchProjectWizardInit(
  signal?: AbortSignal
): Promise<ProjectWizardInit> {
  return request<ProjectWizardInit>(
    "/rs/projectWizard/init",
    { method: "GET" },
    signal
  );
}

/**
 * The structure element of an existing customer, as its projects tell (the parent most of their
 * elements share), for the wizard to preset; null if none is known.
 */
export async function fetchCustomerTask(
  customerId: number,
  signal?: AbortSignal
): Promise<number | null> {
  const answer = await request<{ taskId?: number | null }>(
    `/rs/projectWizard/customerTask?customerId=${customerId}`,
    { method: "GET" },
    signal
  );
  return answer.taskId ?? null;
}

/** The field errors of the request and what it would grant — asked again while the user types. */
export function validateProjectWizard(
  body: ProjectWizardRequest,
  signal?: AbortSignal
): Promise<ProjectWizardValidation> {
  return request<ProjectWizardValidation>(
    "/rs/projectWizard/validate",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}

/** Sets the project up; HTTP 406 carries the `validationErrors`, and then nothing was written. */
export async function executeProjectWizard(
  body: ProjectWizardRequest
): Promise<ProjectWizardExecuteResult> {
  const path = "/rs/projectWizard/execute";
  const res = await rawRequest(path, {
    method: "POST",
    body: JSON.stringify(body),
  });
  if (res.status === 406) {
    const action = (await res.json()) as {
      validationErrors?: ValidationError[];
    };
    return {
      kind: "validationErrors",
      validationErrors: action.validationErrors ?? [],
    };
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return { kind: "ok", result: (await res.json()) as ProjectWizardResult };
}
