"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { cancelJob, fetchJobs, type JobInfo } from "@/lib/rs/jobs";

/**
 * Body of the toast that shows a running backend job (see JobToasts).
 *
 * Reads the job from the query cache instead of taking it as a prop: sonner renders a custom toast
 * once and keeps that element, so a second `toast.custom` under the same id would not bring in newer
 * numbers. Subscribing here makes every poll of JobToasts move the bar.
 *
 * Everything but the cancel button is text the server already translated: `title` names the job,
 * `progressTitle` carries status and counters ("#3, Running: 1.200/5.000").
 */
export function JobProgressToast({ initialJob }: { initialJob: JobInfo }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [cancelling, setCancelling] = useState(false);

  // Disabled: JobToasts does the polling, this only follows its cache entry. The queryFn is passed
  // all the same, because TanStack complains about a query without one even if it never runs.
  const { data } = useQuery({
    queryKey: ["jobs"],
    queryFn: ({ signal }) => fetchJobs(signal),
    enabled: false,
  });
  const job =
    data?.find((candidate) => candidate.id === initialJob.id) ?? initialJob;
  const percentage = Math.min(100, Math.max(0, job.progressPercentage ?? 0));

  async function cancel() {
    setCancelling(true);
    try {
      // The answer already contains the cancelled job, so the toast doesn't wait for the next poll.
      queryClient.setQueryData(["jobs"], await cancelJob(job.id));
    } finally {
      setCancelling(false);
    }
  }

  return (
    <div className="flex w-full flex-col gap-2 rounded-md border bg-background p-4 shadow-lg">
      <div className="flex items-start gap-3">
        <p className="flex-1 text-sm font-medium">{job.title}</p>
        {/* Only while the backend still accepts a cancellation (JobInfo.cancelId). */}
        {job.cancelId != null && (
          <Button
            variant="ghost"
            size="sm"
            disabled={cancelling}
            onClick={() => void cancel()}
            className="-my-1 h-7 px-2 text-xs"
          >
            {t("cancel")}
          </Button>
        )}
      </div>
      <Progress
        value={percentage}
        aria-label={job.progressTitle ?? job.title}
        className="h-1.5"
      />
      <div className="flex items-baseline justify-between gap-3 text-xs text-muted-foreground">
        {/* Status and counters, already translated by the server. */}
        <span>{job.progressTitle}</span>
        {/* The bar alone doesn't say how far along it is — a long run needs a readable number. */}
        <span className="shrink-0 tabular-nums">{percentage}%</span>
      </div>
      {/* Which entity the counters above belong to — a re-index covers the entity and its history. */}
      {job.progressDetails && (
        <span className="text-xs text-muted-foreground">
          {job.progressDetails}
        </span>
      )}
    </div>
  );
}
