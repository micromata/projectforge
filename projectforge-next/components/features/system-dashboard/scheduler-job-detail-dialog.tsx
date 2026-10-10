"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { StatusPill } from "@/components/shared/status-pill";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber, formatTimestampMinutes } from "@/lib/format";
import {
  fetchSchedulerJobDetail,
  runSchedulerJobNow,
  type SchedulerJobDetail,
  type SchedulerRunEntry,
} from "@/lib/rs/admin-scheduler";
import { toast } from "@/lib/toast";
import { DetailText, Fact } from "./admin-error-facts";
import { SchedulerJobStatus } from "./scheduler-job-status";
import {
  formatDurationMillis,
  RUN_STATUS_KEYS,
  RUN_STATUS_TONES,
  scheduleText,
} from "./scheduler-labels";
import { SchedulerRunsChart } from "./scheduler-runs-chart";

export const SCHEDULER_QUERY_KEY = "adminScheduler";

/**
 * Everything known about a scheduled job: what it does, when it runs, its runs per day and month, its last runs
 * since the start and the failed and slow runs stored before, and "run now".
 */
export function SchedulerJobDetailDialog({
  id,
  onClose,
}: {
  /** The job shown, null for a closed dialog. */
  id: string | null;
  onClose: () => void;
}) {
  const t = useTranslations();
  const detail = useQuery({
    queryKey: [SCHEDULER_QUERY_KEY, "detail", id],
    queryFn: ({ signal }) => fetchSchedulerJobDetail(id!, signal),
    enabled: id !== null,
    // A started run shows up without a click on refresh.
    refetchInterval: (query) =>
      query.state.data?.job.status === "RUNNING" ? 5_000 : 30_000,
  });
  return (
    <Dialog open={id !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="flex max-h-[90vh] flex-col overflow-y-auto sm:max-w-5xl">
        {detail.data ? (
          <DetailContent detail={detail.data} />
        ) : (
          <DialogHeader>
            <DialogTitle>{t("system.scheduler.tab")}</DialogTitle>
            <p className="text-sm text-muted-foreground">
              {detail.isError ? t("errorpage.title") : t("loading")}
            </p>
          </DialogHeader>
        )}
      </DialogContent>
    </Dialog>
  );
}

function DetailContent({ detail }: { detail: SchedulerJobDetail }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const job = detail.job;
  const timestamp = (value?: number | null) =>
    value != null ? formatTimestampMinutes(value, ctx) : null;
  const count = (value: number) => formatNumber(value, ctx, 0);
  return (
    <>
      <DialogHeader>
        <DialogTitle className="break-words pr-6">{job.title}</DialogTitle>
        <div className="flex flex-wrap items-center gap-2 pt-1">
          <SchedulerJobStatus job={job} />
          <StatusPill tone="neutral" label={job.areaTitle} />
          <span className="font-mono text-xs text-muted-foreground">
            {job.id}
          </span>
          <span className="ml-auto">
            <RunNowButton detail={detail} />
          </span>
        </div>
      </DialogHeader>
      {job.description && (
        <p className="text-sm text-muted-foreground">{job.description}</p>
      )}
      {job.inactiveReason && (
        <DetailText label={t("system.scheduler.detail.inactiveReason")}>
          {job.inactiveReason}
        </DetailText>
      )}
      <dl className="grid grid-cols-1 gap-x-6 gap-y-1 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <Fact label={t("system.scheduler.column.schedule")} mono>
          {scheduleText(job, t, ctx)}
        </Fact>
        <Fact label={t("system.scheduler.column.lastRun")}>
          {timestamp(job.lastRun)}
        </Fact>
        <Fact label={t("system.scheduler.column.nextRun")}>
          {timestamp(job.nextRun)}
        </Fact>
        <Fact label={t("system.scheduler.column.duration")}>
          {formatDurationMillis(job.lastDurationMs, ctx)}
        </Fact>
        <Fact label={t("system.scheduler.column.avgDuration7d")}>
          {formatDurationMillis(job.avgDurationMs7d, ctx)}
        </Fact>
        <Fact label={t("system.scheduler.column.maxDuration7d")}>
          {formatDurationMillis(job.maxDurationMs7d, ctx)}
        </Fact>
        <Fact label={t("system.scheduler.column.runs7d")}>
          {count(job.runs7d)}
        </Fact>
        <Fact label={t("system.scheduler.column.errors7d")}>
          {count(job.errors7d)}
        </Fact>
        <Fact label={t("system.scheduler.column.skipped7d")}>
          {count(job.skipped7d)}
        </Fact>
        <Fact label={t("system.scheduler.column.slow7d")}>
          {count(job.slow7d)}
        </Fact>
      </dl>
      {job.lastError && (
        <DetailText
          label={`${t("system.scheduler.detail.lastError")}${
            job.lastErrorTime ? ` (${timestamp(job.lastErrorTime)})` : ""
          }`}
        >
          <span className="break-words">{job.lastError}</span>
        </DetailText>
      )}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <SchedulerRunsChart
          title={t("system.scheduler.detail.daily", {
            arg0: detail.dailyRetentionDays,
          })}
          periods={detail.days}
        />
        <SchedulerRunsChart
          title={t("system.scheduler.detail.monthly")}
          periods={detail.months}
          months
        />
      </div>
      <RunList
        title={t("system.scheduler.detail.recentRuns")}
        runs={detail.recentRuns}
      />
      <RunList
        title={t("system.scheduler.detail.storedRuns")}
        runs={detail.storedRuns}
      />
    </>
  );
}

/** "Run now" behind a question: a job may take long or change data (e.g. send mails). */
function RunNowButton({ detail }: { detail: SchedulerJobDetail }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [confirm, setConfirm] = useState(false);
  const job = detail.job;
  const runNow = useMutation({
    mutationFn: () => runSchedulerJobNow(job.id),
    onSuccess: (conflict) => {
      if (conflict === "RUNNING") {
        toast.error(t("system.scheduler.runNow.running"));
      } else if (conflict === "INACTIVE") {
        toast.error(t("system.scheduler.runNow.inactive"));
      } else {
        toast.success(t("system.scheduler.runNow.started"));
      }
      // The run starts in a thread of its own: shown as running a moment later.
      setTimeout(
        () =>
          queryClient.invalidateQueries({ queryKey: [SCHEDULER_QUERY_KEY] }),
        500
      );
    },
    onError: (err) =>
      toast.error(err instanceof Error ? err.message : String(err)),
  });
  return (
    <>
      <Button
        size="sm"
        variant="outline"
        disabled={!job.runNowAllowed || runNow.isPending}
        title={
          job.runNowAllowed
            ? undefined
            : t("system.scheduler.runNow.notAllowed")
        }
        onClick={() => setConfirm(true)}
      >
        {t("system.scheduler.runNow._")}
      </Button>
      <ConfirmDialog
        open={confirm}
        onOpenChange={setConfirm}
        title={t("system.scheduler.runNow._")}
        description={t("system.scheduler.runNow.question", {
          arg0: job.title,
        })}
        confirmLabel={t("system.scheduler.runNow._")}
        onConfirm={() => {
          setConfirm(false);
          runNow.mutate();
        }}
      />
    </>
  );
}

/** Runs as a compact table, newest first; a failed run's stack trace is folded in. */
function RunList({
  title,
  runs,
}: {
  title: string;
  runs: SchedulerRunEntry[];
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return (
    <div className="space-y-1">
      <div className="text-xs font-medium text-muted-foreground">{title}</div>
      {runs.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("system.scheduler.detail.noRuns")}
        </p>
      ) : (
        <div className="max-h-72 overflow-auto rounded border">
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-muted text-xs text-muted-foreground">
              <tr>
                <th className="px-2 py-1 text-left font-medium">
                  {t("system.scheduler.detail.start")}
                </th>
                <th className="px-2 py-1 text-right font-medium">
                  {t("system.scheduler.column.duration")}
                </th>
                <th className="px-2 py-1 text-left font-medium">
                  {t("status")}
                </th>
                <th className="px-2 py-1 text-left font-medium">
                  {t("system.scheduler.detail.info")}
                </th>
              </tr>
            </thead>
            <tbody>
              {runs.map((run, index) => (
                <tr
                  key={`${run.start}-${index}`}
                  className="border-t align-top"
                >
                  <td className="whitespace-nowrap px-2 py-1 tabular-nums">
                    {formatTimestampMinutes(run.start, ctx)}
                  </td>
                  <td className="whitespace-nowrap px-2 py-1 text-right tabular-nums">
                    {formatDurationMillis(run.durationMs, ctx)}
                  </td>
                  <td className="px-2 py-1">
                    <div className="flex flex-wrap gap-1">
                      <StatusPill
                        tone={RUN_STATUS_TONES[run.status]}
                        label={t(RUN_STATUS_KEYS[run.status])}
                      />
                      {run.slow && (
                        <StatusPill
                          tone="danger"
                          label={t("system.scheduler.detail.slow")}
                        />
                      )}
                      {run.trigger === "MANUAL" && (
                        <StatusPill
                          tone="neutral"
                          label={t("system.scheduler.detail.manual")}
                        />
                      )}
                    </div>
                  </td>
                  <td className="min-w-0 px-2 py-1">
                    {run.errorMessage && (
                      <div className="break-words text-destructive">
                        {run.errorMessage}
                      </div>
                    )}
                    {run.notes.map((note, i) => (
                      <div key={i} className="break-words">
                        {note}
                      </div>
                    ))}
                    {run.stackExcerpt && (
                      <details>
                        <summary className="cursor-pointer text-xs text-muted-foreground">
                          {t("system.admin.adminErrors.stackTrace")}
                        </summary>
                        <pre className="max-h-60 overflow-auto whitespace-pre font-mono text-xs">
                          {run.stackExcerpt}
                        </pre>
                      </details>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
