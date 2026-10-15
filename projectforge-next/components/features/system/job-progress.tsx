"use client";

import { Progress } from "@/components/ui/progress";
import type { JobInfo } from "@/lib/rs/jobs";

/** The inline progress of a background job started on the System page (see [useSystemJob]). */
export function JobProgress({ job }: { job: JobInfo }) {
  const percentage = Math.min(100, Math.max(0, job.progressPercentage ?? 0));
  return (
    <div className="flex flex-col gap-2">
      <Progress
        value={percentage}
        aria-label={job.progressTitle ?? job.title}
        className="h-1.5"
      />
      <div className="flex items-baseline justify-between gap-3 text-xs text-muted-foreground">
        <span>{job.progressTitle}</span>
        <span className="shrink-0 tabular-nums">{percentage}%</span>
      </div>
      {job.progressDetails && (
        <span className="text-xs text-muted-foreground">
          {job.progressDetails}
        </span>
      )}
      {job.errorMessage && (
        <span className="text-xs text-destructive">{job.errorMessage}</span>
      )}
    </div>
  );
}
