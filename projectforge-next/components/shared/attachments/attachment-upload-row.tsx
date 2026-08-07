"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon, AlertCircleIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";
import type { UploadJob } from "@/hooks/use-attachment-uploads";

interface Props {
  job: UploadJob;
  onCancel: (jobId: number) => void;
}

/**
 * One file being uploaded, with its own progress bar — several files mean several of these, as in
 * the legacy `MultipleFileUploadArea`.
 *
 * Successful jobs are removed by the hook rather than shown as "done": the file then appears in the
 * attachment list right below, and two rows for one file would only puzzle.
 */
export function AttachmentUploadRow({ job, onCancel }: Props) {
  const t = useTranslations();
  const failed = job.state === "failed";
  // The bar is indeterminate until the first progress event; showing 0 % would look stuck.
  const percent = job.percent ?? 0;

  return (
    <li className="flex items-center gap-3 border-b border-border/60 py-2 last:border-b-0">
      <div className="min-w-0 flex-1">
        <div className="flex items-baseline gap-1.5">
          {failed && (
            <HugeiconsIcon
              icon={AlertCircleIcon}
              size={11}
              className="shrink-0 text-destructive"
              aria-hidden
            />
          )}
          <span className="truncate text-xs font-medium">{job.name}</span>
        </div>
        {failed ? (
          <p className="text-[11px] text-destructive">{job.error}</p>
        ) : (
          <div className="mt-1.5 flex items-center gap-2">
            <Progress
              value={percent}
              // Announced as a progress bar by name, so it is clear which file it belongs to.
              aria-label={`${t("attachment.upload.title")}: ${job.name}`}
              // upload-progress styles the primitive's fill from the outside — the stripes belong
              // to the indicator, and components/ui/ must not be edited (see globals.css).
              className={cn(
                "h-2 upload-progress",
                job.state === "sending" && "upload-progress-active",
                job.state === "queued" && "opacity-50"
              )}
            />
            <span className="w-9 shrink-0 text-right text-[11px] tabular-nums text-muted-foreground">
              {job.percent == null ? "…" : `${job.percent} %`}
            </span>
          </div>
        )}
      </div>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="size-7 shrink-0 text-muted-foreground hover:text-destructive"
        // Cancels a running upload, drops a queued one, dismisses a failed one — all "remove this
        // row", so one button covers them and "cancel" is the honest word for it.
        aria-label={`${t("cancel")}: ${job.name}`}
        onClick={() => onCancel(job.id)}
      >
        <HugeiconsIcon icon={Cancel01Icon} size={13} />
      </Button>
    </li>
  );
}
