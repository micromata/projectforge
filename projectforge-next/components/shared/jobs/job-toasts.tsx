"use client";

import { useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import {
  fetchJobs,
  isJobFailed,
  isJobTerminated,
  type JobInfo,
} from "@/lib/rs/jobs";
import { useJobStore } from "@/store/job-store";
import { JobProgressToast } from "./job-progress-toast";

/** Same interval the legacy job monitor polls with (CustomizedJobsMonitor). */
const POLL_INTERVAL_MS = 2000;

/** Toasts are addressed by the job they show, so a poll updates one instead of stacking a new one. */
function toastId(jobId: number): string {
  return `job-${jobId}`;
}

/**
 * Shows a progress toast for every backend job the app is watching, and replaces it with the result
 * once the job is over. Renders nothing itself — the toasts live in the app's `Toaster` (top right).
 *
 * Mounted once for the whole authenticated area, because a job outlives the page that started it:
 * a re-index keeps running while the user navigates on.
 */
export function JobToasts() {
  const watchedJobIds = useJobStore((s) => s.watchedJobIds);
  const unwatchJob = useJobStore((s) => s.unwatchJob);
  /** Jobs whose progress toast already exists — see below why it must not be created twice. */
  const shown = useRef(new Set<number>());

  const { data } = useQuery({
    queryKey: ["jobs"],
    queryFn: ({ signal }) => fetchJobs(signal),
    // Only while something is being watched — no background polling for its own sake.
    refetchInterval: watchedJobIds.length > 0 ? POLL_INTERVAL_MS : false,
    enabled: watchedJobIds.length > 0,
    // The client's global 60s would make the poll return the same cached answer every time.
    staleTime: 0,
  });

  useEffect(() => {
    if (!data) return;
    for (const id of watchedJobIds) {
      const job = data.find((candidate) => candidate.id === id);
      if (!job) {
        if (!shown.current.has(id)) {
          // Not in the list and never seen: this is the cached answer of the previous run, which
          // cannot know a job that just started. Waiting for the next poll — dropping the job here
          // would leave the second re-index of a session without any toast.
          continue;
        }
        // Seen before and gone now: the backend forgets terminated jobs after an hour.
        toast.dismiss(toastId(id));
        shown.current.delete(id);
        unwatchJob(id);
        continue;
      }
      if (isJobTerminated(job)) {
        showResult(job);
        shown.current.delete(id);
        unwatchJob(id);
      } else if (!shown.current.has(id)) {
        // Once per job: the toast keeps itself up to date from the query cache, and a second call
        // would not replace sonner's already rendered element anyway.
        shown.current.add(id);
        toast.custom(() => <JobProgressToast initialJob={job} />, {
          id: toastId(id),
          duration: Infinity,
        });
      }
    }
  }, [data, watchedJobIds, unwatchJob]);

  return null;
}

/**
 * Replaces the progress toast with the outcome.
 *
 * The progress toast is dismissed first instead of being overwritten under its id: sonner keeps the
 * custom content of an existing toast and would only wrap it in the success styling, leaving the
 * (now stale) progress bar on screen forever — its duration is Infinity.
 */
function showResult(job: JobInfo): void {
  toast.dismiss(toastId(job.id));
  if (isJobFailed(job)) {
    // The reason, not the counters: a refused job never ran, so its numbers are meaningless.
    toast.error(job.errorMessage ?? job.progressTitle ?? job.title ?? "", {
      description: job.title,
    });
  } else {
    toast.success(job.progressTitle ?? job.title ?? "", {
      // The per-class counts if the job has them, so the sum above stays explainable after the run.
      description: [job.title, job.progressDetails].filter(Boolean).join(" — "),
    });
  }
}
