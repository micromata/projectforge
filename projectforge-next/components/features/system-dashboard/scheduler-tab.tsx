"use client";

import { useTranslations } from "next-intl";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { TabsContent } from "@/components/ui/tabs";
import { useFormatContext } from "@/hooks/use-format";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import { formatTimestampMinutes } from "@/lib/format";
import { fetchSchedulerJobs } from "@/lib/rs/admin-scheduler";
import { updateSearchParams } from "@/lib/search-params";
import {
  SCHEDULER_QUERY_KEY,
  SchedulerJobDetailDialog,
} from "./scheduler-job-detail-dialog";
import { SchedulerJobsTable } from "./scheduler-jobs-table";

/** The job whose detail is open, kept in the url (`?tab=scheduler&job=<id>`). */
export const JOB_PARAM = "job";

/**
 * The scheduler tab of the system dashboard (`?tab=scheduler`, admin group only): every scheduled job with its
 * last and next run, duration and status, refreshed every 30 seconds. A job's detail shows its history and starts
 * it at once.
 */
export function SchedulerTab() {
  return (
    <TabsContent value="scheduler" className="flex min-h-0 flex-col text-sm">
      <SchedulerJobs />
    </TabsContent>
  );
}

/** Mounted with the open tab only, so the jobs are fetched once it is shown. */
function SchedulerJobs() {
  const t = useTranslations();
  const ctx = useFormatContext();
  const params = useSearchParams();
  const jobId = params.get(JOB_PARAM) || null;
  const list = useQuery({
    queryKey: [SCHEDULER_QUERY_KEY, "list"],
    queryFn: ({ signal }) => fetchSchedulerJobs(signal),
    refetchInterval: 30_000,
  });
  const data = list.data;
  return (
    <>
      {!data ? (
        <div className="pb-8 pt-2">
          {isAccessDenied(list.error) ? (
            <p className="text-destructive">{t("access.exception.noAccess")}</p>
          ) : list.isError ? (
            <p className="text-destructive">{t("errorpage.title")}</p>
          ) : (
            <p className="text-muted-foreground">{t("loading")}</p>
          )}
        </div>
      ) : (
        <>
          <p className="pb-2 text-xs text-muted-foreground">
            {t("system.scheduler.hint.notListed")}
            {data.readySince != null &&
              ` ${t("system.scheduler.hint.readySince", {
                arg0: formatTimestampMinutes(data.readySince, ctx),
              })}`}
          </p>
          <SchedulerJobsTable
            jobs={data.jobs}
            isFetching={list.isFetching}
            onOpen={(job) =>
              updateSearchParams({ [JOB_PARAM]: job.id }, "push")
            }
          />
        </>
      )}
      <SchedulerJobDetailDialog
        id={jobId}
        onClose={() => updateSearchParams({ [JOB_PARAM]: null }, "push")}
      />
    </>
  );
}
