/**
 * Background jobs of the backend (`JobsMonitorPageRest`, `org.projectforge.framework.jobs`).
 *
 * Long-running operations answer with a job id instead of blocking (see startReindexOrRunIt in
 * AbstractPagesRest); the client polls this controller for the progress. The endpoints are shared with
 * the legacy frontend and answer with a `ResponseAction` whose variables carry the payload, so the
 * unwrapping happens here and nowhere else.
 */

import { request } from "./client";
import type { ResponseAction, UIColorName } from "./types";

/** One job as `org.projectforge.rest.jobs.JobInfo` serializes it. */
export interface JobInfo {
  id: number;
  title?: string;
  area?: string;
  queueName?: string;
  progressPercentage?: number;
  /** Already translated: "#3, Running: 1.200/5.000". */
  progressTitle?: string;
  /** The counters above broken down, e.g. "BookDO: 3/3, HistoryEntryDO: 166/166". */
  progressDetails?: string;
  /** Status, user and runtime as markdown, pipe separated. */
  info?: string;
  progressBarColor?: UIColorName;
  infoColor?: UIColorName;
  /** True while the job is running — the legacy monitor animates its bar with it. */
  animated?: boolean;
  /** Set only while the job can still be cancelled, and only for a user allowed to (writeAccess). */
  cancelId?: number;
  /** Translated reason of a failed or refused job — the counters of `progressTitle` explain nothing there. */
  errorMessage?: string;
}

/**
 * A job is over once the backend stops offering to cancel it: `cancelId` is set for RUNNING and
 * WAITING only (see JobInfo.create). The status itself isn't part of the DTO.
 */
export function isJobTerminated(job: JobInfo): boolean {
  return job.cancelId == null;
}

/** A terminated job that didn't finish successfully is marked DANGER by the backend. */
export function isJobFailed(job: JobInfo): boolean {
  return job.infoColor === "danger";
}

/** The jobs of the logged-in user that the backend still keeps (terminated ones for an hour). */
export async function fetchJobs(signal?: AbortSignal): Promise<JobInfo[]> {
  const action = await request<ResponseAction>(
    "/rs/jobsMonitor/jobs?all=true",
    { method: "GET" },
    signal
  );
  return readJobs(action);
}

/**
 * Asks the backend to cancel a job. GET despite changing state — the endpoint is shared with the
 * legacy frontend. Answers with the updated job list.
 */
export async function cancelJob(
  jobId: number,
  signal?: AbortSignal
): Promise<JobInfo[]> {
  const action = await request<ResponseAction>(
    `/rs/jobsMonitor/cancel?jobId=${jobId}`,
    { method: "GET" },
    signal
  );
  return readJobs(action);
}

/** The list sits two levels deep: `variables.variables.jobs` (see getJobsAsVariable). */
function readJobs(action: ResponseAction): JobInfo[] {
  const inner = action.variables?.variables as { jobs?: JobInfo[] } | undefined;
  return inner?.jobs ?? [];
}
