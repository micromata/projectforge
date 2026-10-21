"use client";

import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";
import type { JobInfo } from "@/lib/rs/jobs";
import type { UIColorName } from "@/lib/rs/types";

/**
 * Fill of the bar by `progressBarColor` (JobInfo.create): green while running, pale while waiting, the
 * primary colour once finished, red if failed, cancelled or refused — the colours the legacy monitor had.
 */
const BAR_COLOR: Partial<Record<UIColorName, string>> = {
  success: "[&_[data-slot=progress-indicator]]:bg-brand-green",
  light: "[&_[data-slot=progress-indicator]]:bg-muted-foreground/40",
  warning: "[&_[data-slot=progress-indicator]]:bg-warning",
  danger: "[&_[data-slot=progress-indicator]]:bg-destructive",
};

/**
 * The bar and the counters of a backend job, shared by its toast (JobProgressToast) and the job monitor
 * page. Everything shown is text the server already translated: `progressTitle` carries status and
 * counters ("#3, Running: 1.200/5.000").
 */
export function JobProgress({ job }: { job: JobInfo }) {
  const percentage = Math.min(100, Math.max(0, job.progressPercentage ?? 0));
  return (
    <>
      <Progress
        value={percentage}
        aria-label={job.progressTitle ?? job.title}
        className={cn(
          "h-1.5",
          job.progressBarColor && BAR_COLOR[job.progressBarColor],
          // Moving stripes while the job runs (globals.css), so a bar that sits at one percentage still
          // shows the job is alive.
          job.animated && "upload-progress upload-progress-active"
        )}
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
    </>
  );
}
