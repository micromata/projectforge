"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { JobProgress } from "@/components/shared/jobs/job-progress";
import { MarkdownText } from "@/components/shared/markdown-text";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import {
  cancelJob,
  fetchJobs,
  isJobFailed,
  isJobTerminated,
  type JobInfo,
} from "@/lib/rs/jobs";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";

/** Same interval as the job toasts (JobToasts), whose cache entry this page shares. */
const POLL_INTERVAL_MS = 2000;

/**
 * The job monitor (`/next/jobsMonitor`, formerly the dynamic React page `react/jobsMonitor/dynamic`): the
 * backend jobs of the logged-in user, running ones first, terminated ones as long as the backend keeps them
 * (an hour). Polls while it is open and shares the query `["jobs"]` with the toasts, so both show the same.
 *
 * `?jobId=` puts that job first and marks it. `?caller=` is where an import page wants to be taken back to once
 * its job has finished successfully (without `jobId`: once all listed jobs have).
 *
 * Every user may open it: the backend answers only the jobs the user may read and cancels only the ones the
 * user may write. The menu offers it to admins and the finance group.
 */
export function JobsMonitorPage() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const jobId = Number(searchParams.get("jobId")) || undefined;
  const caller = searchParams.get("caller");
  const [cancelCandidate, setCancelCandidate] = useState<JobInfo | null>(null);
  const returned = useRef(false);

  const { data, isPending } = useQuery({
    queryKey: ["jobs"],
    queryFn: ({ signal }) => fetchJobs(signal),
    refetchInterval: POLL_INTERVAL_MS,
    // The client's global 60s would make the poll return the same cached answer every time.
    staleTime: 0,
  });

  const jobs = useMemo(() => pinFirst(data ?? [], jobId), [data, jobId]);

  useEffect(() => {
    if (returned.current || !caller || !isSameOriginUrl(caller)) return;
    const awaited = jobId != null ? jobs.filter((j) => j.id === jobId) : jobs;
    if (
      awaited.length > 0 &&
      awaited.every((job) => isJobTerminated(job) && !isJobFailed(job))
    ) {
      returned.current = true;
      // The caller is a page of the legacy app (an import), so a full navigation, not the router.
      window.location.assign(caller);
    }
  }, [jobs, jobId, caller]);

  async function cancel(job: JobInfo) {
    try {
      // The answer already contains the cancelled job, so page and toast don't wait for the next poll.
      queryClient.setQueryData(["jobs"], await cancelJob(job.id));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
    }
  }

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.administration")}
        title={t("jobs.monitor.title")}
      />
      <div className="flex flex-col gap-3 px-4 pb-6 pt-2">
        {!isPending && jobs.length === 0 && (
          <p className="text-sm text-muted-foreground">
            {t("jobs.monitor.noJobsAvailable")}
          </p>
        )}
        {jobs.map((job) => (
          <JobCard
            key={job.id}
            job={job}
            highlighted={job.id === jobId}
            onCancel={() => setCancelCandidate(job)}
          />
        ))}
      </div>
      <ConfirmDialog
        open={cancelCandidate != null}
        onOpenChange={(open) => {
          if (!open) setCancelCandidate(null);
        }}
        title={cancelCandidate?.title ?? t("jobs.monitor.title")}
        description={t("jobs.job.cancel.confirmationMessage")}
        confirmLabel={t("yes")}
        cancelLabel={t("no")}
        destructive
        onConfirm={() => {
          if (cancelCandidate) void cancel(cancelCandidate);
          setCancelCandidate(null);
        }}
      />
    </PageShell>
  );
}

function JobCard({
  job,
  highlighted,
  onCancel,
}: {
  job: JobInfo;
  highlighted: boolean;
  onCancel: () => void;
}) {
  const t = useTranslations();
  const failed = isJobFailed(job);
  return (
    <div
      data-testid="job-card"
      className={cn(
        "flex flex-col gap-2 rounded-md border p-4",
        highlighted && "border-primary"
      )}
    >
      <div className="flex items-start gap-3">
        <p className="flex-1 text-sm font-medium">{job.title}</p>
        {/* Only while the backend still accepts a cancellation (JobInfo.cancelId). */}
        {job.cancelId != null && (
          <Button
            variant="outline"
            size="sm"
            onClick={onCancel}
            className="-my-1 h-7 px-2 text-xs"
          >
            {t("cancel")}
          </Button>
        )}
      </div>
      <JobProgress job={job} />
      {/* Status, user and run-time, as markdown written by the backend (AbstractJob.info). */}
      {job.info && (
        <MarkdownText
          text={job.info}
          className={cn(
            "text-xs",
            failed ? "text-destructive" : "text-muted-foreground"
          )}
        />
      )}
      {failed && job.errorMessage && (
        <p className="text-sm text-destructive">{job.errorMessage}</p>
      )}
    </div>
  );
}

/** The backend orders by status and time; the job the page was opened for goes first all the same. */
function pinFirst(jobs: JobInfo[], jobId: number | undefined): JobInfo[] {
  const pinned = jobs.find((job) => job.id === jobId);
  return pinned ? [pinned, ...jobs.filter((job) => job !== pinned)] : jobs;
}

/** `?caller=` comes from the url, so only a page of this server is a place to go back to. */
function isSameOriginUrl(url: string): boolean {
  try {
    return (
      new URL(url, window.location.origin).origin === window.location.origin
    );
  } catch {
    return false;
  }
}
