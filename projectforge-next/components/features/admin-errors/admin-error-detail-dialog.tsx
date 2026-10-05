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
      {/* A column rather than the dialog's grid: a grid track grows with the stack trace's longest line, while a
          column holds every part to the dialog's width. Only the stack trace gives way to the height (see
          below); the dialog scrolls as a whole only if even the rest doesn't fit. */}
      <DialogContent className="flex max-h-[90vh] flex-col overflow-y-auto sm:max-w-5xl">
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
 * The admin log viewer at the problem's level, searching for the class of the problem's location (`Foo` of
 * `Foo:42`) or, for an unexpected request error, its uri: its search doesn't know the codes, and a normalized
 * message isn't found. Only the last log events since the server's start are there.
 */
function logViewerUrl(detail: LogGroupDetail): string | null {
  const search =
    (detail.entry.code === REQUEST_ERROR
      ? requestUri(detail.sampleRequest)
      : null) ?? outerClass(detail.entry);
  if (!search) return null;
  // FATAL is never a threshold (see LOG_THRESHOLDS).
  const threshold =
    detail.entry.level === "FATAL" ? "ERROR" : detail.entry.level;
  return `next/adminLogViewer?search=${encodeURIComponent(search)}&threshold=${threshold}`;
}

/** `SupportLogEvents.REQUEST_ERROR`: logged with its uri by `GlobalDefaultExceptionHandler`. */
const REQUEST_ERROR = "support.requestError";

/**
 * The uri of a request's problem (`GET /rs/foo?x=1` → `/rs/foo`): its location is the root cause's frame, which
 * the logged line of the request needn't contain, while its uri is part of it.
 */
function requestUri(request: string | null | undefined): string | null {
  const uri = request?.split(" ")[1]?.split("?")[0];
  return uri || null;
}

/**
 * The top-level class of the location: a nested class or companion is located as `Foo.Bar`, but searched in the
 * log viewer by its binary name `org.projectforge.Foo$Bar`, which contains `Foo` only.
 */
function outerClass(entry: LogGroupEntry): string | null {
  const className = entry.location?.split(":")[0]?.split(".")[0];
  return className && className !== "?" ? className : null;
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
            <LogViewerLink url={logViewerUrl(detail)} />
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
            <DetailText label={t("system.admin.adminErrors.action._")}>
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
        // The one part that shrinks (to min-h-40) and scrolls in itself, so the facts above stay in view.
        <div className="flex min-h-40 flex-col gap-0.5">
          <div className="text-xs font-medium text-muted-foreground">
            {t("system.admin.adminErrors.stackTrace")}
          </div>
          <pre className="min-h-0 flex-1 overflow-auto rounded border bg-muted/40 p-2 font-mono text-xs">
            {detail.sampleStackTrace}
          </pre>
        </div>
      )}
    </>
  );
}
