"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cancelJob, fetchJobs, type JobInfo } from "@/lib/rs/jobs";
import { NotificationCloseButton } from "@/components/shared/notifications/notification-close-button";
import { JobProgress } from "./job-progress";

/**
 * Body of the toast that shows a running backend job (see JobToasts).
 *
 * Reads the job from the query cache instead of taking it as a prop: sonner renders a custom toast
 * once and keeps that element, so a second `toast.custom` under the same id would not bring in newer
 * numbers. Subscribing here makes every poll of JobToasts move the bar.
 *
 * Everything but the cancel button is text the server already translated: `title` names the job, the
 * bar and the counters below it are JobProgress.
 *
 * `onClose` only hides the toast, it doesn't stop the job (that is the cancel button): the result still
 * comes as a toast of its own once the job is over (showJobResult). Without it there is no close button
 * (the import page shows the progress inline, not as a toast).
 */
export function JobProgressToast({
  initialJob,
  onClose,
}: {
  initialJob: JobInfo;
  onClose?: () => void;
}) {
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
        {/* Sonner gives a custom toast no close button of its own, and this one stays until the job is
            over — possibly minutes in front of what it covers. */}
        {onClose && (
          <NotificationCloseButton onClose={onClose} className="-my-1 -mr-2" />
        )}
      </div>
      <JobProgress job={job} />
    </div>
  );
}
