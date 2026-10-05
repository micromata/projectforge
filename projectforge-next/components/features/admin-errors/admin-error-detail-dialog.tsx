"use client";

import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { LOG_LEVEL_KEYS, logLevelTone } from "@/components/shared/log-level";
import { LogViewerLink } from "@/components/shared/log-viewer-link";
import { StatusPill } from "@/components/shared/status-pill";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  fetchAdminErrorDetail,
  updateAdminErrors,
  type LogGroupDetail,
  type LogGroupEntry,
  type LogGroupUpdate,
} from "@/lib/rs/admin-errors";
import { toast } from "@/lib/toast";
import { AdminErrorActions } from "./admin-error-actions";
import { AdminErrorFacts, DetailText } from "./admin-error-facts";
import { AdminErrorStatus } from "./admin-error-status";
import { CATEGORY_KEYS } from "./admin-errors-labels";
import { OccurrenceChart } from "./occurrence-chart";

/**
 * Everything known about a problem of the error dashboard: what it means and what to do (the texts of its
 * event), how often it occurred over the last week and month, a sample occurrence with its stack trace, and the
 * status actions. A change refreshes the dashboard's list as well.
 */
export function AdminErrorDetailDialog({
  id,
  onClose,
}: {
  /** The problem shown, null for a closed dialog. */
  id: number | null;
  onClose: () => void;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const detail = useQuery({
    queryKey: ["adminErrors", "detail", id],
    queryFn: ({ signal }) => fetchAdminErrorDetail(id!, signal),
    enabled: id !== null,
  });
  const update = useMutation({
    mutationFn: (change: Omit<LogGroupUpdate, "ids">) =>
      updateAdminErrors({ ...change, ids: [id!] }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["adminErrors"] }),
    onError: (err) =>
      toast.error(err instanceof Error ? err.message : String(err)),
  });

  return (
    <Dialog open={id !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-5xl">
        {detail.data ? (
          <DetailContent
            detail={detail.data}
            pending={update.isPending}
            onChange={(change) => update.mutate(change)}
          />
        ) : (
          <DialogHeader>
            <DialogTitle>{t("system.admin.adminErrors.title")}</DialogTitle>
            <p className="text-sm text-muted-foreground">
              {detail.isError ? t("errorpage.title") : t("loading")}
            </p>
          </DialogHeader>
        )}
      </DialogContent>
    </Dialog>
  );
}

/**
 * The admin log viewer, searching for the class of the problem's location (`Foo` of `Foo:42`): its search
 * doesn't know the codes, and a normalized message isn't found. Only the last log events are there.
 */
function logViewerUrl(entry: LogGroupEntry): string | null {
  const className = entry.location?.split(":")[0];
  if (!className || className === "?") return null;
  return `next/adminLogViewer?search=${encodeURIComponent(className)}`;
}

function DetailContent({
  detail,
  pending,
  onChange,
}: {
  detail: LogGroupDetail;
  pending: boolean;
  onChange: (change: Omit<LogGroupUpdate, "ids">) => void;
}) {
  const t = useTranslations();
  const entry = detail.entry;
  return (
    <>
      <DialogHeader>
        <DialogTitle className="break-words pr-6">
          {entry.message || entry.exceptionClass || entry.code}
        </DialogTitle>
        <div className="flex flex-wrap items-center gap-2 pt-1">
          <AdminErrorStatus entry={entry} />
          <StatusPill
            tone={logLevelTone(entry.level)}
            label={t(LOG_LEVEL_KEYS[entry.level])}
          />
          <StatusPill tone="neutral" label={t(CATEGORY_KEYS[entry.category])} />
          <span className="ml-auto">
            <LogViewerLink url={logViewerUrl(entry)} />
          </span>
        </div>
      </DialogHeader>
      <AdminErrorActions
        detail={detail}
        pending={pending}
        onChange={onChange}
      />
      {detail.registered ? (
        <div className="space-y-2">
          {detail.explanation && (
            <DetailText label={t("system.admin.adminErrors.explanation")}>
              {detail.explanation}
            </DetailText>
          )}
          {detail.action && (
            <DetailText label={t("system.admin.adminErrors.action")}>
              {detail.action}
            </DetailText>
          )}
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">
          {t("system.admin.adminErrors.notRegistered")}
        </p>
      )}
      <AdminErrorFacts detail={detail} />
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <OccurrenceChart
          title={t("system.admin.adminErrors.hourly")}
          values={detail.hourly}
          start={detail.hourlyStart}
          binHours={1}
        />
        <OccurrenceChart
          title={t("system.admin.adminErrors.daily")}
          values={detail.daily}
          start={detail.dailyStart}
          binHours={24}
        />
      </div>
      {detail.sampleMessage && (
        <DetailText label={t("system.admin.adminErrors.sampleMessage")}>
          <pre className="whitespace-pre-wrap break-words font-mono text-xs">
            {detail.sampleMessage}
          </pre>
        </DetailText>
      )}
      {detail.sampleRequest && (
        <DetailText label={t("system.admin.adminErrors.request")}>
          <span className="break-all font-mono text-xs">
            {detail.sampleRequest}
          </span>
        </DetailText>
      )}
      {detail.sampleStackTrace && (
        <DetailText label={t("system.admin.adminErrors.stackTrace")}>
          <pre className="max-h-96 overflow-auto rounded border bg-muted/40 p-2 font-mono text-xs">
            {detail.sampleStackTrace}
          </pre>
        </DetailText>
      )}
    </>
  );
}
