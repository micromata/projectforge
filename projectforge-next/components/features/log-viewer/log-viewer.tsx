"use client";

import { useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { DataTable } from "@/components/data-table";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SearchInput } from "@/components/shared/list/search-input";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { useAuth } from "@/hooks/use-auth";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import { toast } from "@/lib/toast";
import {
  fetchLogViewer,
  queryLogViewer,
  resetLogViewer,
  type LogLevel,
  type LogViewerData,
  type LogViewerEvent,
  type LogViewFilter,
} from "@/lib/rs/log-viewer";
import {
  LOG_LEVEL_KEYS,
  LOG_THRESHOLDS,
  logLevelRowClass,
} from "@/components/shared/log-level";
import { logViewerColumns } from "./log-viewer-columns";

/** Often enough to follow a running import, rare enough not to flood the server. */
const AUTO_REFRESH_MS = 5000;

export interface LogViewerProps {
  /** The admin log viewer: the last log events of the whole system, admin group only. */
  admin: boolean;
  /** The log subscription of the user view; null for the admin log viewer. */
  id: number | null;
}

/**
 * The log viewer (`/next/logViewer/<id>` and `/next/adminLogViewer`), successor of the React app's server laid
 * out log viewer. The user view shows one of the user's own log subscriptions - the log of a DATEV import, a
 * Merlin run, a script or a mass update, linked from those pages -, the admin view the last log events of the
 * whole system. Every endpoint checks on its own (subscription owner resp. admin group, see LogViewerRest).
 */
export function LogViewer({ admin, id }: LogViewerProps) {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();
  const allowed = !admin || isAdmin;

  const initial = useQuery({
    queryKey: ["logViewer", admin, id, "initial"],
    queryFn: ({ signal }) => fetchLogViewer(admin, id, signal),
    enabled: allowed,
    // The filter starts from this answer once; the entries are queried on their own from then on.
    staleTime: Infinity,
  });

  const title = admin
    ? t("system.admin.adminLogViewer.title")
    : (initial.data?.title ?? t("system.admin.logViewer.title"));
  const denied =
    (admin && !isLoading && !isAdmin) || isAccessDenied(initial.error);

  return (
    <PageShell>
      {initial.data && !initial.data.subscriptionMissing ? (
        <LogViewerContent
          admin={admin}
          id={id}
          title={title}
          initial={initial.data}
        />
      ) : (
        <>
          <PageTitleRow
            category={admin ? undefined : t("system.admin.logViewer.title")}
            title={title}
          />
          <div className="px-4 pb-8 pt-2 text-sm">
            {denied ? (
              <p className="text-destructive">
                {t("access.exception.noAccess")}
              </p>
            ) : initial.isError ? (
              <p className="text-destructive">{t("errorpage.title")}</p>
            ) : initial.data?.subscriptionMissing ? (
              <p className="text-muted-foreground">
                {t("system.admin.logViewer.subscriptionMissing")}
              </p>
            ) : (
              <p className="text-muted-foreground">{t("loading")}</p>
            )}
          </div>
        </>
      )}
    </PageShell>
  );
}

/** What the user narrows the entries down to; part of the query key, so a change queries anew. */
interface Criteria {
  threshold: LogLevel;
  search: string;
}

function LogViewerContent({
  admin,
  id,
  title,
  initial,
}: LogViewerProps & { title: string; initial: LogViewerData }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  // A link may start with a search of its own, e.g. the error dashboard's one for a problem (`?search=`).
  // With its level as `?threshold=`, so that a stored higher threshold doesn't hide the problem's entries.
  const params = useSearchParams();
  const linkedSearch = params.get("search");
  const linkedThreshold = LOG_THRESHOLDS.find(
    (level) => level === params.get("threshold")
  );
  const [criteria, setCriteria] = useState<Criteria>({
    threshold: linkedThreshold ?? initial.filter.threshold,
    search: linkedSearch ?? initial.filter.search ?? "",
  });
  const [autoRefresh, setAutoRefresh] = useState(
    initial.filter.autoRefresh === true
  );
  const filter: LogViewFilter = {
    ...criteria,
    autoRefresh,
    logSubscriptionId: id,
  };

  const entriesKey = ["logViewer", admin, id, "entries", criteria];
  // The first entries come with the initial answer, so the start filter doesn't query twice (not for a linked
  // search or threshold: the answer was filtered by the stored one).
  const startCriteria = useState(criteria)[0];
  const entries = useQuery({
    queryKey: entriesKey,
    queryFn: ({ signal }) => queryLogViewer(admin, filter, signal),
    initialData:
      criteria === startCriteria &&
      linkedSearch === null &&
      linkedThreshold === undefined
        ? initial.entries
        : undefined,
    placeholderData: keepPreviousData,
    // Refreshed on demand (button) or by the auto refresh only. Not in a hidden tab (the react-query default):
    // every request touches the session and would keep an idle tab logged in (see use-auth.ts).
    staleTime: Infinity,
    refetchInterval: autoRefresh ? AUTO_REFRESH_MS : false,
  });

  const reset = useMutation({
    mutationFn: () => resetLogViewer(filter),
    onSuccess: (result) =>
      queryClient.setQueryData<LogViewerEvent[]>(entriesKey, result),
    onError: (err) =>
      toast.error(err instanceof Error ? err.message : String(err)),
  });

  const columns = useMemo(() => logViewerColumns(t, admin), [t, admin]);

  return (
    <>
      <PageTitleRow
        category={admin ? undefined : t("system.admin.logViewer.title")}
        title={title}
      >
        <Button
          size="sm"
          variant="outline"
          disabled={entries.isFetching}
          onClick={() => entries.refetch()}
        >
          {t("refresh")}
        </Button>
        {/* The admin log viewer reads the memory appender of the whole system, which has nothing to reset. */}
        {!admin && (
          <Button
            size="sm"
            variant="outline"
            disabled={reset.isPending}
            onClick={() => reset.mutate()}
          >
            {t("reset")}
          </Button>
        )}
      </PageTitleRow>
      <div className="flex flex-wrap items-center gap-4 px-4 pb-2 pt-3">
        <div className="relative w-full max-w-md">
          <SearchInput
            value={criteria.search}
            onChange={(search) => setCriteria({ ...criteria, search })}
          />
        </div>
        <div className="flex items-center gap-2">
          <Label htmlFor="log-viewer-threshold">
            {t("system.admin.logViewer.level")}
          </Label>
          <Select
            value={criteria.threshold}
            onValueChange={(threshold) =>
              setCriteria({ ...criteria, threshold: threshold as LogLevel })
            }
          >
            <SelectTrigger id="log-viewer-threshold" className="h-9 w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {LOG_THRESHOLDS.map((level) => (
                <SelectItem key={level} value={level}>
                  {t(LOG_LEVEL_KEYS[level])}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex items-center gap-2">
          <Switch
            id="log-viewer-auto-refresh"
            checked={autoRefresh}
            onCheckedChange={setAutoRefresh}
          />
          <Label htmlFor="log-viewer-auto-refresh">
            {t("system.admin.logViewer.autoRefresh")}
          </Label>
        </div>
      </div>
      <div className="flex min-h-0 flex-1 flex-col px-4 pb-4">
        <DataTable<LogViewerEvent>
          columns={columns}
          data={entries.data ?? []}
          isFetching={entries.isFetching}
          enableColumnFilters={false}
          manualSorting={false}
          showPagination={false}
          dense
          getRowId={(row) => String(row.id)}
          rowClassName={(row) => logLevelRowClass(row.level)}
        />
      </div>
    </>
  );
}
