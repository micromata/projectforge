"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchJobs, isJobTerminated, type JobInfo } from "@/lib/rs/jobs";
import { useJobStore } from "@/store/job-store";

/** Same interval the global job monitor polls with (see JobToasts). */
const POLL_INTERVAL_MS = 2000;

/**
 * Starts a background job of the System page (reindex, JCR backup ZIP, JCR migration) and follows its
 * progress on the page.
 *
 * The backend answers with the id of a background job (a full run takes minutes); this polls the job
 * list for that id so the card can render an inline progress bar — the "synchronous feel" the classic
 * page had. It also hands the id to the job store, so the app-wide `JobToasts` shows the same run and
 * it survives navigating away.
 */
export function useSystemJob<T>(
  startJob: (body: T) => Promise<{ jobId: number }>
) {
  const watchJob = useJobStore((s) => s.watchJob);
  const [jobId, setJobId] = useState<number | null>(null);
  const [starting, setStarting] = useState(false);

  const { data } = useQuery({
    queryKey: ["jobs"],
    queryFn: ({ signal }) => fetchJobs(signal),
    // Only while a run of ours is being followed; the shared ["jobs"] cache also feeds JobToasts.
    refetchInterval: jobId != null ? POLL_INTERVAL_MS : false,
    enabled: jobId != null,
    staleTime: 0,
  });

  const job: JobInfo | undefined =
    jobId != null
      ? data?.find((candidate) => candidate.id === jobId)
      : undefined;
  // Running until the job appears in the list and reports itself terminated; also true right after
  // starting, before the first poll returns the new job.
  const running =
    starting || (jobId != null && (!job || !isJobTerminated(job)));

  async function start(body: T): Promise<void> {
    setStarting(true);
    try {
      const { jobId: id } = await startJob(body);
      setJobId(id);
      watchJob(id);
    } finally {
      setStarting(false);
    }
  }

  return { start, job, running };
}
